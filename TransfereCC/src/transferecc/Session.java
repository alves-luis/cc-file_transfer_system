package transferecc;

import agenteudp.data.FirstBlockData;
import security.Keys;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Session {

    /* Default size of piece */
    public static int DEFAULT_SIZE_PIECE = 512;

    /* Default number of tries for sendind a packet */
    public static int DEFAULT_NUMBER_OF_TRIES = 5;
    /* Used for computing retransmission timeout */
    private static float ALPHA = (float) 0.125;
    /* Used for computing retransmission timeout */
    private static float BETA = (float) 0.25;

    /* Retransmission timeout in ms */
    private long retransmission_timeout;
    /* Round-trip-time value (smoothed) */
    private float smoothed_round_trip_time;
    /* Variation of rtt */
    private float round_trip_time_variation;

    /* IP of the target client */
    private InetAddress clientIP;
    /* Port of the target client */
    private int clientPort;

    /* Last sent sequence number */
    private long sequenceNumber;
    /* AES Key used for encryption */
    private Keys communicationKeys;

    /* Boolean to test if connection established */
    private boolean connected;
    /* Boolean to test if upload or download */
    private boolean upload;

    /* ID of file to be used in the transfer */
    private String fileID;
    /* Size of file (in bytes) to be used in the transfer */
    private int sizeOfFile;
    /* Hash of file, to verify integrity of the transfer */
    private byte[] hashOfFile;
    /* Pieces of file, mapped by offset -> array of data */
    private Map<Integer,byte[]> piecesOfFile;
    /* Size of piece to be sent (will be updated acordingly if
    * implement flux control */
    private int sizeOfPiece;

    public Session(String ip, int destPort, Keys comKeys) throws UnknownHostException {
        this.clientIP = InetAddress.getByName(ip);
        this.clientPort = destPort;
        this.sequenceNumber = new Random().nextLong();
        this.retransmission_timeout = 1000;
        this.smoothed_round_trip_time = 0;
        this.round_trip_time_variation = 0;
        this.communicationKeys = comKeys.clone();
        this.connected = false;
        this.upload = false;
        this.fileID = null;
        this.sizeOfFile = 0;
        this.hashOfFile = null;
        this.piecesOfFile = new HashMap<>();
        this.sizeOfPiece = Session.DEFAULT_SIZE_PIECE;
    }

    /**
     * Generates a new sequence number
     * @return sequence number
     */
    public long genNewSequenceNumber() {
        return this.sequenceNumber++;
    }

    /**
     * Method that returns the client IP
     * @return client ip of this session
     */
    public InetAddress getClientIP() {
        return this.clientIP;
    }

    /**
     * Given a timestamp, representing the epoch in ms when the datagram received was generated,
     * updates the state variables to set the new retransmission timeout.
     * @param timestamp when was the datagram sent
     */
    public synchronized void receivedDatagram(long timestamp) {
        long now = Instant.now().toEpochMilli();
        long r = now - timestamp;
        // if first measurement
        if (retransmission_timeout == 1000 && smoothed_round_trip_time == 0 && round_trip_time_variation == 0) {
            smoothed_round_trip_time = r;
            round_trip_time_variation = r / 2;
            retransmission_timeout = Math.round(smoothed_round_trip_time + Math.max(4*round_trip_time_variation,(float)1));
        }
        else {
            round_trip_time_variation = (1 - BETA) * round_trip_time_variation + BETA * Math.abs(Math.round(smoothed_round_trip_time) - r);
            smoothed_round_trip_time = (1 - ALPHA) * smoothed_round_trip_time + ALPHA * r;
            retransmission_timeout = Math.round(smoothed_round_trip_time + Math.max(4*round_trip_time_variation,(float)1));
            System.out.println("RTTVAR: " + round_trip_time_variation + ". SRTT: " + smoothed_round_trip_time + ". RTO: " + retransmission_timeout);
        }
        // if rto less than 1 second, round up
        if (retransmission_timeout < 1000)
            retransmission_timeout = 1000;
            // if rto > 60s, truncate to 60s
        else if (retransmission_timeout > 60000) {
            retransmission_timeout = 60000;
        }
    }


    /**
     * Use this method when connection is established
     */
    public void setConnected() {
        this.connected = true;
    }

    /**
     * Returns the ammount of time you should wait to retransmit
     * @return rto
     */
    public synchronized long getRetransmissionTimeout() {
        return this.retransmission_timeout;
    }

    /**
     * Sets the symetric key that should be used to communicate
     * @param aesKey key in AES encryption
     */
    public void setAESKey(byte[] aesKey) {
        this.communicationKeys.setAESKey(aesKey);
    }

    /**
     * Sets the session as an upload session
     */
    public void setUpload() {
        this.upload = true;
    }

    /**
     * Sets the session as a download session
     */
    public void setDownload() {
        this.upload = false;
    }

    /**
     * @return If it's an upload session
     */
    public boolean upload() {
        return this.upload;
    }

    /**
     * @return lenght of the sum of the pieces of file
     */
    private int lenghtOfPieces() {
        return this.piecesOfFile.values().stream()
                .mapToInt(ar -> ar.length).sum();
    }

    /**
     * Aka do I have all the pieces of the file?
     * @return is the file incomplete?
     */
    public boolean fileIncomplete() {
        return this.lenghtOfPieces() != this.sizeOfFile;
    }

    /**
     * Update the state of the session if sent the header of the file
     * @param pdu first block of data
     */
    public void sentHeader(FirstBlockData pdu) {
        this.fileID = pdu.getFileId();
        this.sizeOfFile = pdu.getFileSize();
        this.hashOfFile = pdu.getHash();
        this.piecesOfFile.put(0,pdu.getData());
    }

    /**
     * Returns the size of the file that should be sent
     * @return sizeof array of bytes to send
     */
    public int getSizeOfPiece(int offset) {
        return this.sizeOfFile - offset < this.sizeOfPiece ? this.sizeOfFile - offset : this.sizeOfPiece;
    }

    /**
     * Getter for the size of file
     * @return size
     */
    public int getSizeOfFile() {
        return this.sizeOfFile;
    }




}
