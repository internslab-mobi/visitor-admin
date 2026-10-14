package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findTopByUserIdAndInvalidatedFalseOrderByCreatedAtDesc(String userId);

    int countByUserIdAndCreatedAtAfter(String userId, LocalDateTime dateTime);

    @Modifying
    @Query("UPDATE PasswordResetToken p SET p.invalidated = true WHERE p.user.id = :userId AND p.invalidated = false")
    void invalidateAllByUserId(@Param("userId") String userId);

    Optional<PasswordResetToken> findByResetTokenHashAndInvalidatedFalse(String resetTokenHash);
}
