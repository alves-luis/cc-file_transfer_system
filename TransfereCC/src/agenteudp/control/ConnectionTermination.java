package agenteudp.control;

import agenteudp.PDU;

public class ConnectionTermination extends PDU {

    public ConnectionTermination(long seqNumber, byte type, byte subtype) {
        super(seqNumber, type, subtype);
    }

}
