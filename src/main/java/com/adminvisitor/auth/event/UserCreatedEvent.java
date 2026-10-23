package com.adminvisitor.auth.event;

/**
 * Domain event published when a new user account has been successfully provisioned.
 * <p>
 * Consumed by asynchronous event listeners to trigger onboarding email delivery containing the initial credentials.
 * </p>
 *
 * @param recipientEmail the email address of the newly provisioned user
 * @param recipientName the full name of the user
 * @param temporaryPassword the generated temporary plaintext password
 * @author Logeshwaran
 */
public record UserCreatedEvent(
        String recipientEmail,
        String recipientName,
        String temporaryPassword
) {
}