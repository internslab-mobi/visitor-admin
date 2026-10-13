package com.adminvisitor.auth.dto.requestdto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank
        String refreshToken
) {
}