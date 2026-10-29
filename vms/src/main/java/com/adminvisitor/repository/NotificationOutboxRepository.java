
package com.adminvisitor.repository;

import com.adminvisitor.entity.NotificationOutbox;
import com.adminvisitor.enums.NotificationOutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutbox, String> {

    @Query("""
        SELECT n FROM NotificationOutbox n
        WHERE n.status = :status
          AND n.nextAttemptAt <= :now
        ORDER BY n.nextAttemptAt ASC, n.id ASC
        """)
    List<NotificationOutbox> findReadyToProcess(
            @Param("status") NotificationOutboxStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying
    @Query("""
        UPDATE NotificationOutbox n
        SET n.status = :processing,
            n.lastAttemptAt = :now,
            n.attemptCount = n.attemptCount + 1
        WHERE n.id = :id
          AND n.status = :pending
          AND n.nextAttemptAt <= :now
        """)
    int claimPendingNotification(
            @Param("id") String id,
            @Param("pending") NotificationOutboxStatus pending,
            @Param("processing") NotificationOutboxStatus processing,
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("""
        UPDATE NotificationOutbox n
        SET n.status = :pending,
            n.lastError = :error,
            n.nextAttemptAt = :now
        WHERE n.status = :processing
          AND n.lastAttemptAt <= :staleBefore
          AND n.attemptCount < n.maxAttempts
        """)
    int recoverStaleNotifications(
            @Param("processing") NotificationOutboxStatus processing,
            @Param("pending") NotificationOutboxStatus pending,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("now") LocalDateTime now,
            @Param("error") String error
    );

    @Modifying
    @Query("""
        UPDATE NotificationOutbox n
        SET n.status = :failed,
            n.lastError = :error
        WHERE n.status = :processing
          AND n.lastAttemptAt <= :staleBefore
          AND n.attemptCount >= n.maxAttempts
        """)
    int failExhaustedStaleNotifications(
            @Param("processing") NotificationOutboxStatus processing,
            @Param("failed") NotificationOutboxStatus failed,
            @Param("staleBefore") LocalDateTime staleBefore,
            @Param("error") String error
    );

    boolean existsByNotificationId(String notificationId);
}
