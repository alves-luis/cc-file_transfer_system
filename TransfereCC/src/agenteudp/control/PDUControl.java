package agenteudp.control;

import agenteudp.PDU;
import agenteudp.PDUTypes;

public abstract class PDUControl extends PDU {

    public PDUControl(long seqNumber, byte subtype) {
        super(seqNumber,PDUTypes.CONTROL,subtype);
    }

    public PDUControl(PDU p, byte subtype) {
        super(p, PDUTypes.CONTROL, subtype);
    }
}
