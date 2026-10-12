package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class BlacklistAlreadyRemovedException extends VmsException {

    public BlacklistAlreadyRemovedException(String message) {
        super(ErrorCode.BLACKLIST_ALREADY_REMOVED, message);
    }
}