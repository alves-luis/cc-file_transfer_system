package security;

/**
 *
 * @author alvesluis
 */
public class SecurityTest {
    
    
    public static void main(String[] args) {
        testKeysOne();
    }

    private static void testKeysOne() {
        Keys myKeys = new Keys();
        Keys otherKeys = new Keys();
        
        String message = "This is a secret message!";

        System.out.println("Me: Encrypting the AES key with your public key!");
        byte[] encryptedAES = myKeys.encryptRSA(otherKeys.getRSAPublicKey());
        
        System.out.println("Them: Decrypting the AES key with my private key!");
        byte[] decryptedAES = otherKeys.decryptRSA(encryptedAES);
        
        System.out.println("Them: Updating my AES key for communicating with you!");
        otherKeys.setAESKey(decryptedAES);
        
        System.out.println("Me: Encrypting a message!");
        byte[] encryptedMessage = myKeys.encryptAES(message.getBytes());
        
        System.out.println("Them: Decrypting the message!");
        byte[] finalDecryption = otherKeys.decryptAES(encryptedMessage);
        
        String finalMessage = new String(finalDecryption);
        System.out.println("Message fully decrypted: " + finalMessage);
    }
}
