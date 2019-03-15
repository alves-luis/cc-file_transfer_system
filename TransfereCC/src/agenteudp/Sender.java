package agenteudp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Sender {

    private DatagramSocket socket;
    private int destPort;

    public Sender(int destPort) {
        try {
            this.socket = new DatagramSocket(5555);
            this.destPort = destPort;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    
    public void sendDatagram(PDU datagram, String destIP) {
        byte[] content = datagram.generatePDU();
        try{
            InetAddress address= InetAddress.getByName(destIP);
            DatagramPacket packet= new DatagramPacket(content,content.length,address,destPort);

        }
        catch(UnknownHostException e){
            System.err.println("Opah, não sei que se passou");
            
        }
    }


}
