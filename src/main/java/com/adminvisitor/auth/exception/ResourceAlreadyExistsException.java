package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

/**
 * Exception thrown when attempting to create a user account or resource that already exists in the system.
 *
 * @author Logeshwaran
 */
public class ResourceAlreadyExistsException extends AuthException {

    /**
     * Constructs a new resource already exists exception with the specified detail message.
     *
     * @param message the detail message describing the conflict
     */
    public ResourceAlreadyExistsException(String message) {
        super(AuthErrorCode.RESOURCE_ALREADY_EXISTS, message);
    }
}