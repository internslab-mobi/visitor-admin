package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

public class InvalidTokenException extends AuthException {

    public InvalidTokenException(String message) {
        super(AuthErrorCode.INVALID_TOKEN, message);
    }
}