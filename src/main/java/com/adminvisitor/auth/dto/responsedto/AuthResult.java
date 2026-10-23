package com.adminvisitor.auth.dto.responsedto;

/**
 * Internal carrier representing the result of a successful user authentication or token refresh operation.
 * <p>
 * Contains the issued JWT access token, raw refresh token string (if applicable), and user profile details.
 * </p>
 *
 * @param accessToken the generated JWT access token string (standard or restricted)
 * @param refreshToken the raw refresh token string, or {@code null} if refresh token issuance was omitted
 * @param user the user response profile containing user identity, role, and password change status
 * @author Logeshwaran
 */
public record AuthResult(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}

