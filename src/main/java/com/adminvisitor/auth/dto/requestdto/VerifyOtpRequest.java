package com.adminvisitor.auth.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for verifying a password-reset one-time password (OTP).
 *
 * @param email the registered email address of the account being recovered
 * @param otp the six-digit numeric verification code received by the user
 * @author Logeshwaran
 */
@Schema(
        name = "VerifyOtpRequest",
        description = "Request payload for verifying a password-reset OTP"
)
public record VerifyOtpRequest(

        @Schema(
                description = "Registered email address associated with the account being recovered",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(
                description = "Six-digit numeric verification code received by the user",
                example = "123456",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Verification code is required")
        @Pattern(
                regexp = "^\\d{6}$",
                message = "Verification code must be a 6-digit number"
        )
        String otp

) {
}