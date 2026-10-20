package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

public class InvalidOtpException extends AuthException {

    public InvalidOtpException(String message) {
        super(AuthErrorCode.INVALID_OTP, message);
    }
}