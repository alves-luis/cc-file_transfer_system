package agenteudp.control;

import agenteudp.PDU;

public class ConnectionRequest extends PDU {

    public ConnectionRequest(long seqNumber, byte type, byte subtype) {
        super(seqNumber, type, subtype);
    }
    
    @Override
    public byte[] generatePDU() {
        return new byte[0];
    }
}
