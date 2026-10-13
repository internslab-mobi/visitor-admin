package com.adminvisitor.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET = "0123456789012345678901234567890123456789";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 900000, 2700000);
    }

    @Test
    void testGenerateTokenStateAuthenticated() {
        String token = jwtService.generateToken("USR001", "ADMIN");
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("USR001", jwtService.extractUserId(token));
        assertEquals("ADMIN", jwtService.extractRole(token));
        assertEquals(JwtService.STATE_AUTHENTICATED, jwtService.extractState(token));
    }

    @Test
    void testGenerateRestrictedTokenStateInitialPasswordChangeRequired() {
        String token = jwtService.generateRestrictedToken("USR002", "FRONT_DESK");
        assertNotNull(token);
        assertTrue(jwtService.isTokenValid(token));
        assertEquals("USR002", jwtService.extractUserId(token));
        assertEquals("FRONT_DESK", jwtService.extractRole(token));
        assertEquals(JwtService.STATE_INITIAL_PASSWORD_CHANGE_REQUIRED, jwtService.extractState(token));
    }

    @Test
    void testExpiredTokenValidationFails() {
        JwtService shortLivedJwtService = new JwtService(SECRET, -1000, 2700000);
        String expiredToken = shortLivedJwtService.generateToken("USR001", "ADMIN");
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
