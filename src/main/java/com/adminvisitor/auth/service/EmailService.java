package com.adminvisitor.auth.service;

import com.adminvisitor.auth.event.UserCreatedEvent;

/**
 * Defines the contract for sending application-related emails,
 * including user onboarding and password reset verification.
 *
 * @author Logeshwaran
 */
public interface EmailService {

    /**
     * Sends an onboarding email to a newly created user.
     *
     * @param data the event containing the user details required for the email
     */
    void sendOnboardingEmail(UserCreatedEvent data);

    /**
     * Sends a password reset OTP email to the specified recipient.
     *
     * @param recipientEmail the email address of the recipient
     * @param recipientName the name of the recipient
     * @param otp the one-time password used for verification
     * @param expirationMinutes the number of minutes until the OTP expires
     */
    void sendPasswordResetOtpEmail(
            String recipientEmail,
            String recipientName,
            String otp,
            int expirationMinutes
    );
}