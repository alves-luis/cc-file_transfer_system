package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.util.Arrays;

public class KeyExchange extends PDUControl {

    private byte[] key; // key that is being sent

    public KeyExchange(long seqNumber, byte[] key) {
        super(seqNumber, PDUTypes.C_KEY_EXCHANGE);
        this.key = key;
    }

    public byte[] getKey() {
        return this.key;
    }

    public byte[] generatePDU() {
        byte[] basePDU = super.generatePDU();
        byte[] finalPDU = new byte[basePDU.length + key.length];

        System.arraycopy(basePDU,0,finalPDU,0,basePDU.length);
        System.arraycopy(this.key,0,finalPDU,basePDU.length,this.key.length);

        byte[] checksum = super.generateChecksum(finalPDU);
        System.arraycopy(checksum,0,finalPDU,0,checksum.length);

        return finalPDU;
    }

    public static KeyExchange degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);

        int sizeOfKey = data.length - BASE_PDU_SIZE;
        byte[] key = new byte[sizeOfKey];
        System.arraycopy(data,BASE_PDU_SIZE,key,0,sizeOfKey);

        KeyExchange result = new KeyExchange(pdu.getSeqNumber(),key);
        result.setTimeStamp(pdu.getTimeStamp());
        result.setChecksum(pdu.getChecksum());
        result.setSeqNumber(pdu.getSeqNumber());
        return result;
    }

    public String toString() {
        String s = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(s).append("Key: ").append(Arrays.toString(key)).append("\n");
        return sb.toString();
    }
}
