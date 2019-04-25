package transferecc;

public class FileNotCompleteException extends Exception {

    public FileNotCompleteException(long sizeOfFile, long lengthOfFileReceived) {
        super("File Size: " + sizeOfFile + ". Size received: " + lengthOfFileReceived);
    }
}
