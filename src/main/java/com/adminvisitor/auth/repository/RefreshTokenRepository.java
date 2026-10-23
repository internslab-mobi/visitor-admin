package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA repository for persisting and querying refresh tokens in {@code vms_refresh_token}.
 * <p>
 * Manages token persistence, pessimistic locking during token rotation, session count checks, and user session revocation.
 * </p>
 *
 * @author Logeshwaran
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Finds a refresh token record by its SHA-256 token hash.
     *
     * @param tokenHash the SHA-256 hash of the plaintext token
     * @return an {@link Optional} containing the refresh token record, or empty if not found
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Revokes all active refresh tokens belonging to a user by recording the revocation timestamp.
     *
     * @param userId the user identifier whose sessions are being terminated
     * @param revokedAt the timestamp when the revocation took effect
     * @return the number of refresh token records updated
     */
    @Modifying
    @Query("""
                UPDATE RefreshToken r
                SET r.revokedAt = :revokedAt
                WHERE r.user.id = :userId
                  AND r.revokedAt IS NULL
            """)
    int revokeAllByUserId(@Param("userId") String userId, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * Counts the number of active, unexpired, and unrevoked refresh tokens associated with a user.
     * <p>
     * Used to enforce maximum concurrent active session policies.
     * </p>
     *
     * @param userId the user identifier
     * @param now the current reference timestamp
     * @return count of active concurrent sessions
     */
    long countByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(
            String userId,
            LocalDateTime now
    );

    /**
     * Finds a refresh token by its hash while acquiring a pessimistic write lock ({@code PESSIMISTIC_WRITE}).
     * <p>
     * Prevents race conditions during concurrent token rotation operations.
     * </p>
     *
     * @param tokenHash the SHA-256 hash of the token
     * @return an {@link Optional} containing the locked refresh token, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT r
                FROM RefreshToken r
                WHERE r.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}