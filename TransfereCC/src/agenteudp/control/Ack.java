package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.nio.ByteBuffer;

public class Ack extends PDUControl {

    private long ack; // sequence number of packet to acknowledge

    public Ack(long seqNumber, long ack) {
        super(seqNumber,PDUTypes.C_ACK);
        this.ack = ack;
    }

    @Override
    public byte[] generatePDU() {
        byte[] basePDU = super.generatePDU();
        byte[] ackAsBytes = ByteBuffer.allocate(8).putLong(this.ack).array();

        byte[] finalPDU = new byte[basePDU.length + ackAsBytes.length];

        System.arraycopy(basePDU,0,finalPDU,0,basePDU.length);
        System.arraycopy(ackAsBytes,0,finalPDU,basePDU.length,ackAsBytes.length);

        byte[] checksum = super.generateChecksum(finalPDU);
        System.arraycopy(checksum,0,finalPDU,0,checksum.length);

        return finalPDU;
    }

    public long getAck() {
        return ack;
    }

    public static Ack degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);

        long ackNum = ByteBuffer.wrap(data,PDU.BASE_PDU_SIZE,8).getLong();

        Ack ackPacket = new Ack(pdu.getSeqNumber(),ackNum);
        ackPacket.setTimeStamp(pdu.getTimeStamp());
        ackPacket.setChecksum(pdu.getChecksum());
        ackPacket.setSeqNumber(pdu.getSeqNumber());
        return ackPacket;
    }

    @Override
    public String toString() {
        String s = super.toString();
        return s + "Ack: " + ack + "\n";
    }
}
