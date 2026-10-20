package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

public class PasswordValidationException extends AuthException {

    public PasswordValidationException(String message) {
        super(AuthErrorCode.INVALID_REQUEST, message);
    }
}