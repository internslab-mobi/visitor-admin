package com.adminvisitor.auth.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO containing the user's credentials for authentication.
 *
 * @param email the user's registered email address
 * @param password the user's plaintext password
 * @author Logeshwaran
 */
@Schema(
        name = "LoginRequest",
        description = "Request payload containing user credentials for authentication"
)
public record LoginRequest(

        @Schema(
                description = "Registered email address of the user",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @Schema(
                description = "Password provided by the user for authentication",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Password is required")
        String password

) {
}