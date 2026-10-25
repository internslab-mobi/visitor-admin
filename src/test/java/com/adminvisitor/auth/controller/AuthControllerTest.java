package com.adminvisitor.auth.controller;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.ErrorResponseFactory;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.enums.UserRole;
import com.adminvisitor.auth.exception.*;
import com.adminvisitor.auth.service.AuthService;
import com.adminvisitor.auth.util.TraceIdProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
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
        authController = new AuthController(
                authService,
                900000L,
                28800000L,
                false
        );

        TraceIdProvider traceIdProvider = mock(TraceIdProvider.class);

        ErrorResponseFactory errorResponseFactory =
                new ErrorResponseFactory(traceIdProvider);

        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(
                        new GlobalExceptionHandler(errorResponseFactory)
                )
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testLoginSuccessNormalUser() throws Exception {
        LoginRequest request =
                new LoginRequest("user@example.com", "Password123!");

        UserResponse userResponse = new UserResponse(
                "USR001",
                "EMP001",
                "user@example.com",
                "John Doe",
                UserRole.ADMIN,
                "ACTIVE",
                false
        );

        AuthResult response = new AuthResult(
                "accessToken123",
                "refreshToken123",
                userResponse,
                LocalDateTime.now().plusHours(8)
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("USR001")))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.mustChangePassword", is(false)))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void testLoginFirstTimeUser() throws Exception {
        LoginRequest request = new LoginRequest(
                "user@example.com",
                "TempPass123!"
        );

        UserResponse userResponse = new UserResponse(
                "USR002",
                "EMP002",
                "user@example.com",
                "Jane Doe",
                UserRole.HR,
                "ACTIVE",
                true
        );

        AuthResult response = new AuthResult(
                "restrictedToken123",
                null,
                userResponse,
                null
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("USR002")))
                .andExpect(jsonPath("$.role", is("HR")))
                .andExpect(jsonPath("$.mustChangePassword", is(true)))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void testLoginBadCredentials() throws Exception {
        LoginRequest request =
                new LoginRequest("user@example.com", "WrongPassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(
                        new BadCredentialsException(
                                "Invalid email or password"
                        )
                );

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath(
                        "$.message",
                        is("Invalid email or password.")
                ))
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")));
    }

    @Test
    void testCreateUserSuccess() throws Exception {
        CreateUserRequest request =
                new CreateUserRequest("EMP002", UserRole.HR);

        UserResponse response = new UserResponse(
                "USR002",
                "EMP002",
                "emp2@example.com",
                "John Doe",
                UserRole.HR,
                "ACTIVE",
                true
        );

        when(authService.createUser(any(CreateUserRequest.class)))
                .thenReturn(response);

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
        CreateUserRequest request =
                new CreateUserRequest("EMP999", UserRole.HR);

        when(authService.createUser(any(CreateUserRequest.class)))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Employee not found with ID: EMP999"
                        )
                );

        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath(
                        "$.message",
                        is("Requested resource was not found.")
                ))
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void testCreateUserAlreadyExistsConflict() throws Exception {
        CreateUserRequest request =
                new CreateUserRequest("EMP001", UserRole.ADMIN);

        when(authService.createUser(any(CreateUserRequest.class)))
                .thenThrow(
                        new ResourceAlreadyExistsException(
                                "Employee already has a user account: EMP001"
                        )
                );

        mockMvc.perform(post("/api/auth/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath(
                        "$.message",
                        is("Resource already exists.")
                ))
                .andExpect(jsonPath("$.code", is("RESOURCE_ALREADY_EXISTS")));
    }

    @Test
    void testChangePasswordSuccess() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("USR002", null)
        );

        ChangePasswordRequest request = new ChangePasswordRequest(
                "TempPass123!",
                "NewStrongPass123!"
        );

        doNothing().when(authService)
                .changePassword(eq("USR002"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.message",
                        containsString("Password changed successfully")
                ));

        verify(authService).changePassword(
                eq("USR002"),
                any(ChangePasswordRequest.class)
        );
    }

    @Test
    void testChangePasswordValidationFailure() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("USR002", null)
        );

        ChangePasswordRequest request = new ChangePasswordRequest(
                "TempPass123!",
                "weak"
        );

        doThrow(
                new PasswordValidationException(
                        "New password must be at least 8 characters long"
                )
        ).when(authService)
                .changePassword(eq("USR002"), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath(
                        "$.message",
                        is("Invalid request.")
                ))
                .andExpect(jsonPath("$.code", is("INVALID_REQUEST")));
    }

    @Test
    void testRefreshSuccess() throws Exception {
        UserResponse userResponse = new UserResponse(
                "USR001",
                "EMP001",
                "user@example.com",
                "John Doe",
                UserRole.ADMIN,
                "ACTIVE",
                false
        );

        AuthResult authResult = new AuthResult(
                "newAccessToken123",
                "newRefreshToken123",
                userResponse,
                LocalDateTime.now().plusHours(2)
        );

        when(authService.refresh("refreshToken123"))
                .thenReturn(authResult);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(
                                "REFRESH_TOKEN",
                                "refreshToken123"
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is("USR001")))
                .andExpect(jsonPath("$.role", is("ADMIN")))
                .andExpect(jsonPath("$.mustChangePassword", is(false)))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        containsString("Max-Age=")
                ));

        verify(authService).refresh("refreshToken123");
    }

    @Test
    void testRefreshRevokedTokenUnauthorized() throws Exception {
        when(authService.refresh("revokedRefreshToken123"))
                .thenThrow(
                        new InvalidTokenException(
                                "Refresh token has been revoked"
                        )
                );

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(
                                "REFRESH_TOKEN",
                                "revokedRefreshToken123"
                        )))
                .andExpect(status().isUnauthorized());

        verify(authService).refresh("revokedRefreshToken123");
    }
}