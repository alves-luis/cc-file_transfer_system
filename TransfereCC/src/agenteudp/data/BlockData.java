package agenteudp.data;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class BlockData extends PDU {

    private long fileID;
    private int offset;
    private byte[] data;

    public BlockData(long seqNumber, long fileID, int offset, byte[] data) {
        super(seqNumber, PDUTypes.DATA, PDUTypes.D_OTHER);
        this.fileID=fileID;
        this.offset=offset;
        this.data=data;
    }
    
    
    public long getFileId(){
        return fileID;
    }
    
    public int getOffset(){
        return offset;
    }
    
    
    public byte[] getData(){
        return data;
    }
    
   

    @Override
    public byte[] generatePDU() {
        byte[] basePDU = super.generatePDU();
        byte[] fileIDAsBytes = ByteBuffer.allocate(8).putLong(this.fileID).array();
        byte[] offsetAsBytes = ByteBuffer.allocate(4).putInt(this.offset).array();

        byte[] finalPDU = new byte[basePDU.length + fileIDAsBytes.length + offsetAsBytes.length + data.length];
       
        System.arraycopy(basePDU,0,finalPDU,0, basePDU.length);
        System.arraycopy(fileIDAsBytes,0,finalPDU,basePDU.length, fileIDAsBytes.length);
        System.arraycopy(offsetAsBytes,0,finalPDU,basePDU.length + 8, offsetAsBytes.length);
        System.arraycopy(data,0,finalPDU,basePDU.length + 12, data.length);

        byte[] checksum = super.generateChecksum(finalPDU);
        System.arraycopy(checksum,0,finalPDU,0,checksum.length);
        
        return finalPDU;

    }
    
    public static BlockData degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);
        
        long fileId = ByteBuffer.wrap(data,PDU.BASE_PDU_SIZE, 8).getLong();
        int offSet = ByteBuffer.wrap(data,PDU.BASE_PDU_SIZE + 8 , 4).getInt();
        
        byte[] dados= Arrays.copyOfRange(data,PDU.BASE_PDU_SIZE+ 12 ,data.length);
       
        BlockData first = new BlockData(pdu.getSeqNumber(), fileId, offSet ,dados );
        return first;
    }

    
}



   
    
     