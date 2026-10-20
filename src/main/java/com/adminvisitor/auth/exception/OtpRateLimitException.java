package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

public class OtpRateLimitException extends AuthException {

    public OtpRateLimitException(String message) {
        super(AuthErrorCode.OTP_RATE_LIMIT_EXCEEDED, message);
    }
}