package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class ResourceNotFoundException extends VmsException {

    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}