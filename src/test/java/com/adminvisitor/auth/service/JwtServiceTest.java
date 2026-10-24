
package com.adminvisitor.auth.service;

import com.adminvisitor.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    @TempDir
    Path tempDir;

    private JwtService jwtService;
    private Path privateKeyPath;
    private Path publicKeyPath;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        privateKeyPath = tempDir.resolve("private.pem");
        publicKeyPath = tempDir.resolve("public.pem");

        Files.writeString(
                privateKeyPath,
                "-----BEGIN PRIVATE KEY-----\n"
                        + Base64.getEncoder().encodeToString(
                        keyPair.getPrivate().getEncoded())
                        + "\n-----END PRIVATE KEY-----\n"
        );

        Files.writeString(
                publicKeyPath,
                "-----BEGIN PUBLIC KEY-----\n"
                        + Base64.getEncoder().encodeToString(
                        keyPair.getPublic().getEncoded())
                        + "\n-----END PUBLIC KEY-----\n"
        );

        jwtService = new JwtService(
                privateKeyPath.toString(),
                publicKeyPath.toString(),
                900000,
                2700000
        );
    }

    @Test
    void testGenerateTokenStateAuthenticated() {
        String token = jwtService.generateToken("USR001", "ADMIN");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("USR001", jwtService.extractUserId(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertEquals(
                JwtService.STATE_AUTHENTICATED,
                jwtService.extractState(token)
        );
    }

    @Test
    void testGenerateRestrictedTokenStateInitialPasswordChangeRequired() {
        String token = jwtService.generateRestrictedToken(
                "USR002", "HR");

        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("USR002", jwtService.extractUserId(token));
        assertEquals("HR", jwtService.extractRole(token));
        assertEquals(
                JwtService.STATE_INITIAL_PASSWORD_CHANGE_REQUIRED,
                jwtService.extractState(token)
        );
    }

    @Test
    void testExpiredTokenValidationFails() {
        JwtService shortLivedJwtService = new JwtService(
                privateKeyPath.toString(),
                publicKeyPath.toString(),
                -1000,
                2700000
        );

        String expiredToken = shortLivedJwtService.generateToken(
                "USR001", "ADMIN");

        assertFalse(jwtService.isTokenValid(expiredToken));
    }

    @Test
    void testTamperedTokenValidationFails() {
        String validToken = jwtService.generateToken("USR001", "ADMIN");
        String tamperedToken = validToken + "tampered";

        assertFalse(jwtService.isTokenValid(tamperedToken));
    }

    @Test
    void testInvalidToken() {
        assertFalse(jwtService.isTokenValid("invalid.token.string"));
    }
}