package agenteudp;

public class PDUTypes {

    public static final byte CONTROL = 0;

    public static final byte C_ACK = 0;
    public static final byte C_CONNECTION_REQUEST = 1;
    public static final byte C_AUTHENTICATION_REQUEST = 2;
    public static final byte C_CONNECTION_TERMINATION = 3;
    public static final byte C_KEY_EXCHANGE = 4;

    public static final byte MANAGEMENT = 1;

    public static final byte M_TYPE = 0;
    public static final byte M_FILE = 1;

    public static final byte DATA = 2;

    public static final byte D_FIRST = 0;
    public static final byte D_OTHER = 1;

}
