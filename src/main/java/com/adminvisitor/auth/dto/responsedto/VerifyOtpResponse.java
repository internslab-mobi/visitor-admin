package com.adminvisitor.auth.dto.responsedto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO returned after successful password-reset OTP verification.
 *
 * @param resetToken the single-use authorization token required to submit the password-reset request
 * @param message the status message confirming successful OTP verification
 * @author Logeshwaran
 */
@Schema(
        name = "VerifyOtpResponse",
        description = "Response returned after successful password-reset OTP verification"
)
public record VerifyOtpResponse(

        @Schema(
                description = "Single-use authorization token required to complete the password reset",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String resetToken,

        @Schema(
                description = "Status message confirming successful OTP verification",
                example = "OTP verified successfully",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message

) {
}