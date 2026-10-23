package com.adminvisitor.auth.dto.requestdto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Data transfer object representing a request to verify a password-reset one-time password (OTP).
 *
 * @param email the registered email address of the account being recovered
 * @param otp the 6-digit numeric verification code received by the user
 * @author Logeshwaran
 */
public record VerifyOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Verification code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Verification code must be a 6-digit number")
        String otp
) {
}
