package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

/**
 * Exception thrown when a requested entity (such as user, employee, or token) is not found in persistence.
 *
 * @author Logeshwaran
 */
public class ResourceNotFoundException extends AuthException {

    /**
     * Constructs a new resource not found exception with the specified detail message.
     *
     * @param message the detail message describing the missing entity
     */
    public ResourceNotFoundException(String message) {
        super(AuthErrorCode.RESOURCE_NOT_FOUND, message);
    }
}