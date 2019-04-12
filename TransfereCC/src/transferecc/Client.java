package transferecc;

import agenteudp.PDU;
import agenteudp.PDUTypes;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.control.ConnectionTermination;
import agenteudp.data.BlockData;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.TreeMap;


public class Client implements Runnable {


    private Sender sender;
    private Receiver receiver;
    private State state ;

    public Client(int destPort) {
        this.sender = new Sender(Sender.DEFAULT_PORT, destPort);
        this.receiver = new Receiver(Receiver.DEFAULT_PORT);
        this.state = new State();
    }


    public boolean startConnection(String destIP) {
        state.setSenderIP(destIP);
        receiver.setExpectedIP(state.getSenderIP());

        ConnectionRequest request = new ConnectionRequest(state.genNewSeqNumber());
        boolean success = sendPacketWithAck(request);

        if (success) {
            state.setConectionEstablished();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean endConnection() {
        ConnectionTermination ending = new ConnectionTermination(state.genNewSeqNumber());
        boolean success = sendPacketWithAck(ending);
        if (success) {
            state.setConectionEnded();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean requestFile(long fileID) {
        int num_tries = 3;
        long timeout = 36000;

        FileID requestFile = new FileID(state.genNewSeqNumber(), PDUTypes.M_FILE, fileID);
        sender.sendDatagram(requestFile, state.getSenderIP());
        while (num_tries > 0) {
            PDU response = receiver.getFIFO(timeout);
            if (response == null) { // timed out
                num_tries--;
                continue;
            }
            state.receivedDatagram();

            if (response instanceof FirstBlockData) {
                receivedFirstBlock(response);
                return true;
            }
        }
        return false;

    }

    public void receivedFirstBlock(PDU response){
        FirstBlockData firstBlock = (FirstBlockData) response;
        state.setFileSize(firstBlock.getFileSize());
        byte[] content = firstBlock.getData();
        byte[] hash = firstBlock.getHash();
        state.setHashOfFile(hash);
        state.sentPieceOfFile(content, 0);
        sendAck(firstBlock);
        if (content.length==state.getFileSize()){
            createFile();

        }
        else {
            receiveBlocksLeft();
        }
    }


    public void receiveBlocksLeft(){
        int received=state.getTreeMap().get(0).length;
        int num_tries = 3;
        long timeout = 36000;

        while(received!=this.state.getFileSize()){

            PDU response = receiver.getFIFO(timeout);
            if (response == null) { // timed out
                num_tries--;
                continue;
            }

            state.receivedDatagram();

            if (response instanceof BlockData) {
               received+= receivedBlock(response);
            }
        }
        createFile();
    }



    public int receivedBlock(PDU response){

        BlockData block = (BlockData) response;
        int offset = block.getOffset();
        byte[] content = block.getData();
        state.sentPieceOfFile(content, offset);
        sendAck(block);
        return content.length;
    }

    public void sendAck(PDU block){
        Ack ack = new Ack(state.genNewSeqNumber(), block.getSeqNumber());
        sender.sendDatagram(ack, state.getSenderIP());
    }

    public void createFile(){

        byte[] result= concatenate();
        System.out.println("SIZE " + result.length);
       // String name= Long.toString(this.state.getFileID());
        File file= new File ("recebiIsto.txt");

        try{
            FileOutputStream out= new FileOutputStream(file);
            out.write(result);
            out.flush();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    private byte[] concatenate() {

        int size= (int) this.state.getFileSize();
        byte[] result = new byte[size];

        TreeMap<Integer,byte[]> file = this.state.getTreeMap();
        for(Map.Entry<Integer,byte[]> entry : file.entrySet()) {
            Integer key = entry.getKey();
            byte[] value = entry.getValue();
            System.arraycopy(value,0,result,key,value.length);
        }
        return result;
    }


    @Override
    public void run() {
        new Thread(receiver).start();
    }

    /**
     * This method, given a packet, keeps sending it until
     * it receives an ack, or gives up if timed out
     * @param packet packet to send and wait for ack
     * @return true if packet sent and received an ack
     */
    private boolean sendPacketWithAck(PDU packet) {
        int num_tries = 3;
        boolean timedOut = false;
        while(num_tries > 0 && !timedOut) {
            sender.sendDatagram(packet,state.getSenderIP());
            PDU response = receiver.getFIFO(Server.DEFAULT_TIMEOUT);
            if (response == null) {
                if (num_tries != 1)
                    num_tries--;
                else
                    timedOut = true;
            }
            else {
                state.receivedDatagram();
                if (response instanceof Ack) {
                    Ack ack = (Ack) response;
                    if (ack.getAck() == packet.getSeqNumber()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
