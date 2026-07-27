package dev.obsidian.storage;

import dev.obsidian.crypto.PBKDF2Helper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class VaultManagerTest {

    @BeforeEach
    public void setup() {
        VaultManager.lockVaultSession();
        // Clean test files if any
        try {
            Path dir = VaultManager.getVaultDir();
            Files.deleteIfExists(dir.resolve(VaultManager.CONTACTS_FILE));
            Files.deleteIfExists(dir.resolve(VaultManager.CHAT_HISTORY_FILE));
        } catch (Exception ignored) {}
    }

    @Test
    public void testVaultFirstTimeSetupAndReturningUserUnlock() throws Exception {
        char[] passphrase = "MasterSecretPassword123!".toCharArray();

        // 1. First time setup: Vault files don't exist yet
        assertTrue(VaultManager.unlockVault(passphrase));
        assertTrue(VaultManager.isVaultUnlocked());
        assertTrue(VaultManager.vaultFileExists(VaultManager.CONTACTS_FILE));

        // Lock session to simulate game restart
        VaultManager.lockVaultSession();
        assertFalse(VaultManager.isVaultUnlocked());

        // 2. Returning user with WRONG password
        char[] wrongPass = "WrongPassphrase!".toCharArray();
        assertFalse(VaultManager.unlockVault(wrongPass));
        assertFalse(VaultManager.isVaultUnlocked());

        // 3. Returning user with CORRECT password
        char[] correctPass = "MasterSecretPassword123!".toCharArray();
        assertTrue(VaultManager.unlockVault(correctPass));
        assertTrue(VaultManager.isVaultUnlocked());
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

        assertTrue(VaultManager.unlockVault(passphrase));
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
}
