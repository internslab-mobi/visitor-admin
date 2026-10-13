package com.adminvisitor.auth.dto.responsedto;

import com.adminvisitor.auth.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String accessToken,
        String refreshToken,
        boolean mustChangePassword,
        UserRole role,
        String userId,
        String message
) {

    public static AuthResponse firstLoginRequired(
            String accessToken,
            String userId,
            UserRole role
    ) {
        return new AuthResponse(
                accessToken,
                null,
                true,
                role,
                userId,
                "First login password change is required."
        );
    }

    public static AuthResponse authenticated(
            String accessToken,
            String refreshToken,
            String userId,
            UserRole role
    ) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                false,
                role,
                userId,
                "Authentication successful."
        );
    }
}