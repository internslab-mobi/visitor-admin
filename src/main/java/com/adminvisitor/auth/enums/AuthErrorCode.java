package com.adminvisitor.auth.enums;

import org.springframework.http.HttpStatus;

/**
 * Enumeration of application error codes defining machine-readable error identifiers,
 * mapped HTTP response statuses, and default user-facing diagnostic messages.
 *
 * @author Logeshwaran
 */
public enum AuthErrorCode {

    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Requested resource was not found."),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Resource already exists."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "Invalid request."),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Request validation failed."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Invalid email or password."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid or expired token."),
    INVALID_OTP(HttpStatus.BAD_REQUEST, "Invalid or expired OTP."),
    OTP_RATE_LIMIT_EXCEEDED(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too many OTP requests. Please try again later."
    ),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied."),
    EMAIL_DELIVERY_FAILED(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Unable to send email. Please try again later."
    ),
    INTERNAL_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred. Please try again later."
    );

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    /**
     * Constructs an error code constant associated with an HTTP status and default message.
     *
     * @param httpStatus the HTTP response status mapped to this error
     * @param defaultMessage the default human-readable message describing the error
     */
    AuthErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    /**
     * Retrieves the HTTP status associated with this error code.
     *
     * @return the {@link HttpStatus}
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /**
     * Retrieves the default human-readable error message.
     *
     * @return the default message string
     */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}