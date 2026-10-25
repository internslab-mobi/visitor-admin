package com.adminvisitor.auth.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for changing a user's password.
 *
 * <p>Used for first-login password changes and voluntary password changes
 * by authenticated users.</p>
 *
 * @param currentPassword the user's existing password
 * @param newPassword the new password selected by the user
 * @param confirmNewPassword confirmation of the new password
 */
@Schema(
        name = "ChangePasswordRequest",
        description = "Request payload for changing a user's password"
)
public record ChangePasswordRequest(

        @Schema(
                description = "The user's current password",
                example = "CurrentPassword123!",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(
                description = "The new password",
                example = "NewPassword456!",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "New password is required")
        String newPassword,

        @Schema(
                description = "Confirmation of the new password",
                example = "NewPassword456!",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Confirm new password is required")
        String confirmNewPassword

) {

        /**
         * Creates a request using the same value for the new password
         * and its confirmation.
         *
         * @param currentPassword the user's current password
         * @param newPassword the new password and its confirmation
         */
        public ChangePasswordRequest(String currentPassword, String newPassword) {
                this(currentPassword, newPassword, newPassword);
        }
}
