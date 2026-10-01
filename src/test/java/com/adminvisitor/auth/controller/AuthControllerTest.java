package com.adminvisitor.auth.controller;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.requestdto.RefreshTokenRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.enums.UserRole;
import com.adminvisitor.auth.exception.GlobalExceptionHandler;
import com.adminvisitor.auth.exception.InvalidTokenException;
import com.adminvisitor.auth.exception.PasswordValidationException;
import com.adminvisitor.auth.exception.ResourceAlreadyExistsException;
import com.adminvisitor.auth.exception.ResourceNotFoundException;
import com.adminvisitor.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    private AuthController authController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        authController = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testLoginSuccessNormalUser() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "Password123!");
        AuthResult response = new AuthResult(
                "accessToken123",
                "refreshToken123",
                false,
                UserRole.ADMIN,
                "USR001",
                "Authentication successful."
        );
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("accessToken123")))
                .andExpect(jsonPath("$.refreshToken", is("refreshToken123")))
                .andExpect(jsonPath("$.mustChangePassword", is(false)))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void testLoginFirstTimeUser() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "TempPass123!");
        AuthResult response = new AuthResult(
                "restrictedToken123",
                null,
                true,
                UserRole.FRONT_DESK,
                "USR002",
                "First login password change is required."
        );

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("restrictedToken123")))
                .andExpect(jsonPath("$.mustChangePassword", is(true)))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void testLoginBadCredentials() throws Exception {
        LoginRequest request = new LoginRequest("user@example.com", "WrongPassword");
        when(authService.login(any(LoginRequest.class))).thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    void testCreateUserSuccess() throws Exception {
        CreateUserRequest request = new CreateUserRequest("EMP002", UserRole.FRONT_DESK);
        UserResponse response = new UserResponse("USR002", "EMP002", "emp2@example.com", "John Doe", UserRole.FRONT_DESK, "ACTIVE", true);

        when(authService.createUser(any(CreateUserRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is("USR002")))
                .andExpect(jsonPath("$.employeeId", is("EMP002")))
                .andExpect(jsonPath("$.mustChangePassword", is(true)));
    }

    @Test
    void testCreateUserEmployeeNotFound() throws Exception {
        CreateUserRequest request = new CreateUserRequest("EMP999", UserRole.FRONT_DESK);
        when(authService.createUser(any(CreateUserRequest.class))).thenThrow(new ResourceNotFoundException("Employee not found with ID: EMP999"));

        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.message", containsString("Employee not found")));
    }

    @Test
    void testCreateUserAlreadyExistsConflict() throws Exception {
        CreateUserRequest request = new CreateUserRequest("EMP001", UserRole.ADMIN);
        when(authService.createUser(any(CreateUserRequest.class))).thenThrow(new ResourceAlreadyExistsException("Employee already has a user account: EMP001"));

        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.message", containsString("already has a user account")));
    }

    @Test
    void testChangePasswordSuccess() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("USR002", null));

        ChangePasswordRequest request = new ChangePasswordRequest("TempPass123!", "NewStrongPass123!");
        doNothing().when(authService).changePassword(eq("USR002"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", containsString("Password changed successfully")));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testChangePasswordValidationFailure() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("USR002", null));

        ChangePasswordRequest request = new ChangePasswordRequest("TempPass123!", "weak");
        doThrow(new PasswordValidationException("New password must be at least 8 characters long"))
                .when(authService).changePassword(eq("USR002"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", containsString("at least 8 characters")));

        SecurityContextHolder.clearContext();
    }

    @Test
    void testRefreshSuccess() throws Exception {
        AuthResult result = new AuthResult(
                "newAccessToken123",
                "newRefreshToken123",
                false,
                UserRole.ADMIN,
                "USR001",
                "Authentication successful."
        );

        when(authService.refresh("refreshToken123")).thenReturn(result);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("refreshToken123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", is("newAccessToken123")))
                .andExpect(jsonPath("$.refreshToken", is("newRefreshToken123")))
                .andExpect(jsonPath("$.mustChangePassword", is(false)));

        verify(authService).refresh("refreshToken123");
    }

    @Test
    void testRefreshRevokedTokenUnauthorized() throws Exception {
        when(authService.refresh("revokedRefreshToken123")).thenThrow(new InvalidTokenException("Refresh token has been revoked"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest("revokedRefreshToken123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Invalid or expired token")));
    }
}
