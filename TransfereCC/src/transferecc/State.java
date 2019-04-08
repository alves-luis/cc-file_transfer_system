package transferecc;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

public class State {

    private long seqNumber;
    private boolean conectionEstablished;
    private InetAddress senderIP;
    private long lastReceivedDatagram;
    private byte[] file;
    private int indexSent; // status of sent file
    private long fileID;

    public State() {
        this.seqNumber = 0;
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

    /** Given an array of bytes that represents a file,
     * returns the ID associated with that file.
     * @param file
     * @return
     */
    public long setFile(byte[] file) {
        this.file = file;
        this.fileID = new Random().nextLong();
        return fileID;
    }

    public void setStartingSeqNumber(long seqNumber) {
        this.seqNumber = seqNumber;
    }
}
