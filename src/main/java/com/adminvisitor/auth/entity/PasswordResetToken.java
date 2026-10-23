package com.adminvisitor.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity managing the lifecycle, verification, and rate limiting of password reset workflows.
 * <p>
 * Mapped to the database table {@code vms_password_reset}. Tracks hashed one-time passwords (OTP),
 * expiry timestamps, verification attempts, dispatch count limits, and single-use reset authorization tokens.
 * </p>
 *
 * @author Logeshwaran
 */
@Entity
@Table(
        name = "vms_password_reset",
        indexes = {
                @Index(name = "idx_vms_password_reset_user", columnList = "user_id"),
                @Index(name = "idx_vms_password_reset_token_hash", columnList = "reset_token_hash")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * BCrypt hash of the 6-digit numeric OTP sent to the user.
     */
    @Column(name = "otp_hash", length = 255)
    private String otpHash;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    /**
     * Running count of incorrect OTP verification attempts against this token.
     */
    @Column(name = "otp_attempt_count", nullable = false)
    private int otpAttemptCount = 0;

    @Column(name = "otp_verified_at")
    private LocalDateTime otpVerifiedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    /**
     * Number of times the OTP has been dispatched, enforced by rate limiting rules.
     */
    @Column(name = "send_count", nullable = false)
    private int sendCount = 1;

    /**
     * Indicates whether this reset session has been superseded or explicitly invalidated.
     */
    @Column(name = "invalidated", nullable = false)
    private boolean invalidated = false;

    /**
     * Cryptographic hash of the single-use reset authorization token issued upon OTP verification.
     */
    @Column(name = "reset_token_hash", length = 255)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private LocalDateTime resetTokenExpiresAt;

    @Column(name = "reset_token_used_at")
    private LocalDateTime resetTokenUsedAt;
}