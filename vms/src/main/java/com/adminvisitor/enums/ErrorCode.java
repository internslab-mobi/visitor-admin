package com.adminvisitor.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "Please correct the invalid fields."
    ),
    INVALID_REQUEST(
            HttpStatus.BAD_REQUEST,
            "The request is invalid."
    ),
    BUSINESS_RULE_VIOLATION(
            HttpStatus.BAD_REQUEST,
            "The request cannot be completed because of a business rule."
    ),
    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
            "Authentication is required or is invalid."
    ),
    FORBIDDEN(
            HttpStatus.FORBIDDEN,
            "You do not have permission to access this resource."
    ),
    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "The requested resource could not be found."
    ),
    DUPLICATE_RESOURCE(
            HttpStatus.CONFLICT,
            "The request conflicts with existing data."
    ),
    BLACKLIST_ALREADY_REMOVED(
            HttpStatus.CONFLICT,
            "This blacklist entry has already been removed."
    ),
    INTERNAL_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred."
    );

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
