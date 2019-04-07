package agenteudp;

public class InvalidCRCException extends Exception {
    public InvalidCRCException() {
        super();
    }

    public InvalidCRCException(String msg) {
        super(msg);
    }
}
