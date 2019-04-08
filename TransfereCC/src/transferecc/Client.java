package transferecc;

import agenteudp.PDU;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
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

    private void sendDatagram(PDU datagram, String destIP) {
        try {
            InetAddress address = InetAddress.getByName(destIP);
            sender.sendDatagram(datagram,address);
        }
        catch (UnknownHostException e) {
            System.err.println("Unknown host!" + destIP);
        }
    }

    public boolean startConnection(String destIP) {
        int num_tries = 3;
        long timeout = 36000;
        state.setSenderIP(destIP);

        ConnectionRequest request = new ConnectionRequest(state.genNewSeqNumber());
        sendDatagram(request,destIP);

        while(num_tries > 0) { // awaits an ack
            PDU response = receiver.getFIFO(timeout);
            if (response == null) { // timed out
                num_tries--;
                continue;
            }
            state.receivedDatagram(response.getTimeStamp());

            if (response instanceof Ack) {
                Ack ack = (Ack) response;
                return true;
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



    @Override
    public void run() {
        new Thread(receiver).start();
    }
}
