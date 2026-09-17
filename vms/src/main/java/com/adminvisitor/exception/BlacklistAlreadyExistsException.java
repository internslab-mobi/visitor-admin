package com.adminvisitor.exception;

public class BlacklistAlreadyExistsException extends RuntimeException {

    public BlacklistAlreadyExistsException(String message) {
        super(message);
    }
}