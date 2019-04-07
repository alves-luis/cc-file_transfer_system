package transferecc;

import agenteudp.PDU;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Server implements Runnable {

    private static long DEFAULT_TIMEOUT = 500000000; // half a second
    private static int DEFAULT_SENDING_PORT = 4444;
    private static int DEFAULT_RECEIVING_PORT = 7777;
    private static int DEFAULT_CLIENT_PORT = 5555;

    private Sender sender;
    private Receiver receiver;
    private State state;

    public Server() {
        this.sender = new Sender(Server.DEFAULT_SENDING_PORT,DEFAULT_CLIENT_PORT);
        this.receiver = new Receiver(Server.DEFAULT_RECEIVING_PORT);
        this.state = new State();
    }

    @Override
    public void run() {
        new Thread(receiver).start();
        while(true) {
            PDU p = receiver.getFIFO(Server.DEFAULT_TIMEOUT);
            try {
                InetAddress address = InetAddress.getByName("localhost");
                if (p instanceof ConnectionRequest)
                    sender.sendDatagram(new Ack(p.getSeqNumber()+1),address);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }
}
