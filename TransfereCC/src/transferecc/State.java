package transferecc;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

    public State() {
        this.seqNumber = 0;
        this.piecesOfFile = new TreeMap<>();
    }

    public long genNewSeqNumber() {
        return this.seqNumber++;
    }

    public InetAddress getSenderIP() {
        return senderIP;
    }

    public void setSenderIP(String senderIP) {
        try {
            this.senderIP = InetAddress.getByName(senderIP);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void receivedDatagram(long when) {
        this.lastReceivedDatagram = when;
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
}
