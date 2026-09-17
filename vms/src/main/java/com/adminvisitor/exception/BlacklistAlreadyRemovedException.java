package com.adminvisitor.exception;

public class BlacklistAlreadyRemovedException extends RuntimeException {

    public BlacklistAlreadyRemovedException(String message) {
        super(message);
    }
}