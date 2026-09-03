package com.ucc.chatbot.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM encryption for stored API secrets.
 * - 256-bit key derived from the ENCRYPTION_KEY env var (must be set in prod).
 * - 12-byte random IV per message, prepended to the ciphertext.
 * - 128-bit GCM auth tag.
 *
 * Format: Base64( IV (12 bytes) || ciphertext || GCM tag (16 bytes) )
 *
 * If ENCRYPTION_KEY is missing or shorter than 16 characters, the bean's
 * methods will throw — encryption keys must be configured explicitly.
 */
@Component
public class EncryptionUtil {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LEN = 12;
    private static final int TAG_BITS = 128;
    private static final String FALLBACK_KEY = "ucc-chatbot-default-secret-key-32";

    private final SecretKey keySpec;
    private final boolean insecureDefault;

    public EncryptionUtil(@Value("${encryption.key:}") String key) {
        if (key == null || key.isBlank() || FALLBACK_KEY.equals(key)) {
            this.insecureDefault = true;
            this.keySpec = null;
        } else {
            this.insecureDefault = false;
            this.keySpec = new SecretKeySpec(deriveKey(key), "AES");
        }
    }

    public String encrypt(String value) {
        if (value == null) return null;
        if (insecureDefault) {
            throw new IllegalStateException("Refusing to encrypt: ENCRYPTION_KEY is not configured. Set a 32+ char secret in your environment.");
        }
        try {
            byte[] iv = new byte[IV_LEN];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            byte[] ct = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[IV_LEN + ct.length];
            System.arraycopy(iv, 0, out, 0, IV_LEN);
            System.arraycopy(ct, 0, out, IV_LEN, ct.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    public String decrypt(String encoded) {
        if (encoded == null) return null;
        if (insecureDefault) {
            throw new IllegalStateException("Refusing to decrypt: ENCRYPTION_KEY is not configured.");
        }
        try {
            byte[] all = Base64.getDecoder().decode(encoded);
            if (all.length < IV_LEN + 16) throw new IllegalArgumentException("Ciphertext too short");
            byte[] iv = Arrays.copyOfRange(all, 0, IV_LEN);
            byte[] ct = Arrays.copyOfRange(all, IV_LEN, all.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    public boolean isInsecureDefault() { return insecureDefault; }

    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return sha.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }
}
