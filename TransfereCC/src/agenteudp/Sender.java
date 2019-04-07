package agenteudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Sender {

    private DatagramSocket socket;
    private int destPort;
    private long seqNumber;

    public Sender(int destPort) {
        try {
            this.socket = new DatagramSocket(6666);
            this.destPort = destPort;
            this.seqNumber = 0;
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public void sendDatagram(PDU datagram, String destIP) {
        byte[] content = datagram.generatePDU();
        try{
            InetAddress address = InetAddress.getByName(destIP);
            DatagramPacket packet = new DatagramPacket(content,content.length,address,destPort);
            socket.send(packet);
        }
        catch(UnknownHostException e){
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("Exceção a enviar o pacote!");
        }
    }



}
