package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

public class ResourceNotFoundException extends AuthException {

    public ResourceNotFoundException(String message) {
        super(AuthErrorCode.RESOURCE_NOT_FOUND, message);
    }
}