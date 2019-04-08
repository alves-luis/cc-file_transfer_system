package transferecc;

import agenteudp.PDU;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.data.FirstBlockData;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Client implements Runnable {

    private static int DEFAULT_HEADER_DATA_SIZE = 128;

    private Sender sender;
    private Receiver receiver;
    private State state;

    public Client(int destPort) {
        this.sender = new Sender(Sender.DEFAULT_PORT, destPort);
        this.receiver = new Receiver(Receiver.DEFAULT_PORT);
        this.state = new State();
    }

    private void sendDatagram(PDU datagram, String destIP) {
        try {
            InetAddress address = InetAddress.getByName(destIP);
            sender.sendDatagram(datagram,address);
        }
        catch (UnknownHostException e) {
            System.err.println("Unknown host!" + destIP);
        }
    }

    public boolean startConnection(String destIP) {
        int num_tries = 3;
        long timeout = 36000;
        state.setSenderIP(destIP);

        ConnectionRequest request = new ConnectionRequest(state.genNewSeqNumber());
        sendDatagram(request,destIP);

        while(num_tries > 0) { // awaits an ack
            PDU response = receiver.getFIFO(timeout);
            if (response == null) { // timed out
                num_tries--;
                continue;
            }
            state.receivedDatagram(response.getTimeStamp());

            if (response instanceof Ack) {
                Ack ack = (Ack) response;
                return true;
            }
            else {
                num_tries--;
            }
        }
        return false;
    }

    /**
     * This method, given an instance of a File, sends its header
     * @param file file to send the header
     * @return
     */
    public boolean sendFileHeader(File file) {
        try {
            long seqNumber = state.genNewSeqNumber();
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] hashOfFile = getHashValue(fileBytes);
            long fileSize = file.length();
            long fileID = state.setFile(fileBytes);
            InetAddress IP = state.getSenderIP();
            byte[] data = getFirstChunkOfData(fileBytes);
            FirstBlockData header = new FirstBlockData(seqNumber,fileID,fileSize,hashOfFile,data);

            int numTries = 3;
            long timeout = 36000;
            while(numTries > 0) {
                sender.sendDatagram(header,IP);

                PDU response = receiver.getFIFO(timeout);
                if (response == null) { // timed out
                    numTries--;
                    continue;
                }
                state.receivedDatagram(response.getTimeStamp());
                if (response instanceof Ack) {
                    long seq = response.getSeqNumber();
                    if (seq == seqNumber+1) {

                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return false;
    }


    /** Given an array of bytes, returns itsfile hash using SHA-1
     * @param data
     * @return
     */
    private static byte[] getHashValue(byte[] data){
        byte[] sol = null;
        try{
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            sol = md.digest(data);
        }
        catch(NoSuchAlgorithmException e){;}
        return sol;
    }

    /**
     * Given the file as byte[], returns the data sent on the
     * first block
     * @param data
     * @return
     */
    private static byte[] getFirstChunkOfData(byte[] data) {
        byte[] result = new byte[DEFAULT_HEADER_DATA_SIZE];
        System.arraycopy(data,0,result,0,DEFAULT_HEADER_DATA_SIZE);
        return result;
    }


    @Override
    public void run() {
        new Thread(receiver).start();
    }
}
