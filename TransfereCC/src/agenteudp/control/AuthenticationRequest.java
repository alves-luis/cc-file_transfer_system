package agenteudp.control;

import agenteudp.PDU;

public class AuthenticationRequest extends PDU {

    public AuthenticationRequest(long seqNumber, byte type, byte subtype) {
        super(seqNumber, type, subtype);
    }
    
    
    @Override
    public byte[] generatePDU() {
        return new byte[0];
    }
}
