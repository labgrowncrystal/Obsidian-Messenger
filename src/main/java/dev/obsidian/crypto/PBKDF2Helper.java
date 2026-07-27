package dev.obsidian.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * PBKDF2WithHmacSHA256 Master Key Derivation for Local Vault Encryption (chat_history.enc & contacts.enc).
 */
public class PBKDF2Helper {
    private static final int ITERATION_COUNT = 100000;
    private static final int KEY_LENGTH = 256;

    public static SecretKey deriveMasterKey(char[] passphrase, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passphrase, salt, ITERATION_COUNT, KEY_LENGTH);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to derive PBKDF2 key: " + e.getMessage(), e);
        }
    }
}
