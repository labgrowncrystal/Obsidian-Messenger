package dev.obsidian.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * PBKDF2WithHmacSHA256 Master Key Derivation for Local Vault Encryption (chat_history.enc & contacts.enc).
 * Features 600,000 Iterations (OWASP Standard) & char[] Memory Hygiene Wiping.
 */
public class PBKDF2Helper {
    public static final int OWASP_RECOMMENDED_ITERATIONS = 600000;
    private static final int KEY_LENGTH = 256;

    public static SecretKey deriveMasterKey(char[] passphrase, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(passphrase, salt, OWASP_RECOMMENDED_ITERATIONS, KEY_LENGTH);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
            
            // Clear temporary keyBytes array for memory hygiene
            Arrays.fill(keyBytes, (byte) 0);
            return secretKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to derive PBKDF2 key: " + e.getMessage(), e);
        }
    }

    public static void wipePassphrase(char[] passphrase) {
        if (passphrase != null) {
            Arrays.fill(passphrase, '\0');
        }
    }
}
