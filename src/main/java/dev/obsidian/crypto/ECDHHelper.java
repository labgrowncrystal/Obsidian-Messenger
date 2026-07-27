package dev.obsidian.crypto;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

/**
 * Elliptic Curve Diffie-Hellman (ECDH secp256r1) Ephemeral Key Agreement Helper.
 */
public class ECDHHelper {

    public static class ECDHKeyPair {
        public final PrivateKey privateKey;
        public final PublicKey publicKey;
        public final String publicKeyBase64;

        public ECDHKeyPair(PrivateKey privateKey, PublicKey publicKey) {
            this.privateKey = privateKey;
            this.publicKey = publicKey;
            this.publicKeyBase64 = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        }
    }

    public static ECDHKeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        keyPairGenerator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return new ECDHKeyPair(keyPair.getPrivate(), keyPair.getPublic());
    }

    public static SecretKey deriveSharedSecret(PrivateKey privateKey, String remotePublicKeyBase64) throws Exception {
        byte[] remoteKeyBytes = Base64.getDecoder().decode(remotePublicKeyBase64);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        PublicKey remotePublicKey = keyFactory.generatePublic(new X509EncodedKeySpec(remoteKeyBytes));

        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(remotePublicKey, true);

        byte[] sharedSecret = keyAgreement.generateSecret();

        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] derivedAesKey = sha256.digest(sharedSecret);

        return new SecretKeySpec(derivedAesKey, "AES");
    }
}
