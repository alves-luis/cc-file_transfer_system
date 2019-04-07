package agenteudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Sender {

    public static int DEFAULT_PORT = 6666;

    private DatagramSocket socket;
    private int destPort;

    public Sender(int senderPort, int destPort) {
        try {
            this.socket = new DatagramSocket(senderPort);
            this.destPort = destPort;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void sendDatagram(PDU datagram, InetAddress address) {
        byte[] content = datagram.generatePDU();
        try{
            DatagramPacket packet = new DatagramPacket(content,content.length,address,destPort);
            socket.send(packet);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
