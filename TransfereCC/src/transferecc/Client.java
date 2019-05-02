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


public class Client implements Runnable {


    private Sender sender;
    private Receiver receiver;
    private ClientState state;
    private Keys cryptoKeys;

    /**
     * Default constructor for Client
     * @param serverIP IP of the server that will receive the requests
     * @param destPort destination port of the server that will receive the requests
     */
    public Client(String serverIP, int destPort) {
        try {
            this.state = new ClientState(InetAddress.getByName(serverIP),destPort);
            this.cryptoKeys = new Keys();
            this.receiver = new Receiver(Receiver.DEFAULT_PORT, InetAddress.getByName(serverIP), cryptoKeys);
            this.sender = new Sender(Sender.DEFAULT_PORT, destPort, cryptoKeys);
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    /**
     * Method that starts a connection with the server
     * @return success of establishing a connection
     */
    public boolean startConnection() {
        ConnectionRequest request = new ConnectionRequest(state.genNewSequenceNumber());

        int numberOfTries = ClientState.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;
        // while didn't time out and in valid number of tries, send connection request
        while(numberOfTries > 0 && !timedOut) {
            sender.sendDatagram(request,state.getServerIP());
            KeyExchange response = receiver.getKeyExchange(state.getRetransmissionTimeout());
            // if response == null, failed to retrieve
            if (response == null) {
                if (numberOfTries != 1) {
                    numberOfTries--;
                    state.timedOut();
                }
                else
                    timedOut = true;
            }
            // else, got what I wanted
            else {
                state.receivedDatagram(response.getTimeStamp());
                // so send my AES key
                boolean success = sendAESKey(response);
                if (success) {// if all went well with sending AES key, it's connected
                    state.setConnected();
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Method that is used after receiving the public key from the server.
     * Now sends the symetric key (AES), and waits for an ack
     * @param keyEx packet containing the public key
     * @return success
     */
    private boolean sendAESKey(KeyExchange keyEx) {
        byte[] encryptedAES = cryptoKeys.encryptRSA(keyEx.getKey());
        KeyExchange packetWithAES = new KeyExchange(state.genNewSequenceNumber(),encryptedAES);
        receiver.activateAESKeyEncryption();
        boolean result = sendPacketWithAck(packetWithAES);
        // if got the ack, then activate encryption
        if (result) {
            sender.activateAESKeyEncryption();
            state.sentAESKey();
        }
        // else deactivate receiver encryption
        else {
            sender.deactivateAESKeyEncryption();
            receiver.deactivateAESKeyEncryption();
        }
        return result;
    }

    /**
     * This method, given a packet, keeps sending it until
     * it receives an ack or gives up if timed out
     * @param packet packet to send and wait for ack
     * @return true if packet sent and received an ack
     */
    private boolean sendPacketWithAck(PDU packet) {
        int numberOfTries = ClientState.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;
        // while didn't time out and in valid number of tries, keeps sending packet
        while(numberOfTries > 0 && !timedOut) {
            sender.sendDatagram(packet,state.getServerIP());
            Ack response = receiver.getAck(packet.getSeqNumber(),state.getRetransmissionTimeout());
            // if response == null, failed to receive ack
            if (response == null) {
                if (numberOfTries != 1) {
                    numberOfTries--;
                    state.timedOut();
                }
                else
                    timedOut = true;
            }
            else {
                state.receivedDatagram(response.getTimeStamp());
                return true;
            }
        }
        return false;
    }

    public boolean endConnection() {
        ConnectionTermination ending = new ConnectionTermination(state.genNewSequenceNumber());
        boolean success = sendPacketWithAck(ending);
        if (success) {
            state.setDisconnected();
            return true;
        }
        else {
            return false;
        }
    }

    public boolean requestFile(String fileID) {
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

    private void createFile() {
        try{
            byte[] result = state.concatenateFile();
            File file= new File ("C_"+state.getFileID());
            FileOutputStream out= new FileOutputStream(file);
            out.write(result);
            out.flush();
        }
        catch (FileNotCompleteException e) {
            System.err.println("Tried to create an incomplete file!" + e.toString());
        }
        catch (SHA1FileException e) {
            System.err.println("Concatenated the file, but hash does not match! Asking for the file again!" + e.toString());
            requestFile(state.getFileID());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        new Thread(receiver).start();
    }
}
