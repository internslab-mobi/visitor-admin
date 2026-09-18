package com.adminvisitor.exception;

public class BadgeAlreadyExistsException extends RuntimeException {

    public BadgeAlreadyExistsException(String message) {
        super(message);
    }
}