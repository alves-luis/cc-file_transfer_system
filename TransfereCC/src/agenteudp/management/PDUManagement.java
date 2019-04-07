package agenteudp.management;

import agenteudp.PDU;
import agenteudp.PDUTypes;

public abstract class PDUManagement extends PDU {

    private byte direction;

    public PDUManagement(long seqNumber, byte subtype, byte direction) {
        super(seqNumber, PDUTypes.MANAGEMENT, subtype);
        this.direction = direction;
    }

    public byte getDirection() {
        return this.direction;
    }

    public void setDirection(byte dir) {
        this.direction = dir;
    }
}
