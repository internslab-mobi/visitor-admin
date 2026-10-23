package com.adminvisitor.auth.controller;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.ForgotPasswordRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.requestdto.ResetPasswordRequest;
import com.adminvisitor.auth.dto.requestdto.VerifyOtpRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.dto.responsedto.VerifyOtpResponse;
import com.adminvisitor.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

/**
 * REST controller for authentication and user account operations.
 *
 * @author Logeshwaran
 */
@Tag(
        name = "Authentication",
        description = "APIs for authentication, user management, and password recovery"
)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACCESS_COOKIE = "ACCESS_TOKEN";
    private static final String REFRESH_COOKIE = "REFRESH_TOKEN";

    private final AuthService authService;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final boolean cookieSecure;

    /**
     * Creates the authentication controller with its required dependencies and configuration.
     *
     * @param authService the authentication service
     * @param accessTokenExpiration the access token lifetime in milliseconds
     * @param refreshTokenExpiration the refresh token lifetime in milliseconds
     * @param cookieSecure whether authentication cookies require HTTPS
     */
    public AuthController(
            AuthService authService,
            @Value("${jwt.expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-expiration}") long refreshTokenExpiration,
            @Value("${auth.cookie.secure}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.cookieSecure = cookieSecure;
    }

    /**
     * Authenticates a user and sets the appropriate authentication cookies.
     *
     * @param request the login credentials
     * @return the authenticated user's details
     */
    @Operation(
            summary = "Login",
            description = "Authenticates a user and sets HTTP-only authentication cookies. Users required to change their password must complete that step before normal access."
    )
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResult result = authService.login(request);

        ResponseCookie accessCookie = createCookie(
                ACCESS_COOKIE,
                result.accessToken(),
                "/api",
                accessTokenExpiration
        );

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString());

        if (result.user().mustChangePassword()) {
            response.header(
                    HttpHeaders.SET_COOKIE,
                    clearCookie(REFRESH_COOKIE, "/api/auth").toString()
            );
        } else {
            ResponseCookie refreshCookie = createCookie(
                    REFRESH_COOKIE,
                    result.refreshToken(),
                    "/api/auth",
                    refreshTokenExpiration
            );

            response.header(
                    HttpHeaders.SET_COOKIE,
                    refreshCookie.toString()
            );
        }

        return response.body(result.user());
    }

    /**
     * Creates a user account.
     *
     * @param request the user creation details
     * @return the created user's details
     */
    @Operation(
            summary = "Create user",
            description = "Creates a new user account. Only administrators are authorized to perform this operation."
    )
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = authService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Changes the password of the currently authenticated user.
     *
     * @param request the password change details
     * @return a confirmation message
     */
    @Operation(
            summary = "Change password",
            description = "Changes the password of the currently authenticated user."
    )

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String userId = authentication.getName();

        authService.changePassword(userId, request);

        ResponseCookie clearAccessCookie =
                clearCookie(ACCESS_COOKIE, "/api");

        ResponseCookie clearRefreshCookie =
                clearCookie(REFRESH_COOKIE, "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body(Map.of(
                        "message",
                        "Password changed successfully. Please log in with your new password."
                ));
    }


    /**
     * Initiates password recovery for the supplied email address.
     *
     * @param request the password recovery request
     * @return the result message
     */
    @Operation(
            summary = "Request password reset",
            description = "Initiates password recovery and sends a verification code when the account is eligible."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        boolean accepted = authService.forgotPassword(request);

        if (!accepted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "No active account found for this email. Please check your email address or contact your administrator."
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "message",
                "Verification code sent. Please check your inbox and spam folder."
        ));
    }

    /**
     * Requests another password recovery verification code.
     *
     * @param request the password recovery request
     * @return the result message
     */
    @Operation(
            summary = "Resend password reset OTP",
            description = "Requests another password recovery verification code."
    )
    @PostMapping("/forgot-password/resend")
    public ResponseEntity<Map<String, String>> resendForgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        boolean accepted = authService.forgotPassword(request);

        if (!accepted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "No active account found for this email. Please check your email address or contact your administrator."
                    ));
        }

        return ResponseEntity.ok(Map.of(
                "message",
                "Verification code sent. Please check your inbox and spam folder."
        ));
    }

    /**
     * Verifies the password reset one-time password.
     *
     * @param request the OTP verification details
     * @return the verification result
     */
    @Operation(
            summary = "Verify password reset OTP",
            description = "Verifies the one-time password submitted during password recovery."
    )
    @PostMapping("/verify-reset-otp")
    public ResponseEntity<VerifyOtpResponse> verifyResetOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        VerifyOtpResponse response = authService.verifyResetOtp(request);

        return ResponseEntity.ok(response);
    }

    /**
     * Resets a user's password.
     *
     * @param request the password reset details
     * @return a confirmation message
     */
    @Operation(
            summary = "Reset password",
            description = "Resets the user's password using the supplied reset details."
    )

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);

        ResponseCookie clearAccessCookie =
                clearCookie(ACCESS_COOKIE, "/api");

        ResponseCookie clearRefreshCookie =
                clearCookie(REFRESH_COOKIE, "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body(Map.of(
                        "message",
                        "Password reset successfully. Please log in with your new password."
                ));
    }


    /**
     * Refreshes the authentication tokens using the refresh token cookie.
     *
     * @param refreshToken the refresh token from the cookie
     * @return the user's details with refreshed authentication cookies
     */
    @Operation(
            summary = "Refresh authentication tokens",
            description = "Uses the refresh token cookie to issue new authentication cookies."
    )
    @PostMapping("/refresh")
    public ResponseEntity<UserResponse> refresh(
            @CookieValue(
                    value = REFRESH_COOKIE,
                    required = false
            ) String refreshToken
    ) {
        AuthResult result = authService.refresh(refreshToken);

        ResponseCookie accessCookie = createCookie(
                ACCESS_COOKIE,
                result.accessToken(),
                "/api",
                accessTokenExpiration
        );

        ResponseCookie refreshCookie = createCookie(
                REFRESH_COOKIE,
                result.refreshToken(),
                "/api/auth",
                refreshTokenExpiration
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(result.user());
    }

    /**
     * Logs out the user and clears the authentication cookies.
     *
     * @param refreshToken the refresh token from the cookie, if present
     * @return a confirmation message
     */
    @Operation(
            summary = "Logout",
            description = "Logs out the user and clears the authentication cookies."
    )
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @CookieValue(
                    value = REFRESH_COOKIE,
                    required = false
            ) String refreshToken
    ) {
        authService.logout(refreshToken);

        ResponseCookie clearAccessCookie =
                clearCookie(ACCESS_COOKIE, "/api");

        ResponseCookie clearRefreshCookie =
                clearCookie(REFRESH_COOKIE, "/api/auth");

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie.toString())
                .body(Map.of(
                        "message",
                        "Logged out successfully"
                ));
    }

    /**
     * Creates an HTTP-only authentication cookie.
     *
     * @param name the cookie name
     * @param value the cookie value
     * @param path the cookie path
     * @param maxAgeMillis the cookie lifetime in milliseconds
     * @return the configured cookie
     */
    private ResponseCookie createCookie(
            String name,
            String value,
            String path,
            long maxAgeMillis
    ) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(path)
                .maxAge(Duration.ofMillis(maxAgeMillis))
                .build();
    }

    /**
     * Creates an expired authentication cookie to clear it from the client.
     *
     * @param name the cookie name
     * @param path the cookie path
     * @return the expired cookie
     */
    private ResponseCookie clearCookie(
            String name,
            String path
    ) {
        return ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(path)
                .maxAge(Duration.ZERO)
                .build();
    }
}