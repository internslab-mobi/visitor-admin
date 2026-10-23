package com.adminvisitor.auth.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing a persisted refresh token supporting session management and token rotation.
 * <p>
 * Mapped to the database table {@code vms_refresh_token}. Stores SHA-256 hashed refresh tokens,
 * associating each with a {@link User} and tracking its creation, expiration, and revocation state.
 * </p>
 *
 * @author Logeshwaran
 */
@Entity
@Table(name = "vms_refresh_token")
@Getter
@Setter
@NoArgsConstructor
public class RefreshToken {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * SHA-256 hash of the issued plaintext refresh token string.
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp when this refresh token was revoked; {@code null} if currently active.
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
}