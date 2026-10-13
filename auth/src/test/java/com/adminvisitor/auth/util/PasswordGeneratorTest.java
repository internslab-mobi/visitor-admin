package com.adminvisitor.auth.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {

    @Test
    void testGenerateTemporaryPasswordComplexity() {
        String password = PasswordGenerator.generateTemporaryPassword(12);
        assertNotNull(password);
        assertEquals(12, password.length());

        assertTrue(password.chars().anyMatch(Character::isLowerCase), "Password must contain lowercase character");
        assertTrue(password.chars().anyMatch(Character::isUpperCase), "Password must contain uppercase character");
        assertTrue(password.chars().anyMatch(Character::isDigit), "Password must contain digit");
        assertTrue(password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0), "Password must contain special character");
    }

    @Test
    void testUniqueness() {
        String p1 = PasswordGenerator.generateTemporaryPassword(12);
        String p2 = PasswordGenerator.generateTemporaryPassword(12);
        assertNotEquals(p1, p2);
    }
}
