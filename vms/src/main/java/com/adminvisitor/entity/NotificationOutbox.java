package com.adminvisitor.entity;

import com.adminvisitor.enums.NotificationOutboxStatus;
import com.adminvisitor.enums.NotificationScenario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_notification_outbox")
@Getter
@Setter
@NoArgsConstructor
public class NotificationOutbox extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 30)
    private String id;

    @Column(name = "notification_id", nullable = false, unique = true, length = 36)
    private String notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario", nullable = false, length = 50)
    private NotificationScenario scenario;

    @Column(name = "recipient", nullable = false, length = 254)
    private String recipient;

    @Column(name = "cc", length = 254)
    private String cc;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "json")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationOutboxStatus status = NotificationOutboxStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
}