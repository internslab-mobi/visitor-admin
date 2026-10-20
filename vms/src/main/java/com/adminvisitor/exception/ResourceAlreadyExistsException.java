package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class ResourceAlreadyExistsException extends VmsException {

    public ResourceAlreadyExistsException(String message) {
        super(ErrorCode.DUPLICATE_RESOURCE, message);
    }
}
