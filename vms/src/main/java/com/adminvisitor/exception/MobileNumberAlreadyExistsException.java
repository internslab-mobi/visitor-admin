package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class MobileNumberAlreadyExistsException extends VmsException {

    public MobileNumberAlreadyExistsException(String message) {
        super(ErrorCode.MOBILE_NUMBER_ALREADY_EXISTS, message);
    }
}
