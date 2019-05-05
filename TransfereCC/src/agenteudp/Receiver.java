package agenteudp;

import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.control.ConnectionTermination;
import agenteudp.control.KeyExchange;
import agenteudp.data.BlockData;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;
import security.Keys;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Receiver implements Runnable {

    public static int DEFAULT_PORT = 5555;

    private DatagramSocket socket;
    private ArrayList<DatagramPacket> datagrams;
    private ArrayList<PDU> pdus;
    private ReentrantLock lock;
    private Condition pduArrived;
    private Condition datagramArrived;
    private InetAddress expectedIP;
    private boolean running;
    private ConcurrentHashMap<Long,Condition> seqToCondition;
    private ConcurrentHashMap<Long,PDU> seqToPdu;
    private boolean aesKeyEncryption; // if should use aesKeyDecryption;
    private Keys communicationKeys;
    private InetAddress senderIP; // used when receiving a connection request


    public Receiver(int defaultPort, InetAddress receiverIP, Keys k) {
        try {
            /* This should be removed soon:*/
            this.seqToCondition = new ConcurrentHashMap<>();
            this.seqToPdu = new ConcurrentHashMap<>();
            /* */
            this.socket = new DatagramSocket(defaultPort);
            this.pdus = new ArrayList<>();
            this.datagrams = new ArrayList<>();
            this.lock = new ReentrantLock();
            this.pduArrived = lock.newCondition();
            this.datagramArrived = lock.newCondition();
            this.expectedIP = receiverIP;
            this.aesKeyEncryption = false;
            this.communicationKeys = k;
            this.senderIP = null;
        }
        catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that blocks until a datagram is received. Returns the content of that datagram
     * @return
     */
    public DatagramPacket receiveDatagram() {
        boolean valid = false;
        try {
            DatagramPacket packet = new DatagramPacket(new byte[64480] ,64480);

            while(!valid) {
                socket.receive(packet);
                InetAddress receiverIP = packet.getAddress();
                if (this.expectedIP == null || receiverIP.equals(this.expectedIP))
                    valid = true;
            }
            return packet;
        } catch (IOException e) {
        }
        return null;
    }

    /**
     * Sets the timeout of this socket
     * @param timeout in ms
     */
    public void setTimeout(int timeout) {
        try {
            this.socket.setSoTimeout(timeout);
        } catch (SocketException e) {
            System.err.println(e.toString());
        }
    }


    private synchronized boolean running() {
        return this.running;
    }

    public synchronized void stopRunning() {
        this.running = false;
        this.socket.close();
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
            DatagramPacket datagram = this.receiveDatagram();
            try {
                lock.lock();
                this.datagrams.add(datagram);
                this.datagramArrived.signal();
            }
            finally {
                lock.unlock();
            }
        }
        System.out.println("Socket is disconnected!");
    }

    /**
     * Given a sequence number, and a timeout, waits that timeout for an Ack
     * @param seqNumber number that comes in the ack field of the PDU that should arrive
     * @param timeout number of milisseconds before timeout
     * @return ack or null, if timed out
     */
    public Ack getAck(long seqNumber, long timeout) {
        Condition c = this.lock.newCondition();
        this.seqToCondition.put(seqNumber,c);
        try {
            lock.lock();
            long timeLeft = timeout * 1000000; // in nano seconds
            Ack response = null;
            while(!this.seqToPdu.containsKey(seqNumber) && timeLeft > 0) {
                timeLeft = c.awaitNanos(timeLeft);
            }
            // if did not timeout, update ack to the processed ack
            if (this.seqToPdu.containsKey(seqNumber)) {
                response = (Ack) this.seqToPdu.remove(seqNumber);
                this.pdus.remove(0);
            }

            return response;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    /**
     * Method that is used when is expecting a keyexchange packet
     * returns either the keyexchange or null, if not a key exchange or timed out
     * @param timeout timeout value in ms
     * @return pdu
     */
    public KeyExchange getKeyExchange(long timeout) {
        try {
            lock.lock();
            long timeoutInNanos = timeout * 1000000;
            while(this.pdus.isEmpty() && timeoutInNanos > 0) {
                timeoutInNanos = this.pduArrived.awaitNanos(timeoutInNanos);
            }
            if (this.pdus.size() > 0) {
                PDU latest = this.pdus.remove(0);
                if (latest instanceof KeyExchange)
                    return (KeyExchange) latest;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    /**
     * Method that is used when is expecting a fileID packet
     * returns either the fileID or null, if not a fileID or timed out
     * @param timeout timeout value in ms
     * @return pdu
     */
    public FileID getFileID(long timeout) {
        try {
            lock.lock();
            long timeoutInNanos = timeout * 1000000;
            while(this.pdus.isEmpty() && timeoutInNanos > 0) {
                timeoutInNanos = this.pduArrived.awaitNanos(timeoutInNanos);
            }
            if (this.pdus.size() > 0) {
                PDU latest = this.pdus.remove(0);
                if (latest instanceof FileID)
                    return (FileID) latest;
            }
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

    public void deactivateAESKeyEncryption() {
        this.aesKeyEncryption = false;
    }

    /**
     * Method that waits until a connection request has arrived
     * @return Connection request or null if invalid pdu
     */
    public ConnectionRequest getConnectionRequest() {
        try {
            lock.lock();
            while(this.pdus.isEmpty()) {
                this.pduArrived.await();
            }
            PDU latest = this.pdus.remove(0);
            if (latest instanceof ConnectionRequest)
                return (ConnectionRequest) latest;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    /**
     * Method that is used when is expecting a FirstBlockData packet
     * returns either the FirstBlockData or null, if not a FirstBlockData or timed out
     * @param timeout timeout value in ms
     * @return pdu
     */
    public FirstBlockData getFirstBlockData(long timeout) {
        try {
            lock.lock();
            long timeoutInNanos = timeout * 1000000;
            while(this.pdus.isEmpty() && timeoutInNanos > 0) {
                timeoutInNanos = this.pduArrived.awaitNanos(timeoutInNanos);
            }
            if (this.pdus.size() > 0) {
                PDU latest = this.pdus.remove(0);
                if (latest instanceof FirstBlockData)
                    return (FirstBlockData) latest;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    /**
     * Method that is used when is expecting a BlockData packet
     * returns either the BlockData or null, if not a BlockData or timed out
     * @param timeout timeout value in ms
     * @return pdu
     */
    public BlockData getBlockData(long timeout) {
        try {
            lock.lock();
            long timeoutInNanos = timeout * 1000000;
            while(this.pdus.isEmpty() && timeoutInNanos > 0) {
                timeoutInNanos = this.pduArrived.awaitNanos(timeoutInNanos);
            }
            if (this.pdus.size() > 0) {
                PDU latest = this.pdus.remove(0);
                if (latest instanceof BlockData)
                    return (BlockData) latest;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
    }

    public InetAddress getSenderIP() {
        return this.senderIP;
    }

    /**
     * Method that is used when is expecting a ConnectionTermination packet
     * returns either the ConnectionTermination or null, if not a ConnectionTermination or timed out
     * @param retransmissionTimeout timeout value in ms
     * @return pdu
     */
    public ConnectionTermination getConnectionTermination(long retransmissionTimeout) {
        try {
            lock.lock();
            long timeoutInNanos = retransmissionTimeout * 1000000;
            while(this.pdus.isEmpty() && timeoutInNanos > 0) {
                timeoutInNanos = this.pduArrived.awaitNanos(timeoutInNanos);
            }
            if (this.pdus.size() > 0) {
                PDU latest = this.pdus.remove(0);
                if (latest instanceof ConnectionTermination)
                    return (ConnectionTermination) latest;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            lock.unlock();
        }
        return null;
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

                    DatagramPacket packet = r.datagrams.get(0);
                    if (packet == null) {
                        continue;
                    }

                    // truncate packet
                    int length = packet.getLength();
                    int offset = packet.getOffset();
                    byte[] data = new byte[length];
                    System.arraycopy(packet.getData(),offset,data,0,length);

                    if (aesKeyEncryption)
                        data = communicationKeys.decryptAES(data);

                    PDU p = DatagramParser.processDatagram(data);
                    System.out.println("---- PROCESSED ----\n"  + p.toString());

                    // if it's a connection request, update the IP field
                    if (p instanceof ConnectionRequest)
                        r.senderIP = packet.getAddress();


                    r.datagrams.remove(0);
                    r.pdus.add(p);
                    r.pduArrived.signalAll();
                    // if it's an ack, alert the ack receiver
                    if (p instanceof Ack) {
                        Ack ack = (Ack) p;
                        r.seqToPdu.put(ack.getAck(), ack);
                        Condition c = r.seqToCondition.get(ack.getAck());
                        if (c != null)
                            c.signal();
                    }
                } catch (InterruptedException | InvalidTypeOfDatagram | InvalidCRCException e) {
                    r.datagrams.remove(0);
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
