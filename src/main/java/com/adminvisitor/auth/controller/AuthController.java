package com.adminvisitor.auth.controller;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.ForgotPasswordRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.requestdto.ResetPasswordRequest;
import com.adminvisitor.auth.dto.requestdto.VerifyOtpRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResponse;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.dto.responsedto.VerifyOtpResponse;
import com.adminvisitor.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String ACCESS_COOKIE = "ACCESS_TOKEN";
    private static final String REFRESH_COOKIE = "REFRESH_TOKEN";

    private final AuthService authService;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;
    private final boolean cookieSecure;

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

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
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

        if (result.mustChangePassword()) {
            response.header(
                    HttpHeaders.SET_COOKIE,
                    clearCookie(REFRESH_COOKIE, "/api/auth").toString()
            );

            return response.body(
                    AuthResponse.firstLoginRequired(
                            result.userId(),
                            result.role(),
                            result.user()
                    )
            );
        }

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

        return response.body(
                AuthResponse.authenticated(
                        result.userId(),
                        result.role(),
                        result.user()
                )
        );
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = authService.createUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

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
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, clearAccessCookie.toString())
                .body(Map.of(
                        "message",
                        "Password changed successfully. Please log in with your new password."
                ));
    }

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

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<VerifyOtpResponse> verifyResetOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        VerifyOtpResponse response = authService.verifyResetOtp(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);

        return ResponseEntity.ok(Map.of(
                "message",
                "Password reset successfully. Please log in with your new password."
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
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
                .body(AuthResponse.authenticated(
                        result.userId(),
                        result.role(),
                        result.user()
                ));
    }

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