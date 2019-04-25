package agenteudp.data;

import agenteudp.PDU;
import agenteudp.PDUTypes;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FirstBlockData extends PDU {

    private static int SIZE_OF_HASH_ARRAY = 20;

    private short sizeOfFileID;
    private String fileID;
    private int fileSize;
    private byte[] hash;
    private byte[] data;


    public FirstBlockData(long seqNumber, String fileId, int fileSize, byte[] hash, byte[] data){
        super(seqNumber,PDUTypes.DATA,PDUTypes.D_FIRST);
        Integer intSizeOfID = fileId.length();
        this.sizeOfFileID = intSizeOfID.shortValue();
        this.fileID = fileId;
        this.fileSize = fileSize;
        this.hash = hash;
        this.data = data;
    }

    public FirstBlockData(PDU pdu, short sizeOfFileID, String fileID, int fileSize, byte[] hash, byte[] data) {
        super(pdu, PDUTypes.DATA, PDUTypes.D_FIRST);
        this.sizeOfFileID = sizeOfFileID;
        this.fileID = fileID;
        this.fileSize = fileSize;
        this.hash = hash;
        this.data = data;
    }

    public String getFileId(){
        return fileID;
    }

    public short getSizeOfFileID() {
        return this.sizeOfFileID;
    }

    public int getFileSize(){
        return fileSize;
    }


    public byte[] getData(){
        return data;
    }

    public byte[] getHash() {
        return this.hash;
    }


    @Override
    public byte[] generatePDU() {
        byte[] basePDU = super.generatePDU();
        byte[] sizeOfFileIdAsBytes = ByteBuffer.allocate(2).putShort(this.sizeOfFileID).array();
        byte[] fileIdAsBytes = this.fileID.getBytes(Charset.forName("UTF-8"));
        byte[] fileSizeAsByte = ByteBuffer.allocate(4).putInt(this.fileSize).array();

        byte[] finalPDU = new byte[basePDU.length + sizeOfFileIdAsBytes.length +
                fileIdAsBytes.length + fileSizeAsByte.length + hash.length + data.length];

        int currentOffset = 0;

        // Copy basePDU into final PDU
        System.arraycopy(basePDU,0,finalPDU, currentOffset, basePDU.length);
        currentOffset += basePDU.length;

        // copy size of file ID into final PDU
        System.arraycopy(sizeOfFileIdAsBytes,0,finalPDU, currentOffset,sizeOfFileIdAsBytes.length);
        currentOffset += sizeOfFileIdAsBytes.length;

        // copy the file ID into final PDU
        System.arraycopy(fileIdAsBytes,0,finalPDU, currentOffset, fileIdAsBytes.length);
        currentOffset += fileIdAsBytes.length;

        // copy the size of file into final PDU
        System.arraycopy(fileSizeAsByte,0,finalPDU,currentOffset, 4);
        currentOffset += 4;

        // copy the hash into final PDU
        System.arraycopy(this.hash,0, finalPDU, currentOffset, this.hash.length);
        currentOffset += this.hash.length;

        // copy the data into final PDU
        System.arraycopy(this.data,0,finalPDU,currentOffset, data.length);

        // Generate checksum and put it in final PDU
        byte[] checksum = super.generateChecksum(finalPDU);
        System.arraycopy(checksum,0,finalPDU,0,checksum.length);

        return finalPDU;
    }


    public static FirstBlockData degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);

        int currentOffSet = PDU.BASE_PDU_SIZE;
        short sizeOfFileID = ByteBuffer.wrap(data,currentOffSet, 2).getShort();
        currentOffSet += 2;

        byte[] encodedFileID = Arrays.copyOfRange(data, currentOffSet, currentOffSet + sizeOfFileID);
        currentOffSet += sizeOfFileID;
        String fileID = new String(encodedFileID, Charset.forName("UTF-8"));

        int fileSize = ByteBuffer.wrap(data,currentOffSet, 4).getInt();
        currentOffSet += 4;

        byte[] hash = Arrays.copyOfRange(data,currentOffSet, currentOffSet + SIZE_OF_HASH_ARRAY);
        currentOffSet += SIZE_OF_HASH_ARRAY;

        byte[] dados = Arrays.copyOfRange(data,currentOffSet, data.length);


        return new FirstBlockData(pdu, sizeOfFileID, fileID, fileSize, hash ,dados );
    }

    public static byte[] getHash(byte[] data){
        byte[] sol = null;
        try{
            MessageDigest md;
            md = MessageDigest.getInstance("SHA-1");
            sol = md.digest(data);
        }
        catch(NoSuchAlgorithmException e){
            System.err.println(e.toString());
        }
        return sol;
    }

    public static void main(String[] args) {
        PDU p = new PDU(123, PDUTypes.DATA,PDUTypes.D_FIRST);
        byte[] dados = new byte[23];
        for(int i = 0; i < 23; i++)
            dados[i] = 1;
        byte[] hash = getHash(dados);
        FirstBlockData pdu = new FirstBlockData(p,(short) "program_text".length(),"program_text",23,hash,dados);
        System.out.println(pdu.toString());
        byte[] degenerate = pdu.generatePDU();
        System.out.println(FirstBlockData.degeneratePDU(degenerate).toString());
    }

    public String toString() {
        String s = super.toString();
        StringBuilder sb = new StringBuilder();
        sb.append(s);
        sb.append("Size of File ID: ").append(this.sizeOfFileID).append("\n");
        sb.append("FileID: ").append(this.fileID).append("\n");
        sb.append("Filesize: ").append(this.fileSize).append("\n");
        sb.append("Hash: ").append(Arrays.toString(this.hash)).append("\n");
        sb.append("Data: ").append(Arrays.toString(this.data)).append("\n");
        return sb.toString();
    }
}