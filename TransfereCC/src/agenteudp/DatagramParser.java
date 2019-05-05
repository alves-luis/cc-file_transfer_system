package agenteudp;

import agenteudp.control.*;
import agenteudp.data.BlockData;
import agenteudp.data.FirstBlockData;
import agenteudp.management.FileID;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.zip.CRC32;

/**
 * Class which implements helper methods to parse a byte[] into a valid PDU
 */
public class DatagramParser {

    public static PDU processDatagram(byte[] data) throws InvalidCRCException, InvalidTypeOfDatagram {
        PDU result = null;
        byte[] checksum = getChecksum(data);

        if (!verifyIntegrity(data,checksum))
            throw new InvalidCRCException("Invalid CRC!");

        byte type = getType(data);
        byte subtype = getSubtype(data);
        switch(type) {
            case PDUTypes.CONTROL:
                result = parseControl(data,subtype);
                break;
            case PDUTypes.MANAGEMENT:
                result = parseManagement(data,subtype);
                break;
            case PDUTypes.DATA:
                result = parseData(data,subtype);
                break;
        }
        return result;
    }

    private static PDU parseManagement(byte[] data, byte subtype) {
        switch(subtype) {
            case PDUTypes.M_FILE:
                return FileID.degeneratePDU(data);
            case PDUTypes.M_TYPE:
                return null; // will add SOON!
        }
        return null;
    }

    private static PDU parseControl(byte[] data, byte subtype) {
        switch(subtype) {
            case PDUTypes.C_ACK:
                return Ack.degeneratePDU(data);
            case PDUTypes.C_AUTHENTICATION_REQUEST:
                return AuthenticationRequest.degeneratePDU(data);
            case PDUTypes.C_CONNECTION_REQUEST:
                return ConnectionRequest.degeneratePDU(data);
            case PDUTypes.C_CONNECTION_TERMINATION:
                return ConnectionTermination.degeneratePDU(data);
            case PDUTypes.C_KEY_EXCHANGE:
                return KeyExchange.degeneratePDU(data);

        }
        return null;
    }

    private static PDU parseData(byte[] data, byte subtype) {
        switch(subtype) {
            case PDUTypes.D_FIRST:
                return FirstBlockData.degeneratePDU(data);
            case PDUTypes.D_OTHER:
                return BlockData.degeneratePDU(data);
        }
        return null;
    }

    private static byte getType(byte[] data) throws InvalidTypeOfDatagram {
        if (data.length > 8)
            return data[8];
        else
            throw new InvalidTypeOfDatagram();
    }

    private static byte getSubtype(byte[] data) throws InvalidTypeOfDatagram {
        if (data.length > 9)
            return data[9];
        else
            throw new InvalidTypeOfDatagram();
    }

    private static byte[] getChecksum(byte[] data) {
        if (data != null && data.length > 7)
            return Arrays.copyOfRange(data,0,8);
        return null;
    }

    /**
     * Given an array of data, and an array of bytes with the checksum, returns true
     * if the crc of the data matches the checksum value
     * @param data
     * @param checksumValue
     * @return true if they match
     */
    private static boolean verifyIntegrity(byte[] data, byte[] checksumValue) {
        if (checksumValue == null)
            return false;

        CRC32 crc = new CRC32();
        crc.update(data,8,data.length-8);
        long sum = crc.getValue();
        long checksum = ByteBuffer.wrap(checksumValue,0,8).getLong();
        return checksum == sum;
    }

}
