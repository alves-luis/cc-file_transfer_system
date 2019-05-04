package transferecc;

import security.Keys;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Random;
import java.util.TreeMap;

public class State {

    private Keys communicationKeys;

    public State() {
        this.communicationKeys = new Keys();
    }

    public void setAESKey(byte[] aesKey) {
        this.communicationKeys.setAESKey(aesKey);
    }

    public byte[] getRSAPublicKey() {
        return this.communicationKeys.getRSAPublicKey();
    }

    public byte[] decryptWithMyRSA(byte[] data) {
        return this.communicationKeys.decryptRSA(data);
    }

    public Keys getKeys() {
        return this.communicationKeys;
    }
}
