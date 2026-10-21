package com.adminvisitor.exception;

import com.adminvisitor.enums.ErrorCode;

public class BusinessRuleException extends VmsException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE_VIOLATION, message);
    }
}