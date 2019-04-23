package transferecc;

import security.Keys;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Random;
import java.util.TreeMap;

public class State {

    private long seqNumber;
    private boolean conectionEstablished;
    private boolean transferComplete;
    private InetAddress senderIP;
    private InetAddress receiverIP;
    private int senderPort;
    private int receiverPort;
    private long lastReceivedDatagram;
    private TreeMap<Integer,byte[]> piecesOfFile;
    private long fileID;
    private long fileSize;
    private byte[] hashOfFile;
    private int offset;
    private Keys communicationKeys;
    private boolean sentAesKey;

    public State() {
        this.seqNumber = Math.abs(new Random().nextLong());
        this.piecesOfFile = new TreeMap<>();
        this.offset = 0;
        this.communicationKeys = new Keys();
    }

    public long genNewSeqNumber() {
        return this.seqNumber++;
    }

    public InetAddress getSenderIP() {
        return senderIP;
    }

    public TreeMap<Integer,byte[]> getTreeMap(){
        return this.piecesOfFile;
    }

    public void setSenderIP(String senderIP) {
        try {
            this.senderIP = InetAddress.getByName(senderIP);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void receivedDatagram() {
        this.lastReceivedDatagram = Instant.now().toEpochMilli();
    }

    /**
     * Given an array of bytes that represents a file,
     * returns the ID associated with that file.
     * @param id
     * @return
     */
    public void setFile(long id) {
        this.fileID = id;
    }

    public void setStartingSeqNumber(long seqNumber) {
        this.seqNumber = seqNumber;
    }


    public void sentPieceOfFile(byte[] piece, int offset) {
        this.piecesOfFile.put(offset,piece);
        this.offset += piece.length;
    }

    public void setFileAsTransfered() {
        // TO DO
    }

    public void setFileSize(long size) {
        this.fileSize = size;
    }

    public void setHashOfFile(byte[] hash) {
        this.hashOfFile = hash;
    }

    public void setConectionEstablished() {
        this.conectionEstablished = true;
    }

    public long getFileSize() {
        return this.fileSize;
    }

    public int getOffset() {
        return this.offset;
    }

    public long getFileID() {
        return this.fileID;
    }

    public void setConectionEnded() {
        this.conectionEstablished = false;
    }

    public void setAESKey(byte[] aesKey) {
        this.communicationKeys.setAESKey(aesKey);
    }

    public byte[] encryptAESKey(byte[] pubKey) {
        return this.communicationKeys.encryptRSA(pubKey);
    }

    public byte[] getRSAPublicKey() {
        return this.communicationKeys.getRSAPublicKey();
    }

    public byte[] decryptWithMyRSA(byte[] data) {
        return this.communicationKeys.decryptRSA(data);
    }

    public void sentAESKey() {
        this.sentAesKey = true;
    }

    public boolean isSentAESKey() {
        return this.sentAesKey;
    }

    public Keys getKeys() {
        return this.communicationKeys;
    }
}
