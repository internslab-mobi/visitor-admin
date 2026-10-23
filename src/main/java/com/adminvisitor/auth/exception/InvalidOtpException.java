package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

/**
 * Exception thrown when a submitted one-time password (OTP) is invalid, expired, or failed verification.
 *
 * @author Logeshwaran
 */
public class InvalidOtpException extends AuthException {

    /**
     * Constructs a new invalid OTP exception with the specified detail message.
     *
     * @param message the detail message describing the verification failure
     */
    public InvalidOtpException(String message) {
        super(AuthErrorCode.INVALID_OTP, message);
    }
}