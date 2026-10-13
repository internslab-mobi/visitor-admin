package com.adminvisitor.auth.dto.responsedto;

import com.adminvisitor.auth.enums.UserRole;

public record AuthResult(
        String accessToken,
        String refreshToken,
        boolean mustChangePassword,
        UserRole role,
        String userId,
        String message
) {
}