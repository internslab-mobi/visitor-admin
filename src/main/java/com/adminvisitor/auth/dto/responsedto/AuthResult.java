package com.adminvisitor.auth.dto.responsedto;

public record AuthResult(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}

