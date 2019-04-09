package transferecc;

import agenteudp.PDU;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.data.BlockData;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;

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
    private HashMap<Long,String> files;

    public Server() {
        this.sender = new Sender(Server.DEFAULT_SENDING_PORT,DEFAULT_CLIENT_PORT);
        this.receiver = new Receiver(Server.DEFAULT_RECEIVING_PORT);
        this.state = new State();
        this.files = new HashMap<>();
        this.files.put((long) 1,"programa_teste.txt"); // testing purposes
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
            state.sentPieceOfFile(data,0);
            FirstBlockData header = new FirstBlockData(seqNumber,fileID,fileSize,hashOfFile,data);

            int numTries = 3;
            long timeout = 36000;
            while(numTries > 0) {
                sender.sendDatagram(header,IP); // sends header

                // if no need to send more pieces, wait for ack
                if(data.length == state.getFileSize()) {
                    PDU response = receiver.getFIFO(timeout);
                    if (response == null) { // timed out
                        numTries--;
                        continue;
                    }
                    state.receivedDatagram();
                    if (response instanceof Ack) {
                        Ack ack = (Ack) response;
                        if (ack.getAck() == header.getSeqNumber()) {
                            return true;
                        }
                    }
                }
                // else send rest of file
                else {
                    int offset = state.getOffset();
                    byte[] filePiece = getChunkOfData(fileBytes,offset);
                    this.sendFilePiece(filePiece,offset);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean sendFilePiece(byte[] piece, int offset) {
        BlockData packet = new BlockData(state.genNewSeqNumber(),state.getFileID(),offset,piece);
        return sendPacketWithAck(packet);
    }

    /**
     * This method, given a packet, keeps sending it until
     * it receives an ack, or gives up if timed out
     * @param packet
     * @return
     */
    public boolean sendPacketWithAck(PDU packet) {
        int num_tries = 3;
        long timeout = 3600;
        boolean timedOut = false;
        while(num_tries > 0 && !timedOut) {
            sender.sendDatagram(packet,state.getSenderIP());
            PDU response = receiver.getFIFO(timeout);
            if (response == null) {
                if (num_tries != 1)
                    num_tries--;
                else
                    timedOut = true;
            }
            else {
                state.receivedDatagram();
                if (response instanceof Ack) {
                    Ack ack = (Ack) response;
                    if (ack.getAck() == packet.getSeqNumber()) {
                        return true;
                    }
                }
            }
        }
        return false;

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
                    state.receivedDatagram();
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
        int sizeOfChunk = data.length > DEFAULT_HEADER_DATA_SIZE ? DEFAULT_HEADER_DATA_SIZE : data.length;
        byte[] result = new byte[sizeOfChunk];
        System.arraycopy(data,0,result,0,sizeOfChunk);
        return result;
    }

    /**
     * Given the byte[] and an offset, returns the truncated byte[]
     * if its length-offset is bigger than DEFAULT HEADER DATA SIZE
     * @param data
     * @param offset
     * @return
     */
    private static byte[] getChunkOfData(byte[] data, int offset) {
        int sizeOfChunk = data.length-offset > DEFAULT_HEADER_DATA_SIZE ? DEFAULT_HEADER_DATA_SIZE : data.length - offset;
        byte[] result = new byte[sizeOfChunk];
        System.arraycopy(data,offset,result,0,sizeOfChunk);
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
            state.receivedDatagram();
            if (p instanceof FileID) {
                FileID packet = (FileID) p;
                long fileId = packet.getFileID();
                String filePath = this.files.get(fileId);
                if (filePath != null) {
                    File f = new File(filePath);
                    this.sendFileHeader(f);
                    return true;
                }
                else
                    return false;
            }
            else {
                num_tries--;
            }
        }
        return false;
    }
}
