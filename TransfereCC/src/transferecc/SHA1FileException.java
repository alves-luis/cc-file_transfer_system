package transferecc;

import java.util.Arrays;

public class SHA1FileException extends Exception {

    public SHA1FileException(byte[] expectedHash, byte[] resultingHash) {
        super("Expected Hash: " + Arrays.toString(expectedHash) + "\n Got Hash: " + Arrays.toString(resultingHash));
    }
}
