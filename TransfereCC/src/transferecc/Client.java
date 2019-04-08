package transferecc;

import agenteudp.PDU;
import agenteudp.PDUTypes;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;

import java.net.InetAddress;
import java.net.UnknownHostException;


public class Client implements Runnable {


    private Sender sender;
    private Receiver receiver;
    private State state;

    public Client(int destPort) {
        this.sender = new Sender(Sender.DEFAULT_PORT, destPort);
        this.receiver = new Receiver(Receiver.DEFAULT_PORT);
        this.state = new State();
    }


    public boolean startConnection(String destIP) {
        int num_tries = 3;
        long timeout = 36000;
        state.setSenderIP(destIP);

        ConnectionRequest request = new ConnectionRequest(state.genNewSeqNumber());
        sender.sendDatagram(request,state.getSenderIP());

        while(num_tries > 0) { // awaits an ack
            PDU response = receiver.getFIFO(timeout);
            if (response == null) { // timed out
                num_tries--;
                continue;
            }
            state.receivedDatagram(response.getTimeStamp());

            if (response instanceof Ack) {
                Ack ack = (Ack) response;
                long whatAcks = ack.getAck();
                if (whatAcks == request.getSeqNumber()) {
                    state.setConectionEstablished();
                    return true;
                }
            }
            else {
                num_tries--;
            }
        }
        return false;
    }

    public boolean endConnection() {
        return false; // TO DO
    }

    public boolean requestFile(long fileID) {
        int num_tries = 3;
        long timeout = 36000;

        FileID requestFile = new FileID(state.genNewSeqNumber(),PDUTypes.M_FILE,fileID);
        sender.sendDatagram(requestFile,state.getSenderIP());
        while(num_tries > 0) {
            PDU response = receiver.getFIFO(timeout);
            if (response == null) { // timed out
                num_tries--;
                continue;
            }
            state.receivedDatagram(response.getTimeStamp());

            if (response instanceof FirstBlockData) {
                FirstBlockData firstBlock = (FirstBlockData) response;
                state.setFileSize(firstBlock.getFileSize());
                byte[] content = firstBlock.getData();
                byte[] hash = firstBlock.getHash();
                state.setHashOfFile(hash);
                state.sentPieceOfFile(content,0);
                return true;
            }
        }
        return false;

    }



    @Override
    public void run() {
        new Thread(receiver).start();
    }
}
