package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

/**
 * Abstract base runtime exception for domain-level authentication and authorization errors.
 * <p>
 * Associates an application-specific {@link AuthErrorCode} with the exception to ensure consistent
 * HTTP status mapping and machine-readable error reporting.
 * </p>
 *
 * @author Logeshwaran
 */
public abstract class AuthException extends RuntimeException {

    private final AuthErrorCode errorCode;

    /**
     * Constructs a new authentication exception with the specified error code and detail message.
     *
     * @param errorCode the domain error code classifying this failure
     * @param message the descriptive detail message
     */
    protected AuthException(AuthErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new authentication exception with the specified error code, detail message, and cause.
     *
     * @param errorCode the domain error code classifying this failure
     * @param message the descriptive detail message
     * @param cause the underlying root cause
     */
    protected AuthException(
            AuthErrorCode errorCode,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Retrieves the application error code associated with this exception.
     *
     * @return the {@link AuthErrorCode}
     */
    public AuthErrorCode getErrorCode() {
        return errorCode;
    }
}