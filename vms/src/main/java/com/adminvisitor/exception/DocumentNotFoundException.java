package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class DocumentNotFoundException extends VmsException {

    public DocumentNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }
}

