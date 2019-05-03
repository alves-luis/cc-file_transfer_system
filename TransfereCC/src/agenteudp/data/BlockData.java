package agenteudp.data;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

public class BlockData extends PDU {

    /* Default value to be used when sending the header of file */
    private static int DEFAULT_HEADER_DATA_SIZE = 512;

    private short sizeOfFileID;
    private String fileID;
    private int offset;
    private byte[] data;

    public BlockData(long seqNumber, String fileID, int offset, byte[] data) {
        super(seqNumber, PDUTypes.DATA, PDUTypes.D_OTHER);
        this.sizeOfFileID = ((Integer) fileID.length()).shortValue();
        this.fileID=fileID;
        this.offset=offset;
        this.data=data;
    }

    public BlockData(PDU p, short sizeOfFileID, String fileID, int offset, byte[] data) {
        super(p,PDUTypes.DATA, PDUTypes.D_OTHER);
        this.sizeOfFileID = sizeOfFileID;
        this.fileID = fileID;
        this.offset = offset;
        this.data = data;
    }
    
    
    public String getFileId(){
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
        byte[] sizeOfFileIdAsBytes = ByteBuffer.allocate(2).putShort(this.sizeOfFileID).array();
        byte[] fileIDAsBytes = this.fileID.getBytes(Charset.forName("UTF-8"));
        byte[] offsetAsBytes = ByteBuffer.allocate(4).putInt(this.offset).array();

        byte[] finalPDU = new byte[basePDU.length + sizeOfFileIdAsBytes.length +
                fileIDAsBytes.length + offsetAsBytes.length + data.length];

        int currentOffset = 0;

        // Copy basePDU into final PDU
        System.arraycopy(basePDU,0,finalPDU,0, basePDU.length);
        currentOffset += basePDU.length;

        // Copy size of file ID into final PDU
        System.arraycopy(sizeOfFileIdAsBytes, 0, finalPDU, currentOffset, sizeOfFileIdAsBytes.length);
        currentOffset += sizeOfFileIdAsBytes.length;

        // copy the file ID into final PDU
        System.arraycopy(fileIDAsBytes,0, finalPDU, currentOffset, fileIDAsBytes.length);
        currentOffset += fileIDAsBytes.length;

        // Copy the offset into final PDU
        System.arraycopy(offsetAsBytes,0, finalPDU, currentOffset, offsetAsBytes.length);
        currentOffset += offsetAsBytes.length;

        // copy the data into final PDU
        System.arraycopy(this.data,0,finalPDU, currentOffset, this.data.length);

        // generate checksum and put it into final PDU
        byte[] checksum = super.generateChecksum(finalPDU);
        System.arraycopy(checksum,0,finalPDU,0,checksum.length);
        
        return finalPDU;

    }
    
    public static BlockData degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);

        int currentOffset = PDU.BASE_PDU_SIZE;
        short sizeOfFileID = ByteBuffer.wrap(data, currentOffset, 2).getShort();
        currentOffset += 2;

        byte[] encodedFileID = Arrays.copyOfRange(data, currentOffset, currentOffset + sizeOfFileID);
        currentOffset += sizeOfFileID;
        String fileID = new String(encodedFileID, Charset.forName("UTF-8"));

        int offSet = ByteBuffer.wrap(data,currentOffset, currentOffset + 4).getInt();
        currentOffset += 4;
        
        byte[] dados = Arrays.copyOfRange(data,currentOffset , data.length);

        return new BlockData(pdu, sizeOfFileID, fileID, offSet, dados);
    }

    /**
     * Given the byte[] and an offset, returns the truncated byte[]
     * if its length-offset is bigger than DEFAULT HEADER DATA SIZE
     * @param data array to retrieve the chunk from
     * @param offset index from which to retrieve the chunk
     * @return returns the piece that should be sent
     */
    public static byte[] getDefaultChunkOfData(byte[] data, int offset) {
        int sizeOfChunk = data.length-offset > DEFAULT_HEADER_DATA_SIZE ? DEFAULT_HEADER_DATA_SIZE : data.length - offset;
        byte[] result = new byte[sizeOfChunk];
        System.arraycopy(data,offset,result,0,sizeOfChunk);
        return result;
    }

    
}



   
    
     