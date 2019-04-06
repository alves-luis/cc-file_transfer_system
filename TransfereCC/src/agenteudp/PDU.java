package agenteudp;

import java.time.Instant;

public abstract class PDU {

    private byte type;
    private byte subtype;
    private String checksum;
    private long timeStamp;
    private long seqNumber;
    
    public PDU(long seqNumber, byte type, byte subtype) {
        this.seqNumber = seqNumber;
        this.type = type;
        this.subtype = subtype;
        this.timeStamp = Instant.now().toEpochMilli();
    }

    public abstract byte[] generatePDU();

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

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
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
    
    
}
