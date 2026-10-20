package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

public class ResourceAlreadyExistsException extends AuthException {

    public ResourceAlreadyExistsException(String message) {
        super(AuthErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }
}