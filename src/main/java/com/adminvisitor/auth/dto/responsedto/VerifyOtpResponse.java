package com.adminvisitor.auth.dto.responsedto;

/**
 * Data transfer object representing the response returned after successful password-reset OTP verification.
 *
 * @param resetToken single-use authorization token required to submit the subsequent password reset request
 * @param message informative status message confirming verification
 * @author Logeshwaran
 */
public record VerifyOtpResponse(
        String resetToken,
        String message
) {
}
