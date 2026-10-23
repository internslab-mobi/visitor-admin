package com.adminvisitor.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cryptographic utility providing secure random token generation and SHA-256 token hashing.
 * <p>
 * Generates URL-safe Base64 random strings for refresh and reset tokens, and computes hex-encoded SHA-256 digests
 * for secure database persistence.
 * </p>
 *
 * @author Logeshwaran
 */
public class TokenUtils {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a cryptographically strong, URL-safe random opaque token string without padding.
     *
     * @return a 32-byte URL-safe Base64 encoded random token
     */
    public static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Computes the SHA-256 hexadecimal hash digest of the given raw token string.
     *
     * @param rawToken the plaintext token to hash
     * @return hex-encoded SHA-256 hash string, or empty string if input is blank
     */
    public static String hashToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }
}
