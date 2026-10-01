package com.adminvisitor.auth.service;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.entity.Employee;
import com.adminvisitor.auth.entity.RefreshToken;
import com.adminvisitor.auth.entity.User;
import com.adminvisitor.auth.enums.UserRole;
import com.adminvisitor.auth.event.UserCreatedEvent;
import com.adminvisitor.auth.exception.EmailSendException;
import com.adminvisitor.auth.exception.InvalidTokenException;
import com.adminvisitor.auth.exception.PasswordValidationException;
import com.adminvisitor.auth.exception.ResourceAlreadyExistsException;
import com.adminvisitor.auth.exception.ResourceNotFoundException;
import com.adminvisitor.auth.repository.EmployeeRepository;
import com.adminvisitor.auth.repository.RefreshTokenRepository;
import com.adminvisitor.auth.repository.UserRepository;
import com.adminvisitor.auth.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private Employee employee;
    private User user;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId("EMP002");
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe@example.com");
        employee.setStatus("ACTIVE");

        user = new User();
        user.setId("USR002");
        user.setEmployee(employee);
        user.setPasswordHash("hashedPassword123");
        user.setRole(UserRole.FRONT_DESK);
        user.setStatus("ACTIVE");
        user.setMustChangePassword(true);
    }

    @Test
    void testCreateUserSuccess() {
        CreateUserRequest request = new CreateUserRequest("EMP002", UserRole.FRONT_DESK);

        when(employeeRepository.findById("EMP002")).thenReturn(Optional.of(employee));
        when(userRepository.findByEmployeeEmail("john.doe@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedTempPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.createUser(request);

        assertNotNull(response);
        assertEquals("EMP002", response.employeeId());
        assertEquals("john.doe@example.com", response.email());
        assertTrue(response.mustChangePassword());
        verify(emailService, times(1))
                .sendOnboardingEmail(any(UserCreatedEvent.class));    }

    @Test
    void testCreateUserEmployeeNotFound() {
        CreateUserRequest request = new CreateUserRequest("EMP999", UserRole.FRONT_DESK);
        when(employeeRepository.findById("EMP999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.createUser(request));
    }

    @Test
    void testCreateUserAlreadyExists() {
        CreateUserRequest request = new CreateUserRequest("EMP002", UserRole.FRONT_DESK);
        when(employeeRepository.findById("EMP002")).thenReturn(Optional.of(employee));
        when(userRepository.findByEmployeeEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        assertThrows(ResourceAlreadyExistsException.class, () -> authService.createUser(request));
    }

    @Test
    void testCreateUserInactiveEmployeeThrowsException() {
        employee.setStatus("INACTIVE");
        CreateUserRequest request = new CreateUserRequest("EMP002", UserRole.FRONT_DESK);
        when(employeeRepository.findById("EMP002")).thenReturn(Optional.of(employee));

        assertThrows(IllegalArgumentException.class, () -> authService.createUser(request));
    }

    @Test
    void testCreateUserEmailFailThrowsException() {
        CreateUserRequest request = new CreateUserRequest("EMP002", UserRole.FRONT_DESK);
        when(employeeRepository.findById("EMP002")).thenReturn(Optional.of(employee));
        when(userRepository.findByEmployeeEmail("john.doe@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("hashedTempPassword");
        doThrow(new EmailSendException("SMTP error"))
                .when(emailService)
                .sendOnboardingEmail(any(UserCreatedEvent.class));
        assertThrows(EmailSendException.class, () -> authService.createUser(request));
    }

    @Test
    void testLoginFirstTimeUserReturnsRestrictedToken() {
        LoginRequest request = new LoginRequest("john.doe@example.com", "tempPassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmployeeEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateRestrictedToken("USR002", "FRONT_DESK")).thenReturn("restrictedJwtToken");

        AuthResult response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.mustChangePassword());
        assertEquals("restrictedJwtToken", response.accessToken());
        assertNull(response.refreshToken());
    }

    @Test
    void testLoginNormalUserReturnsTokens() {
        user.setMustChangePassword(false);
        LoginRequest request = new LoginRequest("john.doe@example.com", "Password@123");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmployeeEmail("john.doe@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("USR002", "FRONT_DESK")).thenReturn("normalJwtToken");
        when(jwtService.getRefreshExpiration()).thenReturn(2700000L);

        AuthResult response = authService.login(request);

        assertNotNull(response);
        assertFalse(response.mustChangePassword());
        assertEquals("normalJwtToken", response.accessToken());
        assertNotNull(response.refreshToken());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void testLoginInactiveUserThrowsException() {
        user.setStatus("INACTIVE");
        LoginRequest request = new LoginRequest("john.doe@example.com", "tempPassword");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmployeeEmail("john.doe@example.com")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }

    @Test
    void testChangePasswordSuccess() {
        ChangePasswordRequest request = new ChangePasswordRequest("hashedPassword123", "NewPassword@123");
        when(userRepository.findById("USR002")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("hashedPassword123", "hashedPassword123")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword@123")).thenReturn("newHashedPassword");

        authService.changePassword("USR002", request);

        assertFalse(user.isMustChangePassword());
        assertEquals("newHashedPassword", user.getPasswordHash());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testChangePasswordWrongCurrentPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPassword", "NewPassword@123");
        when(userRepository.findById("USR002")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashedPassword123")).thenReturn(false);

        assertThrows(PasswordValidationException.class, () -> authService.changePassword("USR002", request));
    }

    @Test
    void testChangePasswordSamePasswordThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest("Password@123", "Password@123");
        when(userRepository.findById("USR002")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@123", "hashedPassword123")).thenReturn(true);

        assertThrows(PasswordValidationException.class, () -> authService.changePassword("USR002", request));
    }

    @Test
    void testChangePasswordWeakPasswordThrowsException() {
        ChangePasswordRequest request = new ChangePasswordRequest("hashedPassword123", "weak");
        when(userRepository.findById("USR002")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("hashedPassword123", "hashedPassword123")).thenReturn(true);

        assertThrows(PasswordValidationException.class, () -> authService.changePassword("USR002", request));
    }

    @Test
    void testRefreshSuccessRotatesToken() {
        user.setMustChangePassword(false);
        String rawToken = TokenUtils.generateOpaqueToken();
        String tokenHash = TokenUtils.hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        refreshToken.setExpiresAt(LocalDateTime.now().plusMinutes(40));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshToken));
        when(jwtService.generateToken("USR002", "FRONT_DESK")).thenReturn("newAccessToken");
        when(jwtService.getRefreshExpiration()).thenReturn(2700000L);

        AuthResult response = authService.refresh(rawToken);

        assertNotNull(response);
        assertEquals("newAccessToken", response.accessToken());
        assertNotNull(response.refreshToken());
        assertNotNull(refreshToken.getRevokedAt());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void testRefreshRevokedTokenThrowsException() {
        String rawToken = TokenUtils.generateOpaqueToken();
        String tokenHash = TokenUtils.hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setRevokedAt(LocalDateTime.now().minusMinutes(1));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshToken));

        assertThrows(InvalidTokenException.class, () -> authService.refresh(rawToken));
    }

    @Test
    void testRefreshExpiredTokenThrowsException() {
        String rawToken = TokenUtils.generateOpaqueToken();
        String tokenHash = TokenUtils.hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().minusMinutes(10));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshToken));

        assertThrows(InvalidTokenException.class, () -> authService.refresh(rawToken));
    }

    @Test
    void testRefreshInactiveUserThrowsException() {
        user.setStatus("INACTIVE");
        String rawToken = TokenUtils.generateOpaqueToken();
        String tokenHash = TokenUtils.hashToken(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setExpiresAt(LocalDateTime.now().plusMinutes(30));

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(refreshToken));

        assertThrows(InvalidTokenException.class, () -> authService.refresh(rawToken));
    }

    @Test
    void testRefreshMissingTokenThrowsException() {
        assertThrows(InvalidTokenException.class, () -> authService.refresh(null));
        assertThrows(InvalidTokenException.class, () -> authService.refresh("   "));
    }
}
