package agenteudp;

import agenteudp.control.Ack;
import agenteudp.control.AuthenticationRequest;
import agenteudp.control.ConnectionRequest;
import agenteudp.control.ConnectionTermination;
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
    private ArrayList<PDU> pdus;
    private ReentrantLock lock;
    private Condition pduArrived;

    public Receiver(int port) {
        try {
            this.socket = new DatagramSocket(port);
            this.expectedSize = 1024;
            this.pdus = new ArrayList<>();
            this.lock = new ReentrantLock();
            this.pduArrived = lock.newCondition();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    /**
     * Method that blocks until a datagram is received. Returns the content of that datagram
     * @return
     */
    public byte[] receiveDatagram() {
        byte[] data = null;
        try {
            byte[] buffer = new byte[this.expectedSize];
            DatagramPacket packet = new DatagramPacket(buffer,this.expectedSize);
            socket.receive(packet);
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

    public PDU processDatagram(byte[] data) throws InvalidCRCException, InvalidTypeOfDatagram {
        PDU result = null;
        byte[] checksum = getChecksum(data);

        if (!verifyIntegrity(data,checksum))
            throw new InvalidCRCException("Invalid CRC!");

        byte type = getType(data);
        byte subtype = getSubtype(data);
        switch(type) {
            case PDUTypes.CONTROL:
                result = parseControl(data,subtype);
                break;
            case PDUTypes.MANAGEMENT:
                result = parseManagement(data,subtype);
                break;
            case PDUTypes.DATA:
                result = parseData(data,subtype);
                break;
        }
        return result;
    }

    private static PDU parseManagement(byte[] data, byte subtype) {
        switch(subtype) {
            case PDUTypes.M_FILE:
                return FileID.degeneratePDU(data);
            case PDUTypes.M_TYPE:
                return null; // will add SOON!
        }
        return null;
    }

    private static PDU parseControl(byte[] data, byte subtype) {
        switch(subtype) {
            case PDUTypes.C_ACK:
                return Ack.degeneratePDU(data);
            case PDUTypes.C_AUTHENTICATION_REQUEST:
                return AuthenticationRequest.degeneratePDU(data);
            case PDUTypes.C_CONNECTION_REQUEST:
                return ConnectionRequest.degeneratePDU(data);
            case PDUTypes.C_CONNECTION_TERMINATION:
                return ConnectionTermination.degeneratePDU(data);
        }
        return null;
    }

    private static PDU parseData(byte[] data, byte subtype) {
        return null;
    }

    private static byte getType(byte[] data) throws InvalidTypeOfDatagram {
        if (data.length > 8)
            return data[8];
        else
            throw new InvalidTypeOfDatagram();
    }

    private static byte getSubtype(byte[] data) throws InvalidTypeOfDatagram {
        if (data.length > 9)
            return data[9];
        else
            throw new InvalidTypeOfDatagram();
    }

    private static byte[] getChecksum(byte[] data) {
        byte[] checksum = Arrays.copyOfRange(data,0,8);
        return checksum;
    }

    /**
     * Given an array of data, and an array of bytes with the checksum, returns true
     * if the crc of the data matches the checksum value
     * @param data
     * @param checksumValue
     * @return true if they match
     */
    private static boolean verifyIntegrity(byte[] data, byte[] checksumValue) {
        CRC32 crc = new CRC32();
        crc.update(data,8,data.length-8);
        long sum = crc.getValue();
        long checksum = ByteBuffer.wrap(checksumValue,0,8).getLong();
        return checksum == sum;
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

            if (!this.pdus.isEmpty())
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

    @Override
    public void run() {
        System.out.println("Socket is connected!");
        while(true) {
            byte[] datagram = this.receiveDatagram();
            try {
                PDU p = this.processDatagram(datagram);
                System.out.println("Received: \n" + p.toString());
                this.pdus.add(p);
                try {
                    this.lock.lock();
                    this.pduArrived.signalAll();
                }
                finally {
                    this.lock.unlock();
                }
            } catch (InvalidCRCException | InvalidTypeOfDatagram e) {
                e.printStackTrace();
            }
        }
    }
}
