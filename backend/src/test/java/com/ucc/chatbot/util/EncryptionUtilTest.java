package com.ucc.chatbot.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EncryptionUtilTest {

    private static final String SECRET = "test-encryption-secret-for-junit";

    @Test
    void encryptAndDecryptRoundTrip() {
        EncryptionUtil encryptionUtil = new EncryptionUtil(SECRET);

        String encrypted = encryptionUtil.encrypt("sensitive value");

        assertFalse(encrypted.isBlank());
        assertEquals("sensitive value", encryptionUtil.decrypt(encrypted));
    }

    @Test
    void nullValuesRemainNull() {
        EncryptionUtil encryptionUtil = new EncryptionUtil(SECRET);

        assertNull(encryptionUtil.encrypt(null));
        assertNull(encryptionUtil.decrypt(null));
    }

    @Test
    void missingKeyRefusesEncryptionAndDecryption() {
        EncryptionUtil encryptionUtil = new EncryptionUtil("");

        assertThrows(IllegalStateException.class, () -> encryptionUtil.encrypt("value"));
        assertThrows(IllegalStateException.class, () -> encryptionUtil.decrypt("encoded"));
    }

    @Test
    void tamperedCiphertextCannotBeDecrypted() {
        EncryptionUtil encryptionUtil = new EncryptionUtil(SECRET);
        String encrypted = encryptionUtil.encrypt("sensitive value");
        char replacement = encrypted.charAt(encrypted.length() - 1) == 'A' ? 'B' : 'A';
        String tampered = encrypted.substring(0, encrypted.length() - 1) + replacement;

        assertThrows(RuntimeException.class, () -> encryptionUtil.decrypt(tampered));
    }
}