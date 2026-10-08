package com.adminvisitor.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom;

    public AesEncryptionService(
            @Value("${crypto.aes.key}") String base64Key) {

        byte[] keyBytes = Base64.getDecoder().decode(base64Key);

        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "AES key must be exactly 256 bits"
            );
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
        this.secureRandom = new SecureRandom();
    }

    public String encrypt(String plainText) {

        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException(
                    "Text to encrypt cannot be null or blank"
            );
        }

        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] encryptedBytes =
                    cipher.doFinal(
                            plainText.getBytes(StandardCharsets.UTF_8)
                    );

            byte[] result =
                    new byte[iv.length + encryptedBytes.length];

            System.arraycopy(
                    iv,
                    0,
                    result,
                    0,
                    iv.length
            );

            System.arraycopy(
                    encryptedBytes,
                    0,
                    result,
                    iv.length,
                    encryptedBytes.length
            );

            return Base64.getEncoder().encodeToString(result);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to encrypt data",
                    exception
            );
        }
    }

    public String decrypt(String encryptedText) {

        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException(
                    "Encrypted text cannot be null or blank"
            );
        }

        try {
            byte[] decoded =
                    Base64.getDecoder().decode(encryptedText);

            if (decoded.length <= IV_LENGTH) {
                throw new IllegalArgumentException(
                        "Invalid encrypted data"
                );
            }

            byte[] iv = new byte[IV_LENGTH];

            byte[] encryptedBytes =
                    new byte[decoded.length - IV_LENGTH];

            System.arraycopy(
                    decoded,
                    0,
                    iv,
                    0,
                    IV_LENGTH
            );

            System.arraycopy(
                    decoded,
                    IV_LENGTH,
                    encryptedBytes,
                    0,
                    encryptedBytes.length
            );

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    parameterSpec
            );

            byte[] decryptedBytes =
                    cipher.doFinal(encryptedBytes);

            return new String(
                    decryptedBytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to decrypt data",
                    exception
            );
        }
    }
}