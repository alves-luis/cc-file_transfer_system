package transferecc;

import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.data.BlockData;

import java.net.InetAddress;

public class ReliablePiece extends Thread {

    private BlockData data;
    private Sender sender;
    private Receiver receiver;
    private InetAddress destinationIP;
    private long retransmissionTimeout;
    private boolean success;
    private int numberOfAttempts;

    public ReliablePiece(InetAddress destinationIP, long rto, Receiver r, Sender s, BlockData block) {
        this.receiver = r;
        this.sender = s;
        this.data = block;
        this.destinationIP = destinationIP;
        this.retransmissionTimeout = rto;
        this.success = false;
        this.numberOfAttempts = 0;
    }

    @Override
    public void run() {
        int numberOfTries = Session.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;

        while(numberOfTries > 0 && !timedOut) {
            sender.sendDatagram(this.data,this.destinationIP);
            Ack response = receiver.getAck(this.data.getSeqNumber(),this.retransmissionTimeout);
            // if failed to get ack, retransmit
            if (response == null) {
                this.numberOfAttempts++;
                if (numberOfTries > 1)
                    numberOfTries--;
                else
                    timedOut = true;
            }
            else {
                this.success = true;
                break;
            }
        }
    }

    /**
     * Did it run with success?
     * @return success
     */
    public boolean sentReliably() {
        return this.success;
    }

    /**
     * @return number of attempts to send the datagram
     */
    public int getNumberOfAttempts() {
        return this.numberOfAttempts;
    }
}
