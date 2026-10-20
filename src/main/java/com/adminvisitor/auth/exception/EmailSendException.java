package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

public class EmailSendException extends AuthException {

    public EmailSendException(String message) {
        super(AuthErrorCode.EMAIL_DELIVERY_FAILED, message);
    }

    public EmailSendException(String message, Throwable cause) {
        super(AuthErrorCode.EMAIL_DELIVERY_FAILED, message, cause);
    }
}