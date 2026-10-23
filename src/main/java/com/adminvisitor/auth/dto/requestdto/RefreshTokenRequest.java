package com.adminvisitor.auth.dto.requestdto;

import jakarta.validation.constraints.NotBlank;

/**
 * Data transfer object representing a request containing a raw refresh token string.
 *
 * @param refreshToken the plaintext refresh token string submitted for rotation or validation
 * @author Logeshwaran
 */
public record RefreshTokenRequest(
        @NotBlank
        String refreshToken
) {
}