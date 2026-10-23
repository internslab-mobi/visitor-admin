package com.adminvisitor.auth.exception;

import com.adminvisitor.auth.enums.AuthErrorCode;

/**
 * Exception thrown when an email dispatch failure occurs during user onboarding or password reset workflows.
 *
 * @author Logeshwaran
 */
public class EmailSendException extends AuthException {

    /**
     * Constructs a new email send exception with the specified detail message.
     *
     * @param message the detail message describing the failure
     */
    public EmailSendException(String message) {
        super(AuthErrorCode.EMAIL_DELIVERY_FAILED, message);
    }

    /**
     * Constructs a new email send exception with the specified detail message and root cause.
     *
     * @param message the detail message describing the failure
     * @param cause the underlying cause, such as a mail transport exception
     */
    public EmailSendException(String message, Throwable cause) {
        super(AuthErrorCode.EMAIL_DELIVERY_FAILED, message, cause);
    }
}