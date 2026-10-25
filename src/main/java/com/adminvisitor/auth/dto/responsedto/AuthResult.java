package com.adminvisitor.auth.dto.responsedto;

import java.time.LocalDateTime;

/**
 * Internal carrier representing the result of a successful user authentication
 * or token refresh operation.
 *
 * @param accessToken the generated JWT access token string
 * @param refreshToken the raw refresh token string, or null if no refresh token was issued
 * @param user the user response profile
 * @param refreshTokenExpiresAt the refresh token's expiry timestamp, or null if no refresh token was issued
 */
public record AuthResult(
        String accessToken,
        String refreshToken,
        UserResponse user,
        LocalDateTime refreshTokenExpiresAt
) {
}