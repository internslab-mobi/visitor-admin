package com.adminvisitor.auth.service;

import com.adminvisitor.auth.dto.requestdto.ChangePasswordRequest;
import com.adminvisitor.auth.dto.requestdto.CreateUserRequest;
import com.adminvisitor.auth.dto.requestdto.LoginRequest;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.entity.Employee;
import com.adminvisitor.auth.entity.RefreshToken;
import com.adminvisitor.auth.entity.User;
import com.adminvisitor.auth.event.UserCreatedEvent;
import com.adminvisitor.auth.exception.InvalidTokenException;
import com.adminvisitor.auth.exception.PasswordValidationException;
import com.adminvisitor.auth.exception.ResourceAlreadyExistsException;
import com.adminvisitor.auth.exception.ResourceNotFoundException;
import com.adminvisitor.auth.repository.EmployeeRepository;
import com.adminvisitor.auth.repository.RefreshTokenRepository;
import com.adminvisitor.auth.repository.UserRepository;
import com.adminvisitor.auth.util.PasswordGenerator;
import com.adminvisitor.auth.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found with ID: " + request.employeeId()
                        )
                );

        if (userRepository.findByEmployeeEmail(employee.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException(
                    "Employee already has a user account: " + request.employeeId()
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
            throw new IllegalArgumentException(
                    "Employee is not active: " + request.employeeId()
            );
        }

        String newUserId = generateNextUserId();
        String tempPassword = PasswordGenerator.generateTemporaryPassword();

        User user = new User();
        user.setId(newUserId);
        user.setEmployee(employee);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(request.role());
        user.setStatus("ACTIVE");
        user.setMustChangePassword(true);

        userRepository.save(user);

        log.info(
                "USER_CREATED userId={} employeeId={} role={}",
                user.getId(),
                employee.getId(),
                user.getRole()
        );

        String fullName = employee.getFirstName() + " " + employee.getLastName();

        emailService.sendOnboardingEmail(
                new UserCreatedEvent(
                        employee.getEmail(),
                        fullName,
                        tempPassword
                )
        );


        return new UserResponse(
                user.getId(),
                employee.getId(),
                employee.getEmail(),
                fullName,
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword()
        );
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        User user = userRepository.findByEmployeeEmail(request.email())
                .orElseThrow(() -> {
                    log.warn("LOGIN_FAILED reason=USER_NOT_FOUND");
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            log.warn(
                    "LOGIN_FAILED reason=INACTIVE_ACCOUNT userId={}",
                    user.getId()
            );

            throw new BadCredentialsException("User account is inactive");
        }

        if (user.isMustChangePassword()) {

            log.info(
                    "FIRST_LOGIN_PASSWORD_CHANGE_REQUIRED userId={} role={}",
                    user.getId(),
                    user.getRole()
            );

            String restrictedToken =
                    jwtService.generateRestrictedToken(
                            user.getId(),
                            user.getRole().name()
                    );

            return new AuthResult(
                    restrictedToken,
                    null,
                    true,
                    user.getRole(),
                    user.getId(),
                    "First login password change is required."
            );
        }

        String accessToken =
                jwtService.generateToken(
                        user.getId(),
                        user.getRole().name()
                );

        String rawRefreshToken =
                TokenUtils.generateOpaqueToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(
                TokenUtils.hashToken(rawRefreshToken)
        );
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(
                LocalDateTime.now()
                        .plusSeconds(jwtService.getRefreshExpiration() / 1000)
        );

        refreshTokenRepository.save(refreshToken);

        log.info(
                "LOGIN_SUCCESS userId={} role={}",
                user.getId(),
                user.getRole()
        );

        return new AuthResult(
                accessToken,
                rawRefreshToken,
                false,
                user.getRole(),
                user.getId(),
                "Authentication successful."
        );
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn(
                            "PASSWORD_CHANGE_FAILED reason=USER_NOT_FOUND userId={}",
                            userId
                    );
                    return new ResourceNotFoundException(
                            "User not found with ID: " + userId
                    );
                });

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            log.warn(
                    "PASSWORD_CHANGE_FAILED reason=INVALID_CURRENT_PASSWORD userId={}",
                    userId
            );

            throw new PasswordValidationException(
                    "Current password is incorrect"
            );
        }

        validateNewPasswordPolicy(
                request.newPassword(),
                request.currentPassword()
        );

        refreshTokenRepository.revokeAllByUserId(
                userId,
                LocalDateTime.now()
        );

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );
        user.setMustChangePassword(false);

        userRepository.save(user);

        log.info(
                "PASSWORD_CHANGED userId={}",
                userId
        );
    }

    @Override
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            log.warn("REFRESH_REJECTED reason=MISSING_TOKEN");

            throw new InvalidTokenException("Refresh token is missing");
        }

        String tokenHash = TokenUtils.hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> {
                    log.warn("REFRESH_REJECTED reason=INVALID_TOKEN");

                    return new InvalidTokenException(
                            "Invalid or unrecognized refresh token"
                    );
                });

        if (refreshToken.getRevokedAt() != null) {
            log.warn("REFRESH_REJECTED reason=REVOKED_TOKEN");

            throw new InvalidTokenException(
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn(
                    "REFRESH_REJECTED reason=EXPIRED_TOKEN userId={}",
                    refreshToken.getUser().getId()
            );

            throw new InvalidTokenException(
                    "Refresh token has expired"
            );
        }

        User user = refreshToken.getUser();

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            log.warn(
                    "REFRESH_REJECTED reason=INACTIVE_ACCOUNT userId={}",
                    user.getId()
            );

            throw new InvalidTokenException(
                    "Associated user account is inactive"
            );
        }

        // Revoke old token
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        // Issue new rotated refresh token & access token
        String newRawRefreshToken = TokenUtils.generateOpaqueToken();

        RefreshToken newRefreshToken = new RefreshToken();
        newRefreshToken.setUser(user);
        newRefreshToken.setTokenHash(
                TokenUtils.hashToken(newRawRefreshToken)
        );
        newRefreshToken.setCreatedAt(LocalDateTime.now());
        newRefreshToken.setExpiresAt(
                LocalDateTime.now()
                        .plusSeconds(jwtService.getRefreshExpiration() / 1000)
        );

        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken =
                jwtService.generateToken(
                        user.getId(),
                        user.getRole().name()
                );

        log.info(
                "REFRESH_SUCCESS userId={} role={}",
                user.getId(),
                user.getRole()
        );

        return new AuthResult(
                newAccessToken,
                newRawRefreshToken,
                false,
                user.getRole(),
                user.getId(),
                "Authentication successful."
        );
    }

    private synchronized String generateNextUserId() {
        long count = userRepository.count() + 1;
        String nextId = String.format("USR%03d", count);
        while (userRepository.existsById(nextId)) {
            count++;
            nextId = String.format("USR%03d", count);
        }
        return nextId;
    }

    private void validateNewPasswordPolicy(String newPassword, String currentPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new PasswordValidationException("New password must be at least 8 characters long");
        }
        if (newPassword.equals(currentPassword)) {
            throw new PasswordValidationException("New password cannot be the same as the current password");
        }
        boolean hasUpper = newPassword.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = newPassword.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = newPassword.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = newPassword.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new PasswordValidationException("New password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
    }
}
