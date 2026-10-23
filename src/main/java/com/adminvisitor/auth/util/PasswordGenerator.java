package com.adminvisitor.auth.util;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utility for generating cryptographically secure temporary passwords.
 * <p>
 * Guarantees that generated passwords contain at least one uppercase letter, one lowercase letter,
 * one numeric digit, and one special character, shuffled with {@link SecureRandom} to prevent predictable character positioning.
 * </p>
 *
 * @author Logeshwaran
 */
public class PasswordGenerator {

    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_CHARACTERS = LOWERCASE + UPPERCASE + DIGITS + SPECIAL;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int DEFAULT_LENGTH = 12;

    /**
     * Generates a randomized temporary password of default length (12 characters).
     *
     * @return the generated temporary password meeting complexity criteria
     */
    public static String generateTemporaryPassword() {
        return generateTemporaryPassword(DEFAULT_LENGTH);
    }

    /**
     * Generates a randomized temporary password of the specified length.
     * <p>
     * If the specified length is less than 8, a minimum length of 8 is enforced.
     * </p>
     *
     * @param length the desired length of the temporary password (minimum 8)
     * @return the generated temporary password meeting complexity criteria
     */
    public static String generateTemporaryPassword(int length) {
        if (length < 8) {
            length = 8;
        }

        List<Character> passwordChars = new ArrayList<>();

        passwordChars.add(LOWERCASE.charAt(RANDOM.nextInt(LOWERCASE.length())));
        passwordChars.add(UPPERCASE.charAt(RANDOM.nextInt(UPPERCASE.length())));
        passwordChars.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        passwordChars.add(SPECIAL.charAt(RANDOM.nextInt(SPECIAL.length())));

        for (int i = 4; i < length; i++) {
            passwordChars.add(ALL_CHARACTERS.charAt(RANDOM.nextInt(ALL_CHARACTERS.length())));
        }

        Collections.shuffle(passwordChars, RANDOM);

        StringBuilder password = new StringBuilder(length);
        for (char c : passwordChars) {
            password.append(c);
        }

        return password.toString();
    }
}
