package agenteudp;

public abstract class PDU {

    private String checksum;
    private long timeStamp;
    private long seqNumber;

    public abstract byte[] generatePDU();
}
