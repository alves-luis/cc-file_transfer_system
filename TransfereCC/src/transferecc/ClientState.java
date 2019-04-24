package transferecc;

import agenteudp.data.BlockData;
import agenteudp.data.FirstBlockData;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentSkipListMap;

public class ClientState {

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

    /* IP of the target server */
    private InetAddress serverIP;
    /* Port of the target server */
    private int serverPort;

    /* Last sent sequence number*/
    private long sequence_number;

    /* Boolean to check if valid connection */
    private volatile boolean connected;
    /* Boolean to check if currently transfering file */
    private volatile boolean transferingFile;
    /* Boolean to check if should encrypt data */
    private boolean sentAESKey;

    /* Maps the received pieces of files to their offset */
    private ConcurrentSkipListMap<Integer,byte[]> piecesOfFile;
    /* File ID */
    private String fileID;
    /* Hash of the receiving file */
    private byte[] hashOfFile;
    /* Size of the receiving file*/
    private long sizeOfFile;

    public ClientState(InetAddress serverIP, int serverPort) {
        this.retransmission_timeout = 1000;
        this.smoothed_round_trip_time = 0;
        this.round_trip_time_variation = 0;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.sequence_number = new Random().nextLong();
        this.connected = false;
        this.transferingFile = false;
        this.sentAESKey = false;
        this.piecesOfFile = new ConcurrentSkipListMap<>();
        this.fileID = null;
        this.hashOfFile = null;
    }

    /**
     * Generates a new sequence number
     * @return sequence number
     */
    public long genNewSequenceNumber() {
        return this.sequence_number++;
    }

    /**
     * Returns the IP of the server containing the file
     * @return address of the server
     */
    public InetAddress getServerIP() {
        return this.serverIP;
    }

    public void setServerIP(String ip) {
        try {
            this.serverIP = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            System.err.println(e.toString());
        }
    }

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

    public synchronized long getRetransmissionTimeout() {
        return this.retransmission_timeout;
    }

    public void setConnected() {
        this.connected = true;
    }

    public void setDisconnected() {
        this.connected = false;
    }

    public void setTransferingFile() {
        this.transferingFile = true;
    }

    public void receivedFirstBlockOfFile(FirstBlockData block) {
        this.sizeOfFile = block.getFileSize();
        this.hashOfFile = block.getHash();
        this.piecesOfFile.put(0,block.getData());
    }

    public int receivedBlockOfFile(BlockData block) {
        byte[] data = block.getData();
        this.piecesOfFile.put(block.getOffset(),data);
        return data.length;
    }

    public byte[] concatenateFile() {
        byte[] finalFile = new byte[(int) this.sizeOfFile];
        for(Map.Entry<Integer,byte[]> entry : this.piecesOfFile.entrySet()) {
            Integer key = entry.getKey();
            byte[] value = entry.getValue();
            System.arraycopy(value,0,finalFile,key,value.length);
        }
        return finalFile;
    }

    public long getLengthOfFileReceived() {
        long sum = 0;
        for(byte[] pieces : this.piecesOfFile.values()) {
            sum += pieces.length;
        }
        return sum;
    }

    public long getFileSize() {
        return this.sizeOfFile;
    }

    public boolean missingFilePieces() {
        return this.getLengthOfFileReceived() == this.sizeOfFile;
    }

    public boolean isSentAESKey() {
        return this.sentAESKey;
    }

    public void sentAESKey() {
        this.sentAESKey = true;
    }

    public static void main(String[] args) throws UnknownHostException {
        ClientState cs = new ClientState(InetAddress.getByName("localhost"),123);
        long fakeNow = Instant.now().toEpochMilli() - 500;
        cs.receivedDatagram(fakeNow);
        long rto = cs.getRetransmissionTimeout();
        System.out.println("RTO: " + rto);
        cs.receivedDatagram(Instant.now().toEpochMilli());
        rto = cs.getRetransmissionTimeout();
        System.out.println("RTO: " + rto);
        cs.receivedDatagram(Instant.now().toEpochMilli());
        rto = cs.getRetransmissionTimeout();
        System.out.println("RTO: " + rto);
    }



}
