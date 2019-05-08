package security;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

/**
 *
 * @author alvesluis
 */
public class Keys {
    
    /* Pair of public and private keys */
    private KeyPair pair;
    /* Agreed key for communication exchange */
    private SecretKey aesKey;

    public Keys(KeyPair p) {
        this.pair = p;
        this.aesKey = null;
    }
    
    /**
     * Default constructor. Uses 'RSA' and strongest instance of 
     * random generator
     */
    public Keys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom randomNumber = SecureRandom.getInstanceStrong();
            keyGen.initialize(2048, randomNumber);
            this.pair = keyGen.generateKeyPair();
            
            KeyGenerator fac = KeyGenerator.getInstance("AES");
            fac.init(128);
            this.aesKey = fac.generateKey();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Returns the publicKey of this object
     * @return publicKey
     */
    public byte[] getRSAPublicKey() {
        return this.pair.getPublic().getEncoded();
    }
    
    /**
     * Sets this key's AES key
     * @param aesKey
     */
    public void setAESKey(byte[] aesKey) {
        this.aesKey = new SecretKeySpec(aesKey, "AES");
    }
    
    
    /**
     * This method, given a public key (in a byte array), encrypts my AES key
     * using RSA.
     * @param pubKey
     * @return null or encrypted data
     */
    public byte[] encryptRSA(byte[] pubKey) {
        byte[] encryptedData = null;
        try {
            KeyFactory factory = KeyFactory.getInstance("RSA");
            PublicKey key = factory.generatePublic(new X509EncodedKeySpec(pubKey));
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE,key);
            encryptedData = cipher.doFinal(this.aesKey.getEncoded());
        } catch (InvalidKeySpecException | InvalidKeyException | NoSuchAlgorithmException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException ex) {
            System.err.println(ex.toString());
        }
        return encryptedData;
    }
    
    /**
     * Decrypts an array of data encrypted with my public key
     * @param data
     * @return
     */
    public byte[] decryptRSA(byte[] data) {
        byte[] decryptedData = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, this.pair.getPrivate());
            decryptedData = cipher.doFinal(data);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            System.err.println(ex.toString());
        }
        return decryptedData;
    }
    
    /**
     * Given an array of data, encrypts it with my AES key
     * @param data
     * @return
     */
    public byte[] encryptAES(byte[] data) {
        byte[] encryptedData = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, this.aesKey);
            encryptedData = cipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            System.err.println(ex.toString());
        }
        return encryptedData;
    }
    
    /**
     * Given an array of data, decrypts it with my AES key.
     * @param data
     * @return
     */
    public byte[] decryptAES(byte[] data) {
        byte[] decryptedData = null;
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, this.aesKey);
            decryptedData = cipher.doFinal(data);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException ex) {
            System.err.println(ex.toString());
        }
        return decryptedData;
    }

    public Keys clone() {
        KeyPair p = new KeyPair(this.pair.getPublic(),this.pair.getPrivate());
        return new Keys(p);
    }
}
