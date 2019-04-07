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

    public void startConnection(String destIP) {
        int num_tries = 3;
        long timeout = 36000;
        state.setSenderIP(destIP);
        ConnectionRequest request = new ConnectionRequest(state.genNewSeqNumber());
        sendDatagram(request,destIP);
        System.out.println(request.toString());
        while(num_tries > 0) {
            PDU response = receiver.getFIFO(timeout);
            state.receivedDatagram(response.getTimeStamp());
            if (response instanceof Ack) {
                Ack ack = (Ack) response;

                System.out.println(ack.toString());
            }
            else {
                num_tries--;
            }
        }
    }

    @Override
    public void run() {
        new Thread(receiver).start();
    }
}
