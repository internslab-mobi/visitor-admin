package com.adminvisitor.auth.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for initiating self-service password recovery.
 *
 * @param email the registered email address of the account for which password recovery is requested
 * @author Logeshwaran
 */
@Schema(
        name = "ForgotPasswordRequest",
        description = "Request payload for initiating password recovery"
)
public record ForgotPasswordRequest(

        @Schema(
                description = "Registered email address associated with the account",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email

) {
}