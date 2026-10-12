package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class BadgeAlreadyExistsException extends VmsException {

    public BadgeAlreadyExistsException(String message) {
        super(ErrorCode.BADGE_ALREADY_EXISTS, message);
    }
}