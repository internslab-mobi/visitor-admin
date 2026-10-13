package com.adminvisitor.auth.controller;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.requestdto.RefreshTokenRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResponse;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResult authResult = authService.login(request);

        AuthResponse authResponse = authResult.mustChangePassword()
                ? AuthResponse.firstLoginRequired(
                authResult.accessToken(),
                authResult.userId(),
                authResult.role()
        )
                : AuthResponse.authenticated(
                authResult.accessToken(),
                authResult.refreshToken(),
                authResult.userId(),
                authResult.role()
        );

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        UserResponse response = authService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String userId = authentication.getName();

        authService.changePassword(userId, request);

        return ResponseEntity.ok(Map.of(
                "message", "Password changed successfully. Please log in with your new password."
        ));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        AuthResult authResult = authService.refresh(
                request.refreshToken()
        );

        AuthResponse authResponse = AuthResponse.authenticated(
                authResult.accessToken(),
                authResult.refreshToken(),
                authResult.userId(),
                authResult.role()
        );

        return ResponseEntity.ok(authResponse);
    }
}