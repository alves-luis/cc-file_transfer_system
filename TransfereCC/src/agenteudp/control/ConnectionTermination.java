package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

public class ConnectionTermination extends PDUControl {

    public ConnectionTermination(long seqNumber) {
        super(seqNumber, PDUTypes.C_CONNECTION_TERMINATION);
    }

    public ConnectionTermination(PDU p) {
        super(p, PDUTypes.C_CONNECTION_TERMINATION);
    }

    public static ConnectionTermination degeneratePDU(byte[] data) {
        return new ConnectionTermination(PDU.degeneratePDU(data));
    }

}
