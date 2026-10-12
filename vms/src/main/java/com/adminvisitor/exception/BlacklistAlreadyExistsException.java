package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class BlacklistAlreadyExistsException extends VmsException {

    public BlacklistAlreadyExistsException(String message) {
        super(ErrorCode.BLACKLIST_ALREADY_EXISTS, message);
    }
}