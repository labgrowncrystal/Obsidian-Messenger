package dev.obsidian.storage;

import dev.obsidian.crypto.PBKDF2Helper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class VaultManagerTest {

    @Test
    public void testVaultEncryptionAndDecryptionWithSaltHeader() throws Exception {
        char[] passphrase = "MasterSecretPassword123!".toCharArray();
        String jsonPayload = "{\"contacts\":[{\"name\":\"Alex\",\"token\":\"OM-123456\"}]}";

        byte[] encrypted = VaultManager.encryptVaultData(jsonPayload, passphrase);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 32); // MAGIC + SALT + IV + CIPHERTEXT

        String decrypted = VaultManager.decryptVaultData(encrypted, passphrase);
        assertEquals(jsonPayload, decrypted);

        PBKDF2Helper.wipePassphrase(passphrase);
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

        PBKDF2Helper.wipePassphrase(correctPass);
        PBKDF2Helper.wipePassphrase(wrongPass);
    }
}
