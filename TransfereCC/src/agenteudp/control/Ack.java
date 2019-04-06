package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

public class Ack extends PDU {

    public Ack(long seqNumber) {
        super(seqNumber,PDUTypes.CONTROL,PDUTypes.C_ACK);
    }
    
    @Override
    public byte[] generatePDU() {
        long timestamp = this.getTimeStamp();
        long seqNumber = this.getSeqNumber();
        byte type = this.getType();
        byte subtype = this.getSubtype();
        
    }
}
