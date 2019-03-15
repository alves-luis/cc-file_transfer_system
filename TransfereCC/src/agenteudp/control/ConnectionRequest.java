package agenteudp.control;

import agenteudp.PDU;

public class ConnectionRequest extends PDU {
    @Override
    public byte[] generatePDU() {
        return new byte[0];
    }
}
