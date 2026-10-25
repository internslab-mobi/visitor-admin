package com.adminvisitor.auth.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for setting a new password after successful OTP verification.
 *
 * @param resetToken the single-use reset authorization token issued after OTP verification
 * @param newPassword the new password selected by the user
 * @param confirmNewPassword confirmation of the new password
 * @author Logeshwaran
 */
@Schema(
        name = "ResetPasswordRequest",
        description = "Request payload for resetting a password after OTP verification"
)
public record ResetPasswordRequest(

        @Schema(
                description = "Single-use reset authorization token issued after OTP verification",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Reset token is required")
        String resetToken,

        @Schema(
                description = "New password selected by the user",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "New password is required")
        String newPassword,

        @Schema(
                description = "Confirmation of the new password",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Confirm new password is required")
        String confirmNewPassword

) {
}