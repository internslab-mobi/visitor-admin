package com.adminvisitor.auth.dto.requestdto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object representing a request to set a new password following OTP verification.
 *
 * @param resetToken the single-use reset authorization token issued upon OTP verification
 * @param newPassword the desired new plaintext password meeting complexity criteria
 * @param confirmNewPassword confirmation of the new plaintext password
 * @author Logeshwaran
 */
public record ResetPasswordRequest(
        @NotBlank(message = "Reset token is required")
        String resetToken,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Confirm new password is required")
        String confirmNewPassword
) {
}
