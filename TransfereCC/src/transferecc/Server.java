package transferecc;

import agenteudp.PDU;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Server implements Runnable {

    private static long DEFAULT_TIMEOUT = 500000000; // half a second
    private static int DEFAULT_SENDING_PORT = 4444;
    private static int DEFAULT_RECEIVING_PORT = 7777;
    private static int DEFAULT_CLIENT_PORT = 5555;
    private static int DEFAULT_HEADER_DATA_SIZE = 128;
    private static long DEFAULT_FILE_ID = 1;

    private Sender sender;
    private Receiver receiver;
    private State state;

    public Server() {
        this.sender = new Sender(Server.DEFAULT_SENDING_PORT,DEFAULT_CLIENT_PORT);
        this.receiver = new Receiver(Server.DEFAULT_RECEIVING_PORT);
        this.state = new State();
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
            long fileID = DEFAULT_FILE_ID;
            state.setFile(fileID);
            InetAddress IP = state.getSenderIP();
            byte[] data = getFirstChunkOfData(fileBytes);
            FirstBlockData header = new FirstBlockData(seqNumber,fileID,fileSize,hashOfFile,data);

            int numTries = 3;
            long timeout = 36000;
            while(numTries > 0) {
                sender.sendDatagram(header,IP); // sends header
                PDU response = receiver.getFIFO(timeout);
                if (response == null) { // timed out
                    numTries--;
                    continue;
                }
                state.receivedDatagram(response.getTimeStamp());
                if (response instanceof Ack) {
                    Ack ack = (Ack) response;
                    if (ack.getAck() == header.getSeqNumber()) {
                        return true;
                    }
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendFilePiece(byte[] piece) {
        return false; // TO DO
    }

    public boolean receiveConnectionRequest(String ip) {
        int num_tries = 3;
        while(num_tries > 0) {
            PDU p = receiver.getFIFO(Server.DEFAULT_TIMEOUT);
            try {
                InetAddress address = InetAddress.getByName(ip);
                if (p instanceof ConnectionRequest) {
                    state.setStartingSeqNumber(p.getSeqNumber() + 1);
                    sender.sendDatagram(new Ack(state.genNewSeqNumber(),p.getSeqNumber()), address);
                    state.setSenderIP(address.getHostAddress());
                    state.receivedDatagram(p.getTimeStamp());
                    return true;
                }
                else {
                    num_tries--;
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /** Given an array of bytes, returns its file hash using SHA-1
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
        byte[] result = new byte[data.length];
        System.arraycopy(data,0,result,0,data.length);
        return result;
    }

    @Override
    public void run() {
        new Thread(receiver).start();
    }

    public boolean receiveFileRequest() {
        int num_tries = 3;
        while(num_tries > 0) {
            PDU p = receiver.getFIFO(Server.DEFAULT_TIMEOUT);
            state.receivedDatagram(p.getTimeStamp());
            if (p instanceof FileID) {
                File f = new File("programa_teste.txt");
                this.sendFileHeader(f);
                return true;
            }
            else {
                num_tries--;
            }
        }
        return false;
    }
}
