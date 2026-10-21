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

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
                UPDATE RefreshToken r
                SET r.revokedAt = :revokedAt
                WHERE r.user.id = :userId
                  AND r.revokedAt IS NULL
            """)
    int revokeAllByUserId(@Param("userId") String userId, @Param("revokedAt") LocalDateTime revokedAt);

    long countByUser_IdAndRevokedAtIsNullAndExpiresAtAfter(
            String userId,
            LocalDateTime now
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
                SELECT r
                FROM RefreshToken r
                WHERE r.tokenHash = :tokenHash
            """)
    Optional<RefreshToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}