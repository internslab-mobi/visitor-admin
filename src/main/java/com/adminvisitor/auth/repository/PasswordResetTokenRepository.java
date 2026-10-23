package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Spring Data JPA repository for persisting and querying password reset tokens in {@code vms_password_reset}.
 * <p>
 * Manages token lifecycle, verification attempts, rate-limiting counts, and bulk invalidation.
 * </p>
 *
 * @author Logeshwaran
 */
@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, String> {

    /**
     * Finds the most recently created, active (uninvalidated) password reset token for a specific user.
     *
     * @param userId the user identifier
     * @return an {@link Optional} containing the latest active reset token, or empty if none exists
     */
    Optional<PasswordResetToken> findTopByUserIdAndInvalidatedFalseOrderByCreatedAtDesc(String userId);

    /**
     * Counts the number of password reset attempts created for a user after a specified timestamp.
     * <p>
     * Used to enforce hourly rate limiting on password reset requests.
     * </p>
     *
     * @param userId the user identifier
     * @param dateTime the cutoff threshold timestamp
     * @return total count of reset records created after the given timestamp
     */
    int countByUserIdAndCreatedAtAfter(String userId, LocalDateTime dateTime);

    /**
     * Invalidates all active password reset tokens belonging to a user by setting {@code invalidated = true}.
     *
     * @param userId the user identifier whose tokens should be invalidated
     */
    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.invalidated = true WHERE p.user.id = :userId AND p.invalidated = false")
    void invalidateAllByUserId(@Param("userId") String userId);

    /**
     * Looks up an active (uninvalidated) password reset token using its authorization token hash.
     *
     * @param resetTokenHash the cryptographic hash of the reset authorization token
     * @return an {@link Optional} containing the matching reset token record, or empty if not found
     */
    Optional<PasswordResetToken> findByResetTokenHashAndInvalidatedFalse(String resetTokenHash);
}
