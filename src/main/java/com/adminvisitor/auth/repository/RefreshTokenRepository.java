package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository
        extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
    UPDATE RefreshToken r
    SET r.revokedAt = :revokedAt
    WHERE r.user.id = :userId
      AND r.revokedAt IS NULL
""")
    int revokeAllByUserId(
            @Param("userId") String userId,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}