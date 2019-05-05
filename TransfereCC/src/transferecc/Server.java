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

import java.io.File;
import java.io.IOException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Server implements Runnable {

    private static int DEFAULT_SENDING_PORT = 4444;
    private static int DEFAULT_RECEIVING_PORT = 7777;
    private static int DEFAULT_CLIENT_PORT = 5555;

    private Sender sender;
    private Receiver receiver;
    /* For now it's a single Session, if multiplexing more sessions */
    private Session session;
    private State state;

    public Server() {
        this.state = new State();
        this.receiver = new Receiver(Server.DEFAULT_RECEIVING_PORT, null,state.getKeys());
        this.sender = new Sender(Server.DEFAULT_SENDING_PORT,DEFAULT_CLIENT_PORT, state.getKeys());
    }

    /**
     * Method that is called when waiting for a connection request
     * @return success of the operation
     */
    public boolean receiveConnectionRequest() {

        int numberOfTries = Session.DEFAULT_NUMBER_OF_TRIES;
        // while not enough invalid number of tries
        while(numberOfTries > 0) {
            ConnectionRequest r = receiver.getConnectionRequest();
            // if connection == null, failed to retrieve one
            if (r == null) {
                numberOfTries--;
            }
            else {
                // try to initiate a session
                try {
                    this.session = new Session(receiver.getSenderIP().getHostAddress(), Server.DEFAULT_RECEIVING_PORT, state.getKeys());
                }
                catch (UnknownHostException e) {
                    System.err.println(e.toString());
                    return false;
                }
                session.receivedDatagram(r.getTimeStamp());
                receiver.setExpectedIP(session.getClientIP());
                boolean success = sendPublicKey();
                if (success) {
                    return receiveConnectionTermination();
                }
            }
        }
        return false;
    }

    /**
     * Tries to send the public key of the server
     * @return true if all went well
     */
    private boolean sendPublicKey() {
        int numberOfTries = Session.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;

        KeyExchange packetWithRSA = new KeyExchange(session.genNewSequenceNumber(), state.getRSAPublicKey());
        while(numberOfTries > 0 && !timedOut) {
            sender.sendDatagram(packetWithRSA,session.getClientIP());
            KeyExchange pdu = receiver.getKeyExchange(session.getRetransmissionTimeout());
            if (pdu == null) {
                if (numberOfTries > 1)
                    numberOfTries--;
                else
                    timedOut = true;
            }
            else {
                session.receivedDatagram(pdu.getTimeStamp());
                byte[] aesKey = state.decryptWithMyRSA(pdu.getKey());
                session.setAESKey(aesKey);
                // for now, state has the same key
                state.setAESKey(aesKey);
                this.receiver.activateAESKeyEncryption();
                this.sender.activateAESKeyEncryption();
                // this should change in case of multiplexing
                boolean success = receiveFileID(new Ack(session.genNewSequenceNumber(), pdu.getSeqNumber()));
                if (success)
                    return true;
                else {
                    this.receiver.deactivateAESKeyEncryption();
                    this.sender.deactivateAESKeyEncryption();
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Method that keeps sending the Ack from the symmetric key
     * until it receives a file ID
     * @param ackOfKeyEx ack packet to send
     * @return
     */
    private boolean receiveFileID(Ack ackOfKeyEx) {
        int numberOfTries = Session.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;

        while(numberOfTries > 0 && !timedOut) {
            sender.sendDatagram(ackOfKeyEx,session.getClientIP());
            FileID fileID = receiver.getFileID(session.getRetransmissionTimeout());
            // if did not get a valid fileID
            if (fileID == null) {
                if (numberOfTries > 1)
                    numberOfTries--;
                else
                    timedOut = true;
            }
            // got one, so process it
            else {
                session.receivedDatagram(fileID.getTimeStamp());
                byte direction = fileID.getDirection();
                boolean success = false;

                if (direction == PDUManagement.UPLOAD)
                    success = waitForData(fileID.getFileID());
                else if (direction == PDUManagement.DOWNLOAD)
                    success = sendFile(fileID.getFileID());
                return success;
            }

        }
        return false;
    }

    /**
     * This method, given an instance of a File, sends its header
     * @param fileID file to send the header
     * @return success of sending the file
     */
    private boolean sendFile(String fileID) {
        session.setDownload();
        try {
            File file = new File(fileID);
            long seqNumber = session.genNewSequenceNumber();
            // array of bytes of file
            byte[] fileBytes = Files.readAllBytes(file.toPath());
            // get hash of file for verification of integrity
            byte[] hashOfFile = FirstBlockData.getHash(fileBytes);
            // size of file
            int fileSize = Math.toIntExact(file.length());

            byte[] data = FirstBlockData.getFirstChunkOfData(fileBytes);
            FirstBlockData header = new FirstBlockData(seqNumber,fileID,fileSize,hashOfFile,data);

            // sends the header sequentially
            boolean success = sendPacketWithAck(header);

            // if got the ack, start sending the pieces concurrently
            if (success) {
                session.sentHeader(header);

                List<ReliablePiece> sendingThreads = new ArrayList<>();
                int offset = data.length; // current offset
                while(offset != session.getSizeOfFile()) {
                    // send 4 pieces at a time
                    for (int i = 0; i < 4; i++) {

                        // copy new piece into data
                        data = new byte[session.getSizeOfPiece(offset)];
                        System.arraycopy(fileBytes, offset, data, 0, session.getSizeOfPiece(offset));


                        BlockData block = new BlockData(session.genNewSequenceNumber(), fileID, offset, data);
                        ReliablePiece t = new ReliablePiece(session.getClientIP(), session.getRetransmissionTimeout(), this.receiver, this.sender, block);
                        t.start();
                        sendingThreads.add(t);
                        offset += data.length; // update offset
                        // if is last piece, don't create more threads
                        if (offset == session.getSizeOfFile())
                            break;
                    }
                    // send all the pieces, so now wait for the confirmations
                    for (ReliablePiece t : sendingThreads) {
                        try {
                            t.join(session.getRetransmissionTimeout()); // wait for the pieces
                            if (!t.sentReliably())
                                return false;
                            // sent reliably
                            else {
                                int attempts = t.getNumberOfAttempts();
                                // if took more than one attempt, dial back size of piece
                                if (attempts > 0) {
                                    session.reduceSizeOfPiece();
                                }
                                // else increase it (like a man)
                                else {
                                    session.increaseSizeOfPiece();
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            return false;
                        }
                    }
                    sendingThreads.clear();
                }
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * This method, given a packet, keeps sending it until
     * it receives an ack. It blocks until it receives it or times out
     * @param packet packet to send and wait for ack
     * @return true if packet sent and received an ack
     */
    private boolean sendPacketWithAck(PDU packet) {
        int numberOfTries = Session.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;

        while(numberOfTries > 0 && !timedOut) {
            sender.sendDatagram(packet,session.getClientIP());
            Ack response = receiver.getAck(packet.getSeqNumber(),session.getRetransmissionTimeout());
            // if failed to get ack, retransmit
            if (response == null) {
                if (numberOfTries > 1)
                    numberOfTries--;
                else
                    timedOut = true;
            }
            else {
                session.receivedDatagram(response.getTimeStamp());
                return true;
            }
        }
        return false;

    }


    /**
     * Method that is using when client wants to upload a file
     * @param file filename to upload
     * @return success of the operation
     */
    private boolean waitForData(String file) {
        session.setUpload();
        // TO DO
        return true;
    }

    @Override
    public void run() {
        new Thread(receiver).start();
    }

    /**
     * Method that waits on a connection termination packet
     * @return success of the operation
     */
    private boolean receiveConnectionTermination() {
        int numberOfTries = Session.DEFAULT_NUMBER_OF_TRIES;
        boolean timedOut = false;

        while(numberOfTries > 0 && !timedOut) {
            ConnectionTermination t = receiver.getConnectionTermination(session.getRetransmissionTimeout());
            if (t == null) {
                if (numberOfTries > 1)
                    numberOfTries--;
                else
                    timedOut = true;
            }
            else {
                // only sending one ack, might be bad,
                sender.sendDatagram(new Ack(session.genNewSequenceNumber(), t.getSeqNumber()), session.getClientIP());
                session = null; // session is over
                sender.deactivateAESKeyEncryption();
                receiver.deactivateAESKeyEncryption();
                return true;
            }
        }
        return false;
    }
}
