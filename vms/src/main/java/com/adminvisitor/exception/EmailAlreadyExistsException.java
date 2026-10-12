package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class EmailAlreadyExistsException extends VmsException {

    public EmailAlreadyExistsException(String message) {
        super(ErrorCode.EMAIL_ALREADY_EXISTS, message);
    }
}