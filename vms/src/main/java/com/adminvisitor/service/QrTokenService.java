package com.adminvisitor.service;

import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;

@Service
public class QrTokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateToken() {

        try {
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(randomBytes);

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(
                        String.format("%02x", b)
                );
            }

            return hexString.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to generate QR token",
                    e
            );
        }
    }
}