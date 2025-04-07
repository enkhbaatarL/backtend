package com.sk.common.utils;

import com.sk.common.enums.ResultCode;
import com.sk.common.exception.SystemErrorException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class AESUtil {

    private static final String CIPHER_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";

    private static final int AES_KEY_SIZE = 16; // 128bit
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private static final SecureRandom secureRandom = new SecureRandom();

    public static String generateSecret() {
//        byte[] randomBytes = new byte[byteLength];
//        secureRandom.nextBytes(randomBytes);
//        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        UUID uuid = UUID.randomUUID();
        byte[] uuidBytes = asBytes(uuid);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes);
    }

    private static byte[] asBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    public static String encrypt(String plaintext, String key) {
        try {
            byte[] keyBytes = validateKey(key);
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);

            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] encrypted = new byte[iv.length + cipherBytes.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(cipherBytes, 0, encrypted, iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw SystemErrorException.of(ResultCode.Error.SEC_ENCRYPTION_FAILED, e.getMessage());
        }
    }

    public static String decrypt(String encryptedBase64, String key) {
        try {
            byte[] keyBytes = validateKey(key);
            byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] cipherBytes = new byte[encrypted.length - GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, iv.length);
            System.arraycopy(encrypted, iv.length, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance(CIPHER_TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw SystemErrorException.of(ResultCode.Error.SEC_DECRYPTION_FAILED, e.getMessage());
        }
    }

    private static byte[] validateKey(String key) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != AES_KEY_SIZE) {
            throw SystemErrorException.of(ResultCode.Error.SEC_INVALID_KEY, "Key must be 16 bytes (128 bits)");
        }
        return keyBytes;
    }

}
