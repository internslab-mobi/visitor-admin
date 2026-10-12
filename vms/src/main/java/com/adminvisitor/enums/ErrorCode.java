package com.adminvisitor.enums;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VISITOR_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "The requested visitor could not be found."
    ),

    RESOURCE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "The requested resource could not be found."
    ),

    BLACKLIST_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "The requested blacklist entry could not be found."
    ),

    EMAIL_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "A visitor with this email already exists."
    ),

    MOBILE_NUMBER_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "A visitor with this mobile number already exists."
    ),

    BLACKLIST_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "This visitor is already on the blacklist."
    ),

    BLACKLIST_ALREADY_REMOVED(
            HttpStatus.CONFLICT,
            "This blacklist entry has already been removed."
    ),

    BADGE_ALREADY_EXISTS(
            HttpStatus.CONFLICT,
            "A badge already exists for this visit."
    ),

    BADGE_NOT_FOUND(
            HttpStatus.NOT_FOUND,
            "The requested badge could not be found."
    ),

    VALIDATION_FAILED(
            HttpStatus.BAD_REQUEST,
            "Please correct the invalid fields."
    ),

    INVALID_REQUEST_BODY(
            HttpStatus.BAD_REQUEST,
            "The request body is invalid."
    ),

    BUSINESS_RULE_VIOLATION(
            HttpStatus.BAD_REQUEST,
            "The request cannot be completed because of a business rule."
    ),

    INTERNAL_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred."
    ),

    UNAUTHORIZED(
            HttpStatus.UNAUTHORIZED,
        "Authentication is required or is invalid."
    ),

    FORBIDDEN(
            HttpStatus.FORBIDDEN,
        "You do not have permission to access this resource."
    );

    private final HttpStatus httpStatus;
    private final String publicMessage;

    ErrorCode(HttpStatus httpStatus, String publicMessage) {
        this.httpStatus = httpStatus;
        this.publicMessage = publicMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getPublicMessage() {
        return publicMessage;
    }
}

