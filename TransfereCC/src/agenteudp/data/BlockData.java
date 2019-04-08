package agenteudp.data;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.nio.ByteBuffer;

public class BlockData extends PDU {

    private long fileID;
    private int offset;
    private byte[] data;

    public BlockData(long seqNumber, long fileID, int offset, byte[] data) {
        super(seqNumber, PDUTypes.DATA, PDUTypes.D_OTHER);
    }

    @Override
    public byte[] generatePDU() {
        byte[] basePDU = super.generatePDU();
        byte[] fileIDAsBytes = ByteBuffer.allocate(8).putLong(this.fileID).array();
        byte[] offsetAsBytes = ByteBuffer.allocate(4).putInt(this.offset).array();

        byte[] finalPDU = new byte[basePDU.length + fileIDAsBytes.length + offsetAsBytes.length + data.length];

        
    }
}
