package com.adminvisitor.auth.service.Impl;

import com.adminvisitor.auth.dto.requestdto.*;
import com.adminvisitor.auth.dto.responsedto.AuthResult;
import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.dto.responsedto.VerifyOtpResponse;
import com.adminvisitor.auth.entity.Employee;
import com.adminvisitor.auth.entity.PasswordResetToken;
import com.adminvisitor.auth.entity.RefreshToken;
import com.adminvisitor.auth.entity.User;
import com.adminvisitor.auth.event.UserCreatedEvent;
import com.adminvisitor.auth.exception.*;
import com.adminvisitor.auth.repository.EmployeeRepository;
import com.adminvisitor.auth.repository.PasswordResetTokenRepository;
import com.adminvisitor.auth.repository.RefreshTokenRepository;
import com.adminvisitor.auth.repository.UserRepository;
import com.adminvisitor.auth.service.AuthService;
import com.adminvisitor.auth.service.EmailService;
import com.adminvisitor.auth.security.JwtService;
import com.adminvisitor.auth.service.IdGeneratorService;
import com.adminvisitor.auth.util.PasswordGenerator;
import com.adminvisitor.auth.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final IdGeneratorService idGeneratorService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {

        Employee employee = employeeRepository.findById(request.employeeId()).orElseThrow(() -> new ResourceNotFoundException("Employee not found with ID: " + request.employeeId()));

        if (userRepository.findByEmployeeEmail(employee.getEmail()).isPresent()) {
            throw new ResourceAlreadyExistsException("Employee already has a user account: " + request.employeeId());
        }

        if (!"ACTIVE".equalsIgnoreCase(employee.getStatus())) {
            throw new IllegalArgumentException("Employee is not active: " + request.employeeId());
        }

        String newUserId = idGeneratorService.generateId("USER", "USR-");
        String tempPassword = PasswordGenerator.generateTemporaryPassword();

        User user = new User();
        user.setId(newUserId);
        user.setEmployee(employee);
        user.setPasswordHash(passwordEncoder.encode(tempPassword));
        user.setRole(request.role());
        user.setStatus("ACTIVE");
        user.setMustChangePassword(true);

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepository.save(user);

        log.info("USER_CREATED userId={} employeeId={} role={}", user.getId(), employee.getId(), user.getRole());

        String fullName = employee.getFirstName() + " " + employee.getLastName();

        emailService.sendOnboardingEmail(new UserCreatedEvent(employee.getEmail(), fullName, tempPassword));

        return new UserResponse(user.getId(), employee.getId(), employee.getEmail(), fullName, user.getRole(), user.getStatus(), user.isMustChangePassword());
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
            log.warn("LOGIN_FAILED reason=INACTIVE_ACCOUNT userId={}", user.getId());
            throw new BadCredentialsException("User account is inactive");
        }

        Employee employee = user.getEmployee();

        String fullName = employee.getFirstName() + " " + employee.getLastName();

        UserResponse userResponse = new UserResponse(
                user.getId(),
                employee.getId(),
                employee.getEmail(),
                fullName,
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword()
        );

        if (user.isMustChangePassword()) {
            log.info(
                    "FIRST_LOGIN_PASSWORD_CHANGE_REQUIRED userId={} role={}",
                    user.getId(),
                    user.getRole()
            );

            String restrictedToken = jwtService.generateRestrictedToken(
                    user.getId(),
                    user.getRole().name()
            );

            return new AuthResult(
                    restrictedToken,
                    null,
                    true,
                    user.getRole(),
                    user.getId(),
                    "First login password change is required.",
                    userResponse
            );
        }

        String accessToken = jwtService.generateToken(
                user.getId(),
                user.getRole().name()
        );

        String rawRefreshToken = TokenUtils.generateOpaqueToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setId(
                idGeneratorService.generateId("REFRESH_TOKEN", "RTK-")
        );
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenUtils.hashToken(rawRefreshToken));
        refreshToken.setCreatedAt(LocalDateTime.now());
        refreshToken.setExpiresAt(
                LocalDateTime.now().plusSeconds(
                        jwtService.getRefreshExpiration() / 1000
                )
        );

        refreshTokenRepository.save(refreshToken);

        log.info("LOGIN_SUCCESS userId={} role={}", user.getId(), user.getRole());

        return new AuthResult(
                accessToken,
                rawRefreshToken,
                false,
                user.getRole(),
                user.getId(),
                "Authentication successful.",
                userResponse
        );
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            log.info("LOGOUT_PROCESSED reason=NO_REFRESH_TOKEN");
            return;
        }

        String tokenHash = TokenUtils.hashToken(rawRefreshToken);

        Optional<RefreshToken> refreshTokenOpt =
                refreshTokenRepository.findByTokenHash(tokenHash);

        if (refreshTokenOpt.isEmpty()) {
            log.info("LOGOUT_PROCESSED reason=REFRESH_TOKEN_NOT_FOUND");
            return;
        }

        RefreshToken refreshToken = refreshTokenOpt.get();

        if (refreshToken.getRevokedAt() == null) {
            refreshToken.setRevokedAt(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);

            log.info(
                    "LOGOUT_SUCCESS userId={}",
                    refreshToken.getUser().getId()
            );
        }
    }

    @Override
    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {

        User user = userRepository.findById(userId).orElseThrow(() -> {
            log.warn("PASSWORD_CHANGE_FAILED reason=USER_NOT_FOUND userId={}", userId);

            return new ResourceNotFoundException("User not found with ID: " + userId);
        });

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            log.warn("PASSWORD_CHANGE_FAILED reason=INVALID_CURRENT_PASSWORD userId={}", userId);

            throw new PasswordValidationException("Current password is incorrect");
        }

        if (request.confirmNewPassword() == null || !request.newPassword().equals(request.confirmNewPassword())) {

            log.warn("PASSWORD_CHANGE_FAILED reason=PASSWORD_CONFIRMATION_MISMATCH userId={}", userId);

            throw new PasswordValidationException("New password and confirmation password do not match");
        }

        validateNewPasswordPolicy(request.newPassword(), request.currentPassword());

        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);

        userRepository.save(user);

        log.info("PASSWORD_CHANGED userId={}", userId);
    }


    @Override
    @Transactional
    public AuthResult refresh(String rawRefreshToken) {

        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            log.warn("REFRESH_REJECTED reason=MISSING_TOKEN");
            throw new InvalidTokenException("Refresh token is missing");
        }

        String tokenHash = TokenUtils.hashToken(rawRefreshToken);

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(() -> {
                    log.warn("REFRESH_REJECTED reason=INVALID_TOKEN");
                    return new InvalidTokenException(
                            "Invalid or unrecognized refresh token"
                    );
                });

        LocalDateTime now = LocalDateTime.now();

        if (refreshToken.getRevokedAt() != null) {
            log.warn("REFRESH_REJECTED reason=REVOKED_TOKEN");
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (!refreshToken.getExpiresAt().isAfter(now)) {
            log.warn(
                    "REFRESH_REJECTED reason=EXPIRED_TOKEN userId={}",
                    refreshToken.getUser().getId()
            );
            throw new InvalidTokenException("Refresh token has expired");
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

        String newRawRefreshToken = TokenUtils.generateOpaqueToken();

        refreshToken.setTokenHash(
                TokenUtils.hashToken(newRawRefreshToken)
        );
        refreshToken.setExpiresAt(
                now.plusSeconds(
                        jwtService.getRefreshExpiration() / 1000
                )
        );

        refreshTokenRepository.save(refreshToken);

        String newAccessToken = jwtService.generateToken(
                user.getId(),
                user.getRole().name()
        );

        log.info(
                "REFRESH_SUCCESS userId={} role={}",
                user.getId(),
                user.getRole()
        );

        Employee employee = user.getEmployee();

        String fullName = employee.getFirstName() + " " + employee.getLastName();

        UserResponse userResponse = new UserResponse(
                user.getId(),
                employee.getId(),
                employee.getEmail(),
                fullName,
                user.getRole(),
                user.getStatus(),
                user.isMustChangePassword()
        );

        return new AuthResult(
                newAccessToken,
                newRawRefreshToken,
                false,
                user.getRole(),
                user.getId(),
                "Authentication successful.",
                userResponse
        );
    }

    @Override
    @Transactional
    public boolean forgotPassword(ForgotPasswordRequest request) {

        Optional<User> userOpt = userRepository.findByEmployeeEmail(request.email());

        if (userOpt.isEmpty() ||
                !"ACTIVE".equalsIgnoreCase(userOpt.get().getStatus())) {

            log.info("FORGOT_PASSWORD_PROCESSED reason=ACCOUNT_NOT_FOUND_OR_INACTIVE");

            return false;
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        Optional<PasswordResetToken> latestTokenOpt = passwordResetTokenRepository.findTopByUserIdAndInvalidatedFalseOrderByCreatedAtDesc(user.getId());

        if (latestTokenOpt.isPresent()) {
            PasswordResetToken latest = latestTokenOpt.get();

            if (latest.getLastSentAt().plusSeconds(60).isAfter(now)) {

                log.warn("FORGOT_PASSWORD_REJECTED reason=COOLDOWN_ACTIVE userId={}", user.getId());

                throw new OtpRateLimitException("Please wait 60 seconds before requesting another verification code.");
            }
        }

        int sendsInWindow = passwordResetTokenRepository.countByUserIdAndCreatedAtAfter(user.getId(), now.minusMinutes(15));

        if (sendsInWindow >= 3) {
            log.warn("FORGOT_PASSWORD_REJECTED reason=MAX_SENDS_EXCEEDED userId={}", user.getId());

            throw new OtpRateLimitException("Maximum verification code requests reached. Please try again after 15 minutes.");
        }

        passwordResetTokenRepository.invalidateAllByUserId(user.getId());

        int otpInt = SECURE_RANDOM.nextInt(1_000_000);
        String rawOtp = String.format("%06d", otpInt);
        String otpHash = TokenUtils.hashToken(rawOtp);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setId(
                idGeneratorService.generateId("PASSWORD_RESET", "PRT_")
        );
        resetToken.setUser(user);
        resetToken.setOtpHash(otpHash);
        resetToken.setOtpExpiresAt(now.plusMinutes(5));
        resetToken.setOtpAttemptCount(0);
        resetToken.setCreatedAt(now);
        resetToken.setLastSentAt(now);
        resetToken.setSendCount(sendsInWindow + 1);
        resetToken.setInvalidated(false);

        passwordResetTokenRepository.save(resetToken);

        log.info("FORGOT_PASSWORD_OTP_GENERATED userId={}", user.getId());

        String fullName = user.getEmployee().getFirstName() + " " + user.getEmployee().getLastName();

        emailService.sendPasswordResetOtpEmail(user.getEmployee().getEmail(), fullName, rawOtp, 5);

        return true;
    }

    @Override
    @Transactional(noRollbackFor = {InvalidOtpException.class, OtpRateLimitException.class})
    public VerifyOtpResponse verifyResetOtp(VerifyOtpRequest request) {

        Optional<User> userOpt = userRepository.findByEmployeeEmail(request.email());

        if (userOpt.isEmpty() || !"ACTIVE".equalsIgnoreCase(userOpt.get().getStatus())) {

            log.warn("VERIFY_OTP_FAILED reason=USER_NOT_FOUND_OR_INACTIVE");

            throw new InvalidOtpException("Invalid verification code or email");
        }

        User user = userOpt.get();
        LocalDateTime now = LocalDateTime.now();

        PasswordResetToken resetToken = passwordResetTokenRepository.findTopByUserIdAndInvalidatedFalseOrderByCreatedAtDesc(user.getId()).orElseThrow(() -> {
            log.warn("VERIFY_OTP_FAILED reason=NO_ACTIVE_OTP userId={}", user.getId());

            return new InvalidOtpException("Invalid verification code or email");
        });

        if (resetToken.isInvalidated()) {
            log.warn("VERIFY_OTP_FAILED reason=INVALIDATED_OTP userId={}", user.getId());

            throw new InvalidOtpException("Invalid verification code or email");
        }

        if (resetToken.getOtpVerifiedAt() != null) {
            log.warn("VERIFY_OTP_FAILED reason=OTP_ALREADY_VERIFIED userId={}", user.getId());

            throw new InvalidOtpException("Invalid verification code or email");
        }

        if (resetToken.getOtpAttemptCount() >= 5) {
            resetToken.setInvalidated(true);
            passwordResetTokenRepository.save(resetToken);

            log.warn("VERIFY_OTP_FAILED reason=MAX_ATTEMPTS_EXCEEDED userId={}", user.getId());

            throw new OtpRateLimitException("Maximum verification attempts exceeded. Please request a new verification code.");
        }

        if (resetToken.getOtpExpiresAt() != null && resetToken.getOtpExpiresAt().isBefore(now)) {

            resetToken.setInvalidated(true);
            passwordResetTokenRepository.save(resetToken);

            log.warn("VERIFY_OTP_FAILED reason=EXPIRED_OTP userId={}", user.getId());

            throw new InvalidOtpException("Verification code has expired. Please request a new code.");
        }

        String inputOtpHash = TokenUtils.hashToken(request.otp());

        if (!inputOtpHash.equals(resetToken.getOtpHash())) {

            int newCount = resetToken.getOtpAttemptCount() + 1;

            resetToken.setOtpAttemptCount(newCount);

            if (newCount >= 5) {
                resetToken.setInvalidated(true);
            }

            passwordResetTokenRepository.save(resetToken);

            log.warn("VERIFY_OTP_FAILED reason=WRONG_OTP attemptCount={} userId={}", newCount, user.getId());

            if (newCount >= 5) {
                throw new OtpRateLimitException("Maximum verification attempts exceeded. Please request a new verification code.");
            }

            throw new InvalidOtpException("Invalid verification code");
        }

        resetToken.setOtpVerifiedAt(now);

        String rawResetToken = TokenUtils.generateOpaqueToken();

        resetToken.setResetTokenHash(TokenUtils.hashToken(rawResetToken));
        resetToken.setResetTokenExpiresAt(now.plusMinutes(10));

        passwordResetTokenRepository.save(resetToken);

        log.info("VERIFY_OTP_SUCCESS userId={}", user.getId());

        return new VerifyOtpResponse(rawResetToken, "Verification code verified successfully. You may now reset your password.");
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        if (request.confirmNewPassword() == null || !request.newPassword().equals(request.confirmNewPassword())) {

            log.warn("RESET_PASSWORD_FAILED reason=CONFIRMATION_MISMATCH");

            throw new PasswordValidationException("New password and confirmation password do not match");
        }

        if (request.resetToken() == null || request.resetToken().isBlank()) {

            log.warn("RESET_PASSWORD_FAILED reason=MISSING_RESET_TOKEN");

            throw new InvalidTokenException("Reset token is required");
        }

        String resetTokenHash = TokenUtils.hashToken(request.resetToken());

        PasswordResetToken resetTokenEntity = passwordResetTokenRepository.findByResetTokenHashAndInvalidatedFalse(resetTokenHash).orElseThrow(() -> {
            log.warn("RESET_PASSWORD_FAILED reason=INVALID_RESET_TOKEN");

            return new InvalidTokenException("Invalid or unrecognized reset authorization");
        });

        if (resetTokenEntity.getResetTokenUsedAt() != null) {
            log.warn("RESET_PASSWORD_FAILED reason=ALREADY_USED_RESET_TOKEN userId={}", resetTokenEntity.getUser().getId());

            throw new InvalidTokenException("Reset authorization has already been used");
        }

        if (resetTokenEntity.getResetTokenExpiresAt() != null && resetTokenEntity.getResetTokenExpiresAt().isBefore(LocalDateTime.now())) {

            resetTokenEntity.setInvalidated(true);
            passwordResetTokenRepository.save(resetTokenEntity);

            log.warn("RESET_PASSWORD_FAILED reason=EXPIRED_RESET_TOKEN userId={}", resetTokenEntity.getUser().getId());

            throw new InvalidTokenException("Reset authorization has expired");
        }

        if (resetTokenEntity.getOtpVerifiedAt() == null) {
            log.warn("RESET_PASSWORD_FAILED reason=OTP_NOT_VERIFIED userId={}", resetTokenEntity.getUser().getId());

            throw new InvalidTokenException("OTP verification was not completed");
        }

        validateNewPasswordPolicy(request.newPassword(), null);

        User user = resetTokenEntity.getUser();

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            log.warn("RESET_PASSWORD_FAILED reason=INACTIVE_USER userId={}", user.getId());

            throw new InvalidTokenException("Associated user account is inactive");
        }

        resetTokenEntity.setResetTokenUsedAt(LocalDateTime.now());
        resetTokenEntity.setInvalidated(true);

        passwordResetTokenRepository.save(resetTokenEntity);

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setMustChangePassword(false);

        userRepository.save(user);

        refreshTokenRepository.revokeAllByUserId(user.getId(), LocalDateTime.now());

        log.info("RESET_PASSWORD_SUCCESS userId={}", user.getId());
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

            throw new PasswordValidationException("New password must contain at least one uppercase letter, " + "one lowercase letter, one digit, and one special character");
        }
    }
}