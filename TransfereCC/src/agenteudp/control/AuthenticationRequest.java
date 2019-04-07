package agenteudp.control;


import agenteudp.PDUTypes;


public class AuthenticationRequest extends PDUControl {


    public AuthenticationRequest(long seqNumber) {
        super(seqNumber, PDUTypes.C_AUTHENTICATION_REQUEST);
    }
}
