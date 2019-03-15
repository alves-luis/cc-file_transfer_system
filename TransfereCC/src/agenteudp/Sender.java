package agenteudp;

import java.net.DatagramSocket;
import java.net.SocketException;

public class Sender {

    private DatagramSocket socket;
    private int destPort;

    public Sender(int destPort) {
        try {
            this.socket = new DatagramSocket(5555);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }


}
