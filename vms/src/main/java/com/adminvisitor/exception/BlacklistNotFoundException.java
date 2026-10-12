package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class BlacklistNotFoundException extends VmsException {

    public BlacklistNotFoundException(String message) {
        super(ErrorCode.BLACKLIST_NOT_FOUND, message);
    }
}