package transferecc;

import agenteudp.PDU;
import agenteudp.PDUTypes;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.control.ConnectionTermination;
import agenteudp.control.KeyExchange;
import agenteudp.data.BlockData;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;
import security.Keys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.Key;
import java.util.Map;
import java.util.TreeMap;


public class Client implements Runnable {


    private Sender sender;
    private Receiver receiver;
    private ClientState state;
    private Keys cryptoKeys;

    public Client(String serverIP, int destPort) {
        try {
            this.state = new ClientState(InetAddress.getByName(serverIP),destPort);
            this.cryptoKeys = new Keys();
            this.receiver = new Receiver(Receiver.DEFAULT_PORT, null, cryptoKeys);
            this.sender = new Sender(Sender.DEFAULT_PORT, destPort, cryptoKeys);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }


    public boolean startConnection(String destIP) {
        state.setServerIP(destIP);
        receiver.setExpectedIP(state.getServerIP());

        ConnectionRequest request = new ConnectionRequest(state.genNewSequenceNumber());
        boolean success = sendPacketWithResponse(request);

        if (success) {
            state.setConnected();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean endConnection() {
        ConnectionTermination ending = new ConnectionTermination(state.genNewSequenceNumber());
        boolean success = sendPacketWithResponse(ending);
        if (success) {
            state.setDisconnected();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean requestFile(long fileID) {
        int num_tries = 3;
        long timeout = state.getRetransmissionTimeout();

        FileID requestFile = new FileID(state.genNewSequenceNumber(), PDUTypes.M_FILE, fileID);
        state.setTransferingFile();
        sender.sendDatagram(requestFile, state.getServerIP());
        while (num_tries > 0) {
            PDU response = receiver.getFIFO(timeout);
            if (response == null) { // timed out
                num_tries--;
                continue;
            }
            state.receivedDatagram(response.getTimeStamp());
            if (response instanceof FirstBlockData) {
                receivedFirstBlock(response);
                return true;
            }
        }
        return false;

    }

    public void receivedFirstBlock(PDU response){
        FirstBlockData firstBlock = (FirstBlockData) response;
        state.receivedFirstBlockOfFile(firstBlock);
        sendAck(firstBlock);
        if (state.missingFilePieces()){
            createFile();
        }
        else {
            receiveBlocksLeft();
        }
    }


    public void receiveBlocksLeft(){
        long received=state.getLengthOfFileReceived();
        int num_tries = 3;
        long timeout = state.getRetransmissionTimeout();

        while(received!=this.state.getFileSize()){

            PDU response = receiver.getFIFO(timeout);
            if (response == null) { // timed out
                num_tries--;
                continue;
            }

            state.receivedDatagram(response.getTimeStamp());

            if (response instanceof BlockData) {
               received+= receivedBlock(response);
            }
        }
        createFile();
    }



    public int receivedBlock(PDU response){

        BlockData block = (BlockData) response;
        int sizeOfBlock = state.receivedBlockOfFile(block);
        sendAck(block);
        return sizeOfBlock;
    }

    public void sendAck(PDU block){
        Ack ack = new Ack(state.genNewSequenceNumber(), block.getSeqNumber());
        sender.sendDatagram(ack, state.getServerIP());
    }

    public void createFile(){

        byte[] result= state.concatenateFile();
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


    @Override
    public void run() {
        new Thread(receiver).start();
    }

    /**
     * This method, given a packet, keeps sending it until
     * it receives an ack or keyexchange, or gives up if timed out
     * @param packet packet to send and wait for ack
     * @return true if packet sent and received an ack
     */
    private boolean sendPacketWithResponse(PDU packet) {
        int num_tries = 3;
        boolean timedOut = false;
        while(num_tries > 0 && !timedOut) {
            sender.sendDatagram(packet,state.getServerIP());
            PDU response = receiver.getFIFO(state.getRetransmissionTimeout());
            if (response == null) {
                if (num_tries != 1)
                    num_tries--;
                else
                    timedOut = true;
            }
            else {
                state.receivedDatagram(response.getTimeStamp());
                if (response instanceof Ack) {
                    Ack ack = (Ack) response;
                    if (ack.getAck() == packet.getSeqNumber()) {
                        return true;
                    }
                }
                else if (response instanceof KeyExchange) {
                    if (!state.isSentAESKey())
                        sendAESKey((KeyExchange) response);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean sendAESKey(KeyExchange keyEx) {
        byte[] encryptedAES = cryptoKeys.encryptRSA(keyEx.getKey());
        KeyExchange packetWithAES = new KeyExchange(state.genNewSequenceNumber(),encryptedAES);
        receiver.activateAESKeyEncryption();
        boolean result = sendPacketWithResponse(packetWithAES);
        sender.activateAESKeyEncryption();
        state.sentAESKey();
        return result;
    }
}
