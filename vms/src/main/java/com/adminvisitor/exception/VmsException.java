package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public abstract class VmsException extends RuntimeException {

    private final ErrorCode errorCode;

    protected VmsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}

