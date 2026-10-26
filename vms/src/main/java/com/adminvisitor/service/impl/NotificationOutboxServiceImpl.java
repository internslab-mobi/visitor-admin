package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.NotificationRequest;
import com.adminvisitor.entity.NotificationOutbox;
import com.adminvisitor.enums.NotificationOutboxStatus;
import com.adminvisitor.enums.NotificationScenario;
import com.adminvisitor.repository.NotificationOutboxRepository;
import com.adminvisitor.service.IdGeneratorService;
import com.adminvisitor.service.NotificationClientService;
import com.adminvisitor.service.NotificationOutboxService;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
/**
 * Implements notification outbox operations, including enqueueing notifications,
 * claiming records for processing, tracking delivery results, and scheduling retries.
 * A scheduled worker retrieves pending notifications and delegates email delivery
 * to the notification client service.
 *
 * @author Nethra
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxServiceImpl
        implements NotificationOutboxService {

    @Value("${notification.outbox.max-attempts}")
    private int maxAttempts;

    @Value("${notification.outbox.batch-size}")
    private int batchSize;

    @Value("${notification.outbox.retry-delay-seconds}")
    private long retryDelaySeconds;

    @Value("${notification.outbox.polling-delay-ms}")
    private long pollingDelayMs;

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final IdGeneratorService idGeneratorService;
    private final NotificationClientService notificationClientService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    /**
     * Creates a transaction template that runs independently of any existing
     * transaction. This allows outbox claiming and delivery-status updates
     * to be committed separately from the transaction that enqueued a notification.
     *
     * @return a transaction template configured with {@code PROPAGATION_REQUIRES_NEW}
     */
    private TransactionTemplate newIndependentTransaction() {
        TransactionTemplate transactionTemplate =
                new TransactionTemplate(transactionManager);

        transactionTemplate.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW
        );

        return transactionTemplate;
    }

    /**
     * Validates and enqueues a notification request in the outbox for asynchronous
     * delivery. Duplicate notification IDs are skipped to prevent duplicate
     * outbox entries.
     *
     * @param request the notification request containing the ID, scenario,
     *                recipients, and scenario-specific data
     * @throws IllegalArgumentException if the request is null or its notification ID
     *                                  is null or blank, or if its payload cannot be serialized
     */
    @Override
    @Transactional
    public void enqueue(NotificationRequest request) {

        if (request == null
                || request.notificationId() == null
                || request.notificationId().isBlank()) {

            throw new IllegalArgumentException(
                    "Notification ID must not be blank"
            );
        }

        if (notificationOutboxRepository
                .existsByNotificationId(request.notificationId())) {

            log.debug(
                    "NOTIFICATION_OUTBOX_DUPLICATE_SKIPPED notificationId={} scenario={}",
                    request.notificationId(),
                    request.scenario()
            );

            return;
        }

        NotificationOutbox outbox = new NotificationOutbox();

        outbox.setId(
                idGeneratorService.generateId(
                        "NOTIFICATION_OUTBOX",
                        "NTF"
                )
        );

        outbox.setNotificationId(request.notificationId());

        outbox.setScenario(
                NotificationScenario.valueOf(request.scenario())
        );

        outbox.setRecipient(request.recipient());
        outbox.setCc(request.cc());
        outbox.setPayload(serializeData(request));
        outbox.setStatus(NotificationOutboxStatus.PENDING);
        outbox.setAttemptCount(0);
        outbox.setMaxAttempts(maxAttempts);
        outbox.setNextAttemptAt(LocalDateTime.now());

        NotificationOutbox savedOutbox =
                notificationOutboxRepository.save(outbox);

        log.info(
                "NOTIFICATION_OUTBOX_ENQUEUED outboxId={} scenario={} maxAttempts={}",
                savedOutbox.getId(),
                savedOutbox.getScenario(),
                savedOutbox.getMaxAttempts()
        );
    }

    /**
     * Serializes the notification's scenario-specific data into JSON for storage
     * in the outbox payload.
     *
     * @param request the notification request containing the data to serialize
     * @return the serialized JSON payload
     * @throws IllegalArgumentException if serialization fails
     */
    private String serializeData(NotificationRequest request) {

        try {
            return objectMapper.writeValueAsString(request.data());

        } catch (JacksonException exception) {

            throw new IllegalArgumentException(
                    "Failed to serialize notification payload",
                    exception
            );
        }
    }

    /**
     * Atomically claims a pending notification by changing its status to
     * {@code PROCESSING}. The independent transaction helps prevent multiple
     * workers from processing the same notification simultaneously.
     *
     * @param id the outbox record ID
     * @return {@code true} if the notification was successfully claimed;
     *         {@code false} if it could not be claimed
     */
    @Override
    public boolean claim(String id) {

        Boolean claimed = newIndependentTransaction().execute(status -> {

            int updatedRows =
                    notificationOutboxRepository.claimPendingNotification(
                            id,
                            NotificationOutboxStatus.PENDING,
                            NotificationOutboxStatus.PROCESSING,
                            LocalDateTime.now()
                    );

            return updatedRows == 1;
        });

        boolean successfullyClaimed = Boolean.TRUE.equals(claimed);

        if (successfullyClaimed) {
            log.debug(
                    "NOTIFICATION_OUTBOX_CLAIMED outboxId={}",
                    id
            );
        }

        return successfullyClaimed;
    }

    /**
     * Marks a notification as successfully sent, records its delivery timestamp,
     * and clears any previous delivery error.
     *
     * @param id the outbox record ID
     */
    @Override
    public void markSent(String id) {

        newIndependentTransaction().executeWithoutResult(status -> {

            NotificationOutbox outbox = getOutbox(id);

            outbox.setStatus(NotificationOutboxStatus.SENT);
            outbox.setSentAt(LocalDateTime.now());
            outbox.setLastError(null);

            notificationOutboxRepository.save(outbox);

            log.info(
                    "NOTIFICATION_OUTBOX_SENT outboxId={} attemptCount={}",
                    outbox.getId(),
                    outbox.getAttemptCount()
            );
        });
    }

    /**
     * Records a delivery failure and either schedules another attempt or marks
     * the notification as permanently failed when its maximum number of attempts
     * has been reached.
     *
     * @param id the outbox record ID
     * @param error the error message describing the delivery failure
     */
    @Override
    public void markFailed(String id, String error) {

        newIndependentTransaction().executeWithoutResult(status -> {

            NotificationOutbox outbox = getOutbox(id);

            outbox.setLastError(truncateError(error));

            if (outbox.getAttemptCount() >= outbox.getMaxAttempts()) {

                outbox.setStatus(NotificationOutboxStatus.FAILED);

                log.warn(
                        "NOTIFICATION_OUTBOX_FAILED outboxId={} attemptCount={} maxAttempts={} reason={}",
                        outbox.getId(),
                        outbox.getAttemptCount(),
                        outbox.getMaxAttempts(),
                        outbox.getLastError()
                );

                return;
            }

            // Retry after 30, then 60 seconds.
            long delaySeconds = retryDelaySeconds
                    * (1L << (outbox.getAttemptCount() - 1));

            outbox.setStatus(NotificationOutboxStatus.PENDING);

            outbox.setNextAttemptAt(
                    LocalDateTime.now().plusSeconds(delaySeconds)
            );

            log.warn(
                    "NOTIFICATION_OUTBOX_RETRY_SCHEDULED outboxId={} attemptCount={} nextAttemptAt={} reason={}",
                    outbox.getId(),
                    outbox.getAttemptCount(),
                    outbox.getNextAttemptAt(),
                    outbox.getLastError()
            );
        });
    }

    /**
     * Retrieves an outbox record by its ID.
     *
     * @param id the outbox record ID
     * @return the matching notification outbox record
     * @throws IllegalStateException if no outbox record exists for the supplied ID
     */
    private NotificationOutbox getOutbox(String id) {

        return notificationOutboxRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Notification outbox record not found: " + id
                        )
                );
    }

    /**
     * Converts an exception message into a safe, standardized delivery error.
     * Known connection, timeout, and HTTP error categories receive specific
     * messages rather than exposing the original technical details.
     *
     * @param error the original error message
     * @return a normalized error message suitable for storing in the outbox
     */
    private String truncateError(String error) {

        if (error == null || error.isBlank()) {
            return "Notification delivery failed";
        }

        String normalizedError = error.toLowerCase();

        if (normalizedError.contains("timeout")) {
            return "Notification service request timed out";
        }

        if (normalizedError.contains("connection refused")
                || normalizedError.contains("connect exception")
                || normalizedError.contains("connection reset")) {

            return "Notification service is unreachable";
        }

        if (normalizedError.contains("http 5")
                || normalizedError.contains("500")
                || normalizedError.contains("502")
                || normalizedError.contains("503")
                || normalizedError.contains("504")) {

            return "Notification service returned a server error";
        }

        if (normalizedError.contains("http 4")
                || normalizedError.contains("400")
                || normalizedError.contains("401")
                || normalizedError.contains("403")
                || normalizedError.contains("404")) {

            return "Notification service rejected the request";
        }

        return "Notification delivery failed";
    }

    /**
     * Periodically retrieves a limited batch of pending notifications that are
     * ready for delivery and processes each record. The polling interval and
     * batch size are configured through application properties.
     */
    @Override
    @Scheduled(fixedDelayString = "${notification.outbox.polling-delay-ms}")
    public void processPendingNotifications() {

        var notifications =
                notificationOutboxRepository.findReadyToProcess(
                        NotificationOutboxStatus.PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, batchSize)
                );

        if (notifications.isEmpty()) {
            return;
        }

        log.debug(
                "NOTIFICATION_OUTBOX_BATCH_RETRIEVED batchSize={}",
                notifications.size()
        );

        for (NotificationOutbox notification : notifications) {
            processNotification(notification);
        }
    }

    /**
     * Claims and delivers an individual notification. On successful delivery,
     * the record is marked as sent. If delivery fails after the record has been
     * claimed, the failure is recorded and a retry may be scheduled.
     *
     * @param notification the outbox record to process
     */
    private void processNotification(NotificationOutbox notification) {

        String id = notification.getId();
        boolean claimed = false;

        try {
            claimed = claim(id);

            if (!claimed) {
                log.debug(
                        "NOTIFICATION_OUTBOX_CLAIM_SKIPPED outboxId={}",
                        id
                );

                return;
            }

            Map<String, Object> data = objectMapper.readValue(
                    notification.getPayload(),
                    new TypeReference<>() {}
            );

            NotificationRequest request = new NotificationRequest(
                    notification.getNotificationId(),
                    notification.getScenario().name(),
                    notification.getRecipient(),
                    notification.getCc(),
                    data
            );

            notificationClientService.sendEmail(request);

            markSent(id);

            log.info(
                    "NOTIFICATION_OUTBOX_PROCESSING_SUCCEEDED outboxId={} scenario={}",
                    id,
                    notification.getScenario()
            );

        } catch (Exception exception) {

            log.error(
                    "NOTIFICATION_OUTBOX_PROCESSING_FAILED outboxId={} scenario={}",
                    id,
                    notification.getScenario(),
                    exception
            );

            if (claimed) {
                try {
                    markFailed(id, exception.getMessage());

                } catch (Exception updateException) {

                    log.error(
                            "NOTIFICATION_OUTBOX_FAILURE_STATE_UPDATE_FAILED outboxId={}",
                            id,
                            updateException
                    );
                }
            }
        }
    }
}