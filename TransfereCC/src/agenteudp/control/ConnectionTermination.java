package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.nio.ByteBuffer;

public class ConnectionTermination extends PDUControl {

    public ConnectionTermination(long seqNumber) {
        super(seqNumber, PDUTypes.C_CONNECTION_TERMINATION);
    }

    public static ConnectionTermination degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);

        ConnectionTermination packet = new ConnectionTermination(pdu.getSeqNumber());
        packet.setTimeStamp(pdu.getTimeStamp());
        packet.setChecksum(pdu.getChecksum());
        packet.setSeqNumber(pdu.getSeqNumber());
        return packet;
    }

}
