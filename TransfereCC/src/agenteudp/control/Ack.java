package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

public class Ack extends PDUControl {

    public Ack(long seqNumber) {
        super(seqNumber,PDUTypes.C_ACK);
    }

    public static Ack degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);
        Ack ack = new Ack(pdu.getSeqNumber());
        ack.setTimeStamp(pdu.getTimeStamp());
        ack.setChecksum(pdu.getChecksum());
        ack.setSeqNumber(pdu.getSeqNumber());
        return ack;
    }
}
