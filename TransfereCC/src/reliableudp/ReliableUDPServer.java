package reliableudp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author alvesluis
 */
public class ReliableUDPServer implements Runnable {
    
    private DatagramSocket s;
    
    public ReliableUDPServer() {
        try {
            this.s = new DatagramSocket(6969);
        } catch (SocketException ex) {
            Logger.getLogger(ReliableUDPServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        ReliableUDPServer server = new ReliableUDPServer();
        server.run();
    }

    @Override
    public void run() {
        while(true) {
            byte[] buf = new byte[500];
            DatagramPacket p = new DatagramPacket(buf,buf.length);
            try {
                System.out.println("Ainda nao recebi nada!");
                //s.receive(p);
                System.out.println("Agora ja recebi!");
                //String whatTigerSentMe = new String(p.getData());
                //System.out.println("Received " + whatTigerSentMe + " from my favourite Chu!");
                //InetAddress address = p.getAddress();
                //int port = p.getPort();
        
                buf = "oi".getBytes();
                //p.setData(buf);
                InetAddress address = InetAddress.getByName("172.26.75.113");
                p.setAddress(address);
                p.setLength(buf.length);
                p.setPort(6969);
                s.send(p);
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
