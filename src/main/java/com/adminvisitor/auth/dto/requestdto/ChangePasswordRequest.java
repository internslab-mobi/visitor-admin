package com.adminvisitor.auth.dto.requestdto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object representing a request to change an authenticated user's password.
 * <p>
 * Used during first-login forced password reset or subsequent voluntary password change operations.
 * </p>
 *
 * @param currentPassword the user's existing plaintext password
 * @param newPassword the desired new plaintext password meeting complexity criteria
 * @param confirmNewPassword confirmation of the new plaintext password
 * @author Logeshwaran
 */
public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        String newPassword,

        @NotBlank(message = "Confirm new password is required")
        String confirmNewPassword
) {
        /**
         * Convenience constructor initializing the request when newPassword and confirmNewPassword are identical.
         *
         * @param currentPassword the user's existing plaintext password
         * @param newPassword the desired new plaintext password
         */
        public ChangePasswordRequest(String currentPassword, String newPassword) {
                this(currentPassword, newPassword, newPassword);
        }
}
