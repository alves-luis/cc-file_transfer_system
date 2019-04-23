package agenteudp;

import security.Keys;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Sender {

    public static int DEFAULT_PORT = 6666;

    private DatagramSocket socket;
    private int destPort;
    private Keys communicationKeys;
    private boolean encrypted;

    public Sender(int senderPort, int destPort, Keys k) {
        try {
            this.socket = new DatagramSocket(senderPort);
            this.destPort = destPort;
            this.communicationKeys = k;
            this.encrypted = false;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void sendDatagram(PDU datagram, InetAddress address) {
        byte[] content = datagram.generatePDU();
        if (encrypted)
            content = communicationKeys.encryptAES(content);
        try{
            DatagramPacket packet = new DatagramPacket(content,content.length,address,destPort);
            socket.send(packet);
            System.out.println("Sent: \n"  + datagram.toString());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public void activateAESKeyEncryption() {
        this.encrypted = true;
    }
}
