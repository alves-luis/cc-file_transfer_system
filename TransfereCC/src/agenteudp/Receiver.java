package agenteudp;

import agenteudp.control.Ack;
import agenteudp.control.AuthenticationRequest;
import agenteudp.control.ConnectionRequest;
import agenteudp.control.ConnectionTermination;
import agenteudp.data.BlockData;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.CRC32;

public class Receiver implements Runnable {

    public static int DEFAULT_PORT = 5555;

    private DatagramSocket socket;
    private int expectedSize; // this will increment according to flow
    private ArrayList<byte[]> datagrams;
    private ArrayList<PDU> pdus;
    private ReentrantLock lock;
    private Condition pduArrived;
    private Condition datagramArrived;
    private InetAddress expectedIP;
    private boolean running;
    private byte[] buffer;

    /**
     * DEPRECATED
     * @param port
     */
    public Receiver(int port) {
        try {
            this.socket = new DatagramSocket(port);
            this.expectedSize = 2048;
            this.pdus = new ArrayList<>();
            this.datagrams = new ArrayList<>();
            this.lock = new ReentrantLock();
            this.pduArrived = lock.newCondition();
            this.datagramArrived = lock.newCondition();
            this.expectedIP = null;
            this.buffer = new byte[expectedSize];
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public Receiver(int defaultPort, InetAddress receiverIP) {
        try {
            this.socket = new DatagramSocket(defaultPort);
            this.expectedSize = 2048;
            this.pdus = new ArrayList<>();
            this.datagrams = new ArrayList<>();
            this.lock = new ReentrantLock();
            this.pduArrived = lock.newCondition();
            this.datagramArrived = lock.newCondition();
            this.expectedIP = receiverIP;
            this.buffer = new byte[expectedSize];
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that blocks until a datagram is received. Returns the content of that datagram
     * @return
     */
    public byte[] receiveDatagram() {
        byte[] data = null;
        boolean valid = false;
        try {
            DatagramPacket packet = new DatagramPacket(this.buffer, this.expectedSize);

            while(!valid) {
                socket.receive(packet);
                InetAddress receiverIP = packet.getAddress();
                if (this.expectedIP == null || receiverIP.equals(this.expectedIP))
                    valid = true;
            }

            int length = packet.getLength();
            int offset = packet.getOffset();
            data = new byte[length];
            System.arraycopy(packet.getData(),offset,data,0,length);
        }
        catch(UnknownHostException e){
            System.err.println("Opah, não sei que se passou");
        } catch (IOException e) {
            System.err.println("Exceção a enviar o pacote!");
        }
        return data;
    }

    /**
     * Method that blocks until a PDU is received, then returns that PDU
     * @param timeout the timeout of the condition to wait
     * @return null if timed out
     */
    public PDU getFIFO(long timeout) {
        try {
            lock.lock();
            long timeLeft = timeout;
            while(this.pdus.isEmpty() || timeLeft > 0) {
                timeLeft = this.pduArrived.awaitNanos(timeout);
            }

            return this.pdus.remove(0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    private synchronized boolean running() {
        return this.running;
    }

    public synchronized void stopRunning() {
        this.running = false;
    }

    public void setExpectedIP(InetAddress ip) {
        this.expectedIP = ip;
    }

    @Override
    public void run() {
        System.out.println("Socket is connected!");
        Processor p = new Processor(this);
        new Thread(p).start();
        this.running = true;
        while(running) {
            byte[] datagram = this.receiveDatagram();
            try {
                lock.lock();
                this.datagrams.add(datagram);
                this.datagramArrived.signalAll();
            }
            finally {
                lock.unlock();
            }
        }
    }

    /**
     * Processes received datagrams
     */
    private class Processor implements Runnable {

        private Receiver r;

        private Processor(Receiver r) {
            this.r = r;
        }

        @Override
        public void run() {
            while(r.running()) {
                try {
                    lock.lock();
                    int read = r.datagrams.size();
                    while(r.datagrams.isEmpty())
                        r.datagramArrived.await();

                    PDU p = DatagramParser.processDatagram(r.datagrams.get(0));
                    System.out.println("Processed: "  + p.toString());
                    r.datagrams.remove(0);
                    r.pdus.add(p);
                    r.pduArrived.signalAll();

                } catch (InterruptedException | InvalidTypeOfDatagram | InvalidCRCException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
