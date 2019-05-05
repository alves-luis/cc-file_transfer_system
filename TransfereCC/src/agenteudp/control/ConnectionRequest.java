package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class ConnectionRequest extends PDUControl {


    public ConnectionRequest(long seqNumber) {
        super(seqNumber, PDUTypes.C_CONNECTION_REQUEST);
    }

    public ConnectionRequest(PDU p) {
        super(p, PDUTypes.C_CONNECTION_REQUEST);
    }

    public static ConnectionRequest degeneratePDU(byte[] data) {
        return new ConnectionRequest(PDU.degeneratePDU(data));
    }
}
