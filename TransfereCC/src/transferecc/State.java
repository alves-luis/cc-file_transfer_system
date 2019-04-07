package transferecc;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class State {

    private long seqNumber;
    private boolean conectionEstablished;
    private InetAddress senderIP;
    private long lastReceivedDatagram;
    private byte[] file;

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
}
