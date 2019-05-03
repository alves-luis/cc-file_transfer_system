package agenteudp.management;

import agenteudp.PDU;
import agenteudp.PDUTypes;

public abstract class PDUManagement extends PDU {

    public static byte UPLOAD = 0;
    public static byte DOWNLOAD = 1;

    private byte direction;

    public PDUManagement(long seqNumber, byte subtype, byte direction) {
        super(seqNumber, PDUTypes.MANAGEMENT, subtype);
        this.direction = direction;
    }

    public PDUManagement(PDU p, byte subtype, byte direction) {
        super(p, PDUTypes.MANAGEMENT,subtype);
        this.direction = direction;
    }

    public byte getDirection() {
        return this.direction;
    }

    public void setDirection(byte dir) {
        this.direction = dir;
    }

    @Override
    public String toString() {
        return super.toString() + "Direction: " + direction + "\n";
    }
}
