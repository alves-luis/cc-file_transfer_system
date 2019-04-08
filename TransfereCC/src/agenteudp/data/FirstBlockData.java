
package agenteudp.data;

import agenteudp.PDU;
import agenteudp.PDUTypes;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class FirstBlockData extends PDU {
    
    public static byte SIZE = 8;
    
    private long fileID;
    private long fileSize;
    private byte[] hash;
    private byte[] data;
    
    
    public FirstBlockData(long seqNumber, long fileId, long fileSize, byte[] hash, byte[] data){
        super(seqNumber,PDUTypes.DATA,PDUTypes.D_FIRST);
        this.fileID = fileId;
        this.fileSize=fileSize;
        this.hash= hash;
        this.data=data;
    }
    
    public long getFileId(){
        return fileID;
    }
    
    public long getFileSize(){
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
        byte[] fileIdAsBytes = ByteBuffer.allocate(8).putLong(this.fileID).array();
        byte[] fileSizeAsByte = ByteBuffer.allocate(8).putLong(this.fileSize).array();

        byte[] finalPDU = new byte[basePDU.length + fileIdAsBytes.length + fileSizeAsByte.length + hash.length + data.length];

        System.arraycopy(basePDU,0,finalPDU,0, basePDU.length);
        System.arraycopy(fileIdAsBytes,0,finalPDU,basePDU.length,SIZE);
        System.arraycopy(fileSizeAsByte,0,finalPDU,basePDU.length + SIZE, SIZE);
        System.arraycopy(hash,0,finalPDU,basePDU.length + 2 * SIZE ,hash.length);
        System.arraycopy(data,0,finalPDU,basePDU.length + 2*SIZE + hash.length, data.length);

        byte[] checksum = super.generateChecksum(finalPDU);
        System.arraycopy(checksum,0,finalPDU,0,checksum.length);

        return finalPDU;
    }
   
    
     public static FirstBlockData degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);
        
        long fileId = ByteBuffer.wrap(data,PDU.BASE_PDU_SIZE, SIZE).getLong();
        long fileSize = ByteBuffer.wrap(data,PDU.BASE_PDU_SIZE + SIZE , SIZE).getLong();
        
        byte[] hash = Arrays.copyOfRange(data,PDU.BASE_PDU_SIZE + 2 * SIZE , PDU.BASE_PDU_SIZE + 2 * SIZE + 20);
        byte[] dados= Arrays.copyOfRange(data,PDU.BASE_PDU_SIZE+ 2 * SIZE +20 ,data.length);
        System.out.println("HASH: " + hash);
        
       
       
        FirstBlockData first = new FirstBlockData(pdu.getSeqNumber(), fileId, fileSize, hash ,dados );
        return first;
    }
    
    public static void main(String[] args){
        int size=100;
        byte subtype = 1; 
        byte[] aa= new byte[20];
        byte[] b= new byte[size];
        for(int i=0;i<size;i++){
            b[i] = subtype;
        }
        FirstBlockData teste = new FirstBlockData(123,1234,subtype,aa,b);
        byte[] ud= teste.generatePDU();
        
        System.out.print("File ID: " +teste.getFileId() + " FILE SIZE : " + teste.getFileSize() + " HASH: " + Arrays.toString(teste.getHash()) + " DATA: " + Arrays.toString(teste.getData()) + "\n");
        FirstBlockData rest= degeneratePDU(ud);
        System.out.print("File ID: " +rest.getFileId() + " FILE SIZE : " + rest.getFileSize() + " HASH: " + Arrays.toString(rest.getHash()) + " DATA: " + Arrays.toString(rest.getData()) + "\n");
    }
}
