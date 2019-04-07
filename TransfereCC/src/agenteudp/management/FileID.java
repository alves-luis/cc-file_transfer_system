package agenteudp.management;

import agenteudp.PDU;
import agenteudp.PDUTypes;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class FileID extends PDUManagement {

    public static byte LISTING = 0;
    public static byte FILE = 1;

    private String fileID;

    public FileID(long seqNumber, byte direction, String file) {
        super(seqNumber,PDUTypes.M_FILE, direction );
        this.fileID = file;
    }

    @Override
    public byte[] generatePDU() {
        byte[] basePDU = super.generatePDU();
        byte[] fileNameAsBytes = fileID.getBytes(StandardCharsets.UTF_8);

        byte[] finalPDU = new byte[basePDU.length + fileNameAsBytes.length + 1];

        System.arraycopy(basePDU,0,finalPDU,0, basePDU.length);
        finalPDU[basePDU.length + 1] = super.getDirection();
        System.arraycopy(fileNameAsBytes,0,finalPDU,basePDU.length + 1,fileNameAsBytes.length);

        byte[] checksum = super.generateChecksum(finalPDU);
        System.arraycopy(checksum,0,finalPDU,0,checksum.length);

        return finalPDU;
    }

    public static FileID degeneratePDU(byte[] data) {
        PDU pdu = PDU.degeneratePDU(data);
        byte[] file = Arrays.copyOfRange(data,PDU.BASE_PDU_SIZE,data.length);
        String fileName = new String(file,StandardCharsets.UTF_8);
        byte direction = data[BASE_PDU_SIZE + 1];
        FileID fileID = new FileID(pdu.getSeqNumber(), direction, fileName);
        return fileID;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String PDU = super.toString();
        // need to concat superclass toString
        sb.append("FileID: ").append(this.fileID).append("\n");
        return sb.toString();
    }
}
