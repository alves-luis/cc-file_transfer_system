package agenteudp.control;

import agenteudp.PDU;

public class Ack extends PDU {

    @Override
    public byte[] generatePDU() {
        return new byte[0];
    }
}
