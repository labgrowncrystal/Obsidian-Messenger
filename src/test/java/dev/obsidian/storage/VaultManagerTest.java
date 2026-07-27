package dev.obsidian.storage;

import dev.obsidian.crypto.PBKDF2Helper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VaultManagerTest {

    @BeforeEach
    public void setup() {
        VaultManager.lockVaultSession();
    }

    @Test
    public void testVaultEncryptionAndDecryptionWithSaltHeader() throws Exception {
        char[] passphrase = "MasterSecretPassword123!".toCharArray();
        String jsonPayload = "{\"contacts\":[{\"name\":\"Alex\",\"token\":\"OM-123456\"}]}";

        byte[] encrypted = VaultManager.encryptVaultData(jsonPayload, passphrase);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 32); // MAGIC + SALT + IV + CIPHERTEXT

        char[] passphrase2 = "MasterSecretPassword123!".toCharArray();
        String decrypted = VaultManager.decryptVaultData(encrypted, passphrase2);
        assertEquals(jsonPayload, decrypted);
    }

    @Test
    public void testCachedSessionKeyPerformance() throws Exception {
        char[] passphrase = "SessionSecretPassword!".toCharArray();
        String jsonPayload = "{\"msg\":\"Instant speed test\"}";

        assertTrue(VaultManager.unlockVaultSession(passphrase, null));
        assertTrue(VaultManager.isVaultUnlocked());

        long start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            byte[] enc = VaultManager.encryptVaultData(jsonPayload, null);
            String dec = VaultManager.decryptVaultData(enc, null);
            assertEquals(jsonPayload, dec);
        }
        long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 50, "Cached session saves should execute in < 50ms total for 5 ops!");

        VaultManager.lockVaultSession();
        assertFalse(VaultManager.isVaultUnlocked());
    }

    @Test
    public void testWrongPassphraseFailsDecryption() throws Exception {
        char[] correctPass = "CorrectPassword".toCharArray();
        char[] wrongPass = "WrongPassword".toCharArray();
        String jsonPayload = "Secret Content";

        byte[] encrypted = VaultManager.encryptVaultData(jsonPayload, correctPass);

        assertThrows(Exception.class, () -> {
            VaultManager.decryptVaultData(encrypted, wrongPass);
        });
    }
}
