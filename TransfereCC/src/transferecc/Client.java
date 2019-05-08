package transferecc;

import agenteudp.PDU;
import agenteudp.Receiver;
import agenteudp.Sender;
import agenteudp.control.Ack;
import agenteudp.control.ConnectionRequest;
import agenteudp.control.ConnectionTermination;
import agenteudp.control.KeyExchange;
import agenteudp.data.BlockData;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;
import agenteudp.management.PDUManagement;
import security.Keys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;


public class Client {


    private Sender sender;
    private Receiver receiver;
    private ClientState state;
    private Keys cryptoKeys;
    private Thread receivingThread;

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
            this.receiver.setTimeout(10000); // set default receiving timeout
            this.sender = new Sender(Sender.DEFAULT_PORT, destPort, cryptoKeys);
            this.receivingThread = new Thread(receiver);
            this.receivingThread.start();
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
        receiver.stopRunning();
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
        receiver.stopRunning();
        return false;
    }

    /**
     * Method that is called when requesting a file from the server
     * @param fileID
     * @return success of the operation
     */
    public boolean requestFile(String fileID) {
        int numberOfTries = ClientState.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;

        FileID requestFile = new FileID(state.genNewSequenceNumber(), PDUManagement.DOWNLOAD, fileID);
        state.setTransferingFile();
        while(numberOfTries > 0 && !timedOut) {
            sender.sendDatagram(requestFile,state.getServerIP());
            FirstBlockData pdu = receiver.getFirstBlockData(state.getRetransmissionTimeout());
            // if did not get a valid header
            if (pdu == null) {
                if (numberOfTries > 1)
                    numberOfTries--;
                else
                    timedOut = true;
            }
            // got the header, so wait for the rest of the file
            else {
                state.receivedFirstBlockOfFile(pdu);
                state.receivedDatagram(pdu.getTimeStamp());
                // it's only sending a single Ack. Might be bad.
                sender.sendDatagram(new Ack(state.genNewSequenceNumber(),pdu.getSeqNumber()),state.getServerIP());
                return waitForFile();
            }
        }
        return false;
    }

    public boolean sendFile(String filename) {
        // TO DO
        return false;
    }

    /**
     * Method that is called after receiving the header of a file. Waits for the rest of the pieces and sends acks
     * @return success of the waiting operation
     */
    private boolean waitForFile() {
        boolean timedOut = false;
        int numberOfTries = ClientState.DEFAULT_NUMBER_OF_TRIES;
        // if missing pieces
        while (state.missingFilePieces() && !timedOut) {
            BlockData data = receiver.getBlockData(state.getRetransmissionTimeout());
            // if failed to retrieve a block
            if (data == null) {
                if (numberOfTries > 1)
                    numberOfTries--;
                else
                    timedOut = true;
            }
            // got a block, so update the file pieces
            else {
                state.receivedDatagram(data.getTimeStamp());
                state.receivedBlockOfFile(data);
                // got a successful block, so update the number of tries to get a block
                numberOfTries = ClientState.DEFAULT_NUMBER_OF_TRIES;
                // send an ack
                Ack ack = new Ack(state.genNewSequenceNumber(),data.getSeqNumber());
                sender.sendDatagram(ack,state.getServerIP());
            }
        }
        if (!timedOut) {
            createFile();
            return true;
        }
        else
            return false;
    }

    /**
     * Method that is used to instantiate the file
     */
    private void createFile() {
        try{
            byte[] result = state.concatenateFile();
            File file = new File ("C_"+state.getFileID());
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

    /**
     * Method that is used to end a connection to the server
     * @return success of the operation
     */
    public boolean endConnection() {
        ConnectionTermination ending = new ConnectionTermination(state.genNewSequenceNumber());
        boolean success = sendPacketWithAck(ending);
        if (success) {
            state.setDisconnected();
            receiver.stopRunning();
            return true;
        }
        else {
            return false;
        }
    }
}
