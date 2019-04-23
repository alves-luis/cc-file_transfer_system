package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class Ack extends PDUControl {

    private long ack; // sequence number of packet to acknowledge
    private String message; // message encoded in the ack

    public Ack(long seqNumber, long ack) {
        super(seqNumber,PDUTypes.C_ACK);
        this.ack = ack;
        this.message = "";
    }

    public Ack(long seqNumber, long ack, String msg) {
        super(seqNumber,PDUTypes.C_ACK);
        this.ack = ack;
        this.message = msg;
    }

    @Override
    public byte[] generatePDU() {
        byte[] basePDU = super.generatePDU();
        byte[] ackAsBytes = ByteBuffer.allocate(8).putLong(this.ack).array();
        byte[] messageAsBytes = this.message.getBytes(Charset.forName("UTF-8"));

        byte[] finalPDU = new byte[basePDU.length + ackAsBytes.length + messageAsBytes.length];

        System.arraycopy(basePDU,0,finalPDU,0,basePDU.length);
        System.arraycopy(ackAsBytes,0,finalPDU,basePDU.length,ackAsBytes.length);
        if (messageAsBytes.length > 0)
            System.arraycopy(messageAsBytes,0,finalPDU,basePDU.length + ackAsBytes.length,messageAsBytes.length);

        byte[] checksum = super.generateChecksum(finalPDU);
        System.arraycopy(checksum,0,finalPDU,0,checksum.length);

        return finalPDU;
    }

    public long getAck() {
        return ack;
    }

    public String getMessage() {
        return this.message;
    }

    public static Ack degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);

        long ackNum = ByteBuffer.wrap(data,PDU.BASE_PDU_SIZE,8).getLong();
        int sizeOfMsg = data.length - BASE_PDU_SIZE - 8;
        byte[] encodedMsg = new byte[sizeOfMsg];
        System.arraycopy(data,BASE_PDU_SIZE + 8,encodedMsg,0,sizeOfMsg);
        String msg = new String(encodedMsg, Charset.forName("UTF-8"));

        Ack ackPacket = new Ack(pdu.getSeqNumber(),ackNum,msg);
        ackPacket.setTimeStamp(pdu.getTimeStamp());
        ackPacket.setChecksum(pdu.getChecksum());
        ackPacket.setSeqNumber(pdu.getSeqNumber());
        return ackPacket;
    }

    @Override
    public String toString() {
        String s = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(s).append("Ack: ").append(this.ack).append("\n")
                .append("Message: ").append(this.message).append("\n");
        return sb.toString();
    }
}
