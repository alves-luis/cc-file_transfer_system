package transferecc;

import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.data.BlockData;

public class ReliablePiece extends Thread {

    private BlockData data;
    private Sender sender;
    private Receiver receiver;
    private boolean success;
    private int numberOfAttempts;
    private Session session;

    public ReliablePiece(Session ses, Receiver r, Sender s, BlockData block) {
        this.receiver = r;
        this.sender = s;
        this.data = block;
        this.success = false;
        this.numberOfAttempts = 0;
        this.session = ses;
    }

    @Override
    public void run() {
        int numberOfTries = Session.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;

        while(numberOfTries > 0 && !timedOut) {
            sender.sendDatagram(this.data,session.getClientIP());
            Ack response = receiver.getAck(this.data.getSeqNumber(),session.getRetransmissionTimeout());
            // if failed to get ack, retransmit
            if (response == null) {
                this.numberOfAttempts++;
                if (numberOfTries > 1)
                    numberOfTries--;
                else
                    timedOut = true;
            }
            else {
                session.receivedDatagram(response.getTimeStamp());
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
