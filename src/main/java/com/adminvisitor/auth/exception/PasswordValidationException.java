package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

/**
 * Exception thrown when a proposed password fails policy validation rules or confirmation checks.
 *
 * @author Logeshwaran
 */
public class PasswordValidationException extends AuthException {

    /**
     * Constructs a new password validation exception with the specified detail message.
     *
     * @param message the detail message describing the validation failure
     */
    public PasswordValidationException(String message) {
        super(AuthErrorCode.INVALID_REQUEST, message);
    }
}