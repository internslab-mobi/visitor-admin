package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class VisitorNotFoundException extends VmsException {

    public VisitorNotFoundException(String message) {
        super(ErrorCode.VISITOR_NOT_FOUND, message);
    }
}

