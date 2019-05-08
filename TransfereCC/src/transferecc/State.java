package transferecc;

import security.Keys;

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
