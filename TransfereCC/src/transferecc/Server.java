package transferecc;

import agenteudp.PDU;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.control.ConnectionTermination;
import agenteudp.control.KeyExchange;
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
import java.util.HashMap;

public class Server implements Runnable {

    public static int DEFAULT_TIMEOUT = 30000; // 3 seconds
    public static int DEFAULT_TIMEOUT_TRIES = 5; // number of tries before timing out

    private static int DEFAULT_SENDING_PORT = 4444;
    private static int DEFAULT_RECEIVING_PORT = 7777;
    private static int DEFAULT_CLIENT_PORT = 5555;
    private static int DEFAULT_HEADER_DATA_SIZE = 512;
    private static long DEFAULT_FILE_ID = 1;

    private Sender sender;
    private Receiver receiver;
    private State state;
    private HashMap<Long,String> files;

    public Server() {
        this.state = new State();
        this.files = new HashMap<>();
        this.files.put((long) 123,"programa_teste.txt"); // testing purposes
        this.receiver = new Receiver(Server.DEFAULT_RECEIVING_PORT, null,state.getKeys());
        this.sender = new Sender(Server.DEFAULT_SENDING_PORT,DEFAULT_CLIENT_PORT, state.getKeys());
    }

    /**
     * This method, given an instance of a File, sends its header
     * @param file file to send the header
     * @return
     */
    public boolean sendFile(File file) {
        try {
            long seqNumber = state.genNewSeqNumber();
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            byte[] hashOfFile = FirstBlockData.getHash(fileBytes);
            long fileSize = file.length();
            long fileID = DEFAULT_FILE_ID;

            state.setFile(fileID);
            state.setFileSize(fileSize);
            state.setHashOfFile(hashOfFile);

            byte[] data = getFirstChunkOfData(fileBytes);
            state.sentPieceOfFile(data,0);
            FirstBlockData header = new FirstBlockData(seqNumber,fileID,fileSize,hashOfFile,data);
            boolean sent = sendPacketWithResponse(header);
            // if need to send more pieces
            while (state.getOffset() < state.getFileSize()) {
                byte[] filePiece = getChunkOfData(fileBytes,state.getOffset());
                sent = this.sendFilePiece(filePiece,state.getOffset());
            }
            return sent;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Given a piece of a file on a given offset, sends it
     * @param piece Piece of file to send
     * @param offset offset of that piece
     * @return true if managed to send it
     */
    private boolean sendFilePiece(byte[] piece, int offset) {
        BlockData packet = new BlockData(state.genNewSeqNumber(),state.getFileID(),offset,piece);
        boolean success = sendPacketWithResponse(packet);
        state.sentPieceOfFile(piece,offset);
        return success;
    }

    /**
     * This method, given a packet, keeps sending it until
     * it receives an ack or reponse, or gives up if timed out
     * @param packet packet to send and wait for ack
     * @return true if packet sent and received an ack
     */
    private boolean sendPacketWithResponse(PDU packet) {
        int num_tries = 3;
        boolean timedOut = false;
        while(num_tries > 0 && !timedOut) {
            sender.sendDatagram(packet,state.getSenderIP());
            PDU response = receiver.getFIFO(Server.DEFAULT_TIMEOUT);
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
                else if (response instanceof KeyExchange) {
                    KeyExchange keyEx = (KeyExchange) response;
                    byte[] aesKey = state.decryptWithMyRSA(keyEx.getKey());
                    state.setAESKey(aesKey);
                    this.receiver.activateAESKeyEncryption();
                    this.sender.activateAESKeyEncryption();
                    sendAck(keyEx);
                    return true;
                }
            }
        }
        return false;

    }

    public boolean receiveConnectionRequest(String ip) {
        int num_tries = 3;
        while(num_tries > 0) {
            PDU p = receiver.getFIFO();
            try {
                InetAddress address = InetAddress.getByName(ip);
                if (p instanceof ConnectionRequest) {
                    state.setStartingSeqNumber(p.getSeqNumber() + 1);
                    state.setSenderIP(address.getHostAddress());
                    receiver.setExpectedIP(state.getSenderIP());
                    sendPacketWithResponse(new KeyExchange(state.genNewSeqNumber(),state.getRSAPublicKey()));
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

    /**
     * Given the file as byte[], returns the data sent on the
     * first block
     * @param data first chunk of file
     * @return returns the piece that should be sent along with the header
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
     * @param data array to retrieve the chunk from
     * @param offset index from which to retrieve the chunk
     * @return returns the piece that should be sent
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
            PDU p = receiver.getFIFO();
            state.receivedDatagram();
            if (p instanceof FileID) {
                FileID packet = (FileID) p;
                long fileId = packet.getFileID();
                String filePath = this.files.get(fileId);
                if (filePath != null) {
                    File f = new File(filePath);
                    return this.sendFile(f);
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

    public void terminateConnection(PDU p){

        sendAck(p);
        this.state=null;


    }

    public void sendAck(PDU p){
        Ack ack = new Ack(state.genNewSeqNumber(), p.getSeqNumber());
        sender.sendDatagram(ack, state.getSenderIP());
    }


    public boolean receiveConnectionTermination() {
        int num_tries = 3;
        while (num_tries > 0) {
            PDU p = receiver.getFIFO(Server.DEFAULT_TIMEOUT);
            state.receivedDatagram();
            if (p instanceof ConnectionTermination) {
                terminateConnection(p);
                return true;
            }
        }
        return false;
    }
}
