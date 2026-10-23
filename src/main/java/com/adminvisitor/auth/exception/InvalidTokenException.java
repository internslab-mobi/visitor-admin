package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

/**
 * Exception thrown when an access token, refresh token, or password-reset token is invalid, expired, or revoked.
 *
 * @author Logeshwaran
 */
public class InvalidTokenException extends AuthException {

    /**
     * Constructs a new invalid token exception with the specified detail message.
     *
     * @param message the detail message describing the token failure
     */
    public InvalidTokenException(String message) {
        super(AuthErrorCode.INVALID_TOKEN, message);
    }
}