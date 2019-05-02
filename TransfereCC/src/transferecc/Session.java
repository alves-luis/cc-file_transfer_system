package transferecc;

import security.Keys;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Random;

public class Session {

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
    private volatile boolean connected;

    public Session(String ip, int destPort, Keys comKeys) throws UnknownHostException {
        this.clientIP = InetAddress.getByName(ip);
        this.clientPort = destPort;
        this.sequenceNumber = new Random().nextLong();
        this.retransmission_timeout = 1000;
        this.smoothed_round_trip_time = 0;
        this.round_trip_time_variation = 0;
        this.communicationKeys = comKeys.clone();
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
}
