package dev.obsidian.storage;

import dev.obsidian.client.util.LoggerHelper;
import dev.obsidian.crypto.PBKDF2Helper;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Platform-Agnostic Encrypted Vault Storage Manager for Obsidian Messenger.
 * Stores contacts.enc and chat_history.enc using AES-256-GCM.
 * Supports Cached Session Keys to eliminate 300ms lag spikes during frequent chat saves.
 * Header format: [ MAGIC (4B: OMV1) ] [ SALT (16B) ] [ IV (12B) ] [ CIPHERTEXT ]
 */
public class VaultManager {
    private static final byte[] MAGIC = new byte[]{'O', 'M', 'V', '1'};
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static SecretKey cachedSessionKey = null;
    private static byte[] cachedSessionSalt = null;

    public static class Contact {
        public String id;
        public String name;
        public String tokenOrIp;
        public String password;
        public boolean favorite;
        public long lastSeen;

        public Contact() {}

        public Contact(String id, String name, String tokenOrIp, String password, boolean favorite, long lastSeen) {
            this.id = id;
            this.name = name;
            this.tokenOrIp = tokenOrIp;
            this.password = password;
            this.favorite = favorite;
            this.lastSeen = lastSeen;
        }
    }

    public static class ChatMessage {
        public String id;
        public String contactId;
        public String sender;
        public String text;
        public long timestamp;
        public boolean isOutgoing;

        public ChatMessage() {}

        public ChatMessage(String id, String contactId, String sender, String text, long timestamp, boolean isOutgoing) {
            this.id = id;
            this.contactId = contactId;
            this.sender = sender;
            this.text = text;
            this.timestamp = timestamp;
            this.isOutgoing = isOutgoing;
        }
    }

    public static Path getVaultDir() {
        Path path = LoggerHelper.getOmDir().toPath().resolve("vault");
        try {
            Files.createDirectories(path);
        } catch (Exception e) {
            LoggerHelper.error("VaultManager", "Failed to create vault directory: " + e.getMessage());
        }
        return path;
    }

    /**
     * Unlocks the vault for the current session, caching the derived SecretKey to prevent 300ms lag spikes on every message.
     */
    public static boolean unlockVaultSession(char[] passphrase, byte[] salt) {
        try {
            if (salt == null) {
                salt = new byte[SALT_LENGTH];
                new SecureRandom().nextBytes(salt);
            }
            cachedSessionSalt = salt;
            cachedSessionKey = PBKDF2Helper.deriveMasterKey(passphrase, salt);
            return true;
        } catch (Exception e) {
            LoggerHelper.error("VaultManager", "Failed to unlock vault session: " + e.getMessage());
            return false;
        } finally {
            PBKDF2Helper.wipePassphrase(passphrase);
        }
    }

    public static void lockVaultSession() {
        cachedSessionKey = null;
        cachedSessionSalt = null;
    }

    public static boolean isVaultUnlocked() {
        return cachedSessionKey != null;
    }

    public static byte[] encryptVaultData(String plainJson, char[] masterPassphrase) throws Exception {
        try {
            SecureRandom random = new SecureRandom();
            byte[] salt;
            SecretKey masterKey;

            if (cachedSessionKey != null && cachedSessionSalt != null) {
                salt = cachedSessionSalt;
                masterKey = cachedSessionKey;
            } else {
                salt = new byte[SALT_LENGTH];
                random.nextBytes(salt);
                masterKey = PBKDF2Helper.deriveMasterKey(masterPassphrase, salt);
            }

            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plainJson.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(MAGIC.length + SALT_LENGTH + IV_LENGTH + ciphertext.length);
            buffer.put(MAGIC);
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(ciphertext);

            return buffer.array();
        } finally {
            if (masterPassphrase != null) {
                PBKDF2Helper.wipePassphrase(masterPassphrase);
            }
        }
    }

    public static String decryptVaultData(byte[] vaultFileBytes, char[] masterPassphrase) throws Exception {
        try {
            if (vaultFileBytes.length < (MAGIC.length + SALT_LENGTH + IV_LENGTH)) {
                throw new IllegalArgumentException("Invalid or corrupted vault file size");
            }

            ByteBuffer buffer = ByteBuffer.wrap(vaultFileBytes);
            byte[] fileMagic = new byte[MAGIC.length];
            buffer.get(fileMagic);

            if (!Arrays.equals(MAGIC, fileMagic)) {
                throw new SecurityException("Invalid vault file magic header");
            }

            byte[] salt = new byte[SALT_LENGTH];
            buffer.get(salt);

            byte[] iv = new byte[IV_LENGTH];
            buffer.get(iv);

            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            SecretKey masterKey;
            if (cachedSessionKey != null && cachedSessionSalt != null && Arrays.equals(salt, cachedSessionSalt)) {
                masterKey = cachedSessionKey;
            } else {
                masterKey = PBKDF2Helper.deriveMasterKey(masterPassphrase, salt);
                cachedSessionKey = masterKey;
                cachedSessionSalt = salt;
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } finally {
            if (masterPassphrase != null) {
                PBKDF2Helper.wipePassphrase(masterPassphrase);
            }
        }
    }

    public static void saveEncryptedFile(String filename, String jsonContent, char[] passphrase) throws Exception {
        Path filePath = getVaultDir().resolve(filename);
        byte[] encrypted = encryptVaultData(jsonContent, passphrase);
        Files.write(filePath, encrypted, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        LoggerHelper.info("VaultManager", "Saved encrypted vault file: " + filename + " (" + encrypted.length + " bytes)");
    }

    public static String loadEncryptedFile(String filename, char[] passphrase) throws Exception {
        Path filePath = getVaultDir().resolve(filename);
        if (!Files.exists(filePath)) {
            return null;
        }
        byte[] encrypted = Files.readAllBytes(filePath);
        return decryptVaultData(encrypted, passphrase);
    }

    public static boolean vaultFileExists(String filename) {
        return Files.exists(getVaultDir().resolve(filename));
    }
}
