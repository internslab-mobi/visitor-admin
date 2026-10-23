//package com.adminvisitor.service.impl;
//
//import com.adminvisitor.entity.NotificationOutbox;
//import com.adminvisitor.enums.NotificationOutboxStatus;
//import com.adminvisitor.repository.NotificationOutboxRepository;
//import com.adminvisitor.service.NotificationOutboxProcessor;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class NotificationOutboxProcessorImpl implements NotificationOutboxProcessor {
//
//    private final NotificationOutboxRepository notificationOutboxRepository;
//
//    @Transactional
//    public boolean claim(String id) {
//        LocalDateTime now = LocalDateTime.now();
//
//        boolean claimed = notificationOutboxRepository.claimPendingNotification(
//                id,
//                NotificationOutboxStatus.PENDING,
//                NotificationOutboxStatus.PROCESSING,
//                now
//        ) == 1;
//
//        if (claimed) {
//            log.debug("NOTIFICATION_OUTBOX_CLAIMED outboxId={}", id);
//        }
//
//        return claimed;
//    }
//
//    @Transactional
//    public void markSent(String id) {
//        NotificationOutbox outbox = getOutbox(id);
//
//        outbox.setStatus(NotificationOutboxStatus.SENT);
//        outbox.setSentAt(LocalDateTime.now());
//        outbox.setLastError(null);
//
//        notificationOutboxRepository.save(outbox);
//
//        log.info(
//                "NOTIFICATION_OUTBOX_SENT outboxId={} attemptCount={}",
//                outbox.getId(),
//                outbox.getAttemptCount()
//        );
//    }
//
//    @Transactional
//    public void markFailed(String id, String error) {
//        NotificationOutbox outbox = getOutbox(id);
//
//        outbox.setLastError(truncateError(error));
//
//        if (outbox.getAttemptCount() >= outbox.getMaxAttempts()) {
//            outbox.setStatus(NotificationOutboxStatus.FAILED);
//
//            log.warn(
//                    "NOTIFICATION_OUTBOX_FAILED outboxId={} attemptCount={} maxAttempts={} reason={}",
//                    outbox.getId(),
//                    outbox.getAttemptCount(),
//                    outbox.getMaxAttempts(),
//                    outbox.getLastError()
//            );
//
//            return;
//        }
//
//        // Retry after 30, then 60 seconds.
//        long delaySeconds = 30L * (1L << (outbox.getAttemptCount() - 1));
//
//        outbox.setStatus(NotificationOutboxStatus.PENDING);
//        outbox.setNextAttemptAt(
//                LocalDateTime.now().plusSeconds(delaySeconds)
//        );
//
//        log.warn(
//                "NOTIFICATION_OUTBOX_RETRY_SCHEDULED outboxId={} attemptCount={} nextAttemptAt={} reason={}",
//                outbox.getId(),
//                outbox.getAttemptCount(),
//                outbox.getNextAttemptAt(),
//                outbox.getLastError()
//        );
//    }
//
//    private NotificationOutbox getOutbox(String id) {
//        return notificationOutboxRepository.findById(id)
//                .orElseThrow(() ->
//                        new IllegalStateException(
//                                "Notification outbox record not found: " + id
//                        )
//                );
//    }
//
//    private String truncateError(String error) {
//        if (error == null || error.isBlank()) {
//            return "Notification delivery failed";
//        }
//
//        String normalizedError = error.toLowerCase();
//
//        if (normalizedError.contains("timeout")) {
//            return "Notification service request timed out";
//        }
//
//        if (normalizedError.contains("connection refused")
//                || normalizedError.contains("connect exception")
//                || normalizedError.contains("connection reset")) {
//            return "Notification service is unreachable";
//        }
//
//        if (normalizedError.contains("http 5")
//                || normalizedError.contains("500")
//                || normalizedError.contains("502")
//                || normalizedError.contains("503")
//                || normalizedError.contains("504")) {
//            return "Notification service returned a server error";
//        }
//
//        if (normalizedError.contains("http 4")
//                || normalizedError.contains("400")
//                || normalizedError.contains("401")
//                || normalizedError.contains("403")
//                || normalizedError.contains("404")) {
//            return "Notification service rejected the request";
//        }
//
//        return "Notification delivery failed";
//    }
//}