package com.adminvisitor.auth.dto.requestdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO containing a refresh token submitted for validation or rotation.
 *
 * @param refreshToken the raw refresh token submitted by the client
 * @author Logeshwaran
 */
@Schema(
        name = "RefreshTokenRequest",
        description = "Request payload containing a refresh token"
)
public record RefreshTokenRequest(

        @Schema(
                description = "Refresh token submitted for validation or rotation",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Refresh token is required")
        String refreshToken

) {
}