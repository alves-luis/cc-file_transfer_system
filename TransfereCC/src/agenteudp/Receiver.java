package agenteudp;

import security.Keys;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

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
    private ConcurrentHashMap<Long,Condition> seqToCondition;
    private ConcurrentHashMap<Long,PDU> seqToPdu;
    // is used to map all the received datagrams to given IP
    private ConcurrentHashMap<InetAddress,List<byte[]>> knownIpsToReceivedDatagrams;
    private boolean aesKeyEncryption; // if should use aesKeyDecryption;
    private Keys communicationKeys;


    public Receiver(int defaultPort, InetAddress receiverIP, Keys k) {
        try {
            /* This should be removed soon:*/
            this.seqToCondition = new ConcurrentHashMap<>();
            this.seqToPdu = new ConcurrentHashMap<>();
            /* */
            this.socket = new DatagramSocket(defaultPort);
            this.expectedSize = 2048;
            this.pdus = new ArrayList<>();
            this.datagrams = new ArrayList<>();
            this.lock = new ReentrantLock();
            this.pduArrived = lock.newCondition();
            this.datagramArrived = lock.newCondition();
            this.expectedIP = receiverIP;
            this.buffer = new byte[expectedSize];
            this.knownIpsToReceivedDatagrams = new ConcurrentHashMap<>();
            this.aesKeyEncryption = false;
            this.communicationKeys = k;
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
            long timeLeft = timeout * 1000;
            while(this.pdus.isEmpty() || timeLeft > 0) {
                timeLeft = this.pduArrived.awaitNanos(timeLeft);
            }
            if (this.pdus.size() > 0)
                return this.pdus.remove(0);
            else
                return null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    public PDU getFIFO() {
        try {
            lock.lock();
            while(this.pdus.isEmpty()) {
                this.pduArrived.await();
            }
            if (this.pdus.size() > 0)
                return this.pdus.remove(0);
            else
                return null;
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
                this.datagramArrived.signal();
            }
            finally {
                lock.unlock();
            }
        }
    }

    /**
     * Given a sequence number, and a timeout, waits that timeout for a PDU
     * @param seqNumber number that comes in the ack field of the PDU that should arrive
     * @param timeout number of milisseconds before timeout
     * @return ack or null, if timed out
     */
    public PDU getAck(long seqNumber, long timeout) {
        Condition c = this.lock.newCondition();
        this.seqToCondition.put(seqNumber,c);
        try {
            lock.lock();
            long timeLeft = timeout * 1000; // in nano seconds
            PDU response = null;
            while(!this.seqToPdu.containsKey(seqNumber) && timeLeft > 0) {
                timeLeft = this.pduArrived.awaitNanos(timeLeft);
            }
            response = this.seqToPdu.get(seqNumber);

            return response;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    public void activateAESKeyEncryption() {
        this.aesKeyEncryption = true;
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
                    while(r.datagrams.isEmpty())
                        r.datagramArrived.await();

                    byte[] datagram = r.datagrams.get(0);
                    if (aesKeyEncryption)
                        datagram = communicationKeys.decryptAES(datagram);

                    PDU p = DatagramParser.processDatagram(datagram);
                    System.out.println("Processed: \n"  + p.toString());
                    r.datagrams.remove(0);
                    r.pdus.add(p);
                    r.pduArrived.signalAll();
                    r.seqToPdu.put(p.getSeqNumber(),p);
                    Condition c = r.seqToCondition.get(p.getSeqNumber());
                    if (c != null)
                        c.signal();
                } catch (InterruptedException | InvalidTypeOfDatagram | InvalidCRCException e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
