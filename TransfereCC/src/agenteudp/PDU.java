package agenteudp;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.zip.CRC32;

public class PDU {

    public static int BASE_PDU_SIZE = 26;

    private byte type;
    private byte subtype;
    private long checksum;
    private long timeStamp;
    private long seqNumber;
    
    public PDU(long seqNumber, byte type, byte subtype) {
        this.seqNumber = seqNumber;
        this.type = type;
        this.subtype = subtype;
        this.timeStamp = Instant.now().toEpochMilli();
    }

    public byte[] generatePDU() {
        byte type = this.getType();
        byte subtype = this.getSubtype();

        byte[] timeByte = ByteBuffer.allocate(8).putLong(this.getTimeStamp()).array();
        byte[] seqByte = ByteBuffer.allocate(8).putLong(this.getSeqNumber()).array();

        byte[] pdu = new byte[26];
        int pos = 8;
        pdu[pos++] = type;
        pdu[pos++] = subtype;

        for(int i = 0; i < timeByte.length; pos++,i++)
            pdu[pos] = timeByte[i];

        for(int i = 0; i < seqByte.length; i++,pos++) {
            pdu[pos] = seqByte[i];
        }

        byte[] checksum = generateChecksum(pdu);
        for(int i = 0; i < checksum.length; i++) {
            pdu[i] = checksum[i];
        }

        return pdu;
    }

    public byte[] generateChecksum(byte[] data) {
        CRC32 checksum = new CRC32();
        checksum.update(data,8,data.length-8);
        long sum = checksum.getValue();
        this.checksum = sum;
        byte[] checksumByte = ByteBuffer.allocate(8).putLong(sum).array();
        return checksumByte;
    }

    public static PDU degeneratePDU(byte[] data) {
        byte type = data[8];
        byte subtype = data[9];

        long checksum = ByteBuffer.wrap(data,0,8).getLong();
        long timestamp = ByteBuffer.wrap(data,10,8).getLong();
        long seqNumber = ByteBuffer.wrap(data,18,8).getLong();

        PDU result = new PDU(seqNumber,type,subtype);
        result.setChecksum(checksum);
        result.setTimeStamp(timestamp);
        return result;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getSubtype() {
        return subtype;
    }

    public void setSubtype(byte subtype) {
        this.subtype = subtype;
    }

    public long getChecksum() {
        return checksum;
    }

    public void setChecksum(long checksum) {
        this.checksum = checksum;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public long getSeqNumber() {
        return seqNumber;
    }

    public void setSeqNumber(long seqNumber) {
        this.seqNumber = seqNumber;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Type: ").append(type).append("\n");
        sb.append("Subtype: ").append(subtype).append("\n");
        sb.append("Checksum: ").append(checksum).append("\n");
        sb.append("Time-stamp: ").append(timeStamp).append("\n");
        sb.append("Sequence number: ").append(seqNumber).append("\n");
        return sb.toString();
    }
}
