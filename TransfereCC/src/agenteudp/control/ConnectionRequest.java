package agenteudp.control;

import agenteudp.PDUTypes;

import java.nio.ByteBuffer;

public class ConnectionRequest extends PDUControl {

    public ConnectionRequest(long seqNumber) {
        super(seqNumber, PDUTypes.C_CONNECTION_REQUEST);
    }

    public static ConnectionRequest degeneratePDU(byte[] data) {
        byte type = data[8];
        byte subtype = data[9];

        long checksum = ByteBuffer.wrap(data,0,8).getLong();
        long timestamp = ByteBuffer.wrap(data,10,8).getLong();
        long seqNumber = ByteBuffer.wrap(data,18,8).getLong();

        ConnectionRequest result = new ConnectionRequest(seqNumber);
        result.setChecksum(checksum);
        result.setSubtype(subtype);
        result.setType(type);
        result.setTimeStamp(timestamp);

        return result;
    }
}
