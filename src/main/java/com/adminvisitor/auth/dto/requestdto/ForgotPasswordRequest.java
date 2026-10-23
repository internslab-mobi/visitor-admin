package com.adminvisitor.auth.dto.requestdto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object representing a request to initiate self-service password recovery.
 *
 * @param email the registered email address of the account for which password recovery is requested
 * @author Logeshwaran
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email
) {
}
