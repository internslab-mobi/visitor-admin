package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

/**
 * Exception thrown when a user exceeds the allowable rate limit for requesting password-reset OTP codes.
 *
 * @author Logeshwaran
 */
public class OtpRateLimitException extends AuthException {

    /**
     * Constructs a new OTP rate limit exception with the specified detail message.
     *
     * @param message the detail message describing the rate limit violation
     */
    public OtpRateLimitException(String message) {
        super(AuthErrorCode.OTP_RATE_LIMIT_EXCEEDED, message);
    }
}