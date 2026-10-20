
package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.NotificationRequest;
import com.adminvisitor.entity.NotificationOutbox;
import com.adminvisitor.enums.NotificationOutboxStatus;
import com.adminvisitor.repository.NotificationOutboxRepository;
import com.adminvisitor.service.NotificationClientService;
import com.adminvisitor.service.NotificationOutboxProcessor;
import com.adminvisitor.service.NotificationOutboxWorker;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxWorkerImpl implements NotificationOutboxWorker {

    private static final int BATCH_SIZE = 50;

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final NotificationOutboxProcessor notificationOutboxProcessor;
    private final NotificationClientService notificationClientService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void processPendingNotifications() {
        var notifications =
                notificationOutboxRepository.findReadyToProcess(
                        NotificationOutboxStatus.PENDING,
                        LocalDateTime.now(),
                        PageRequest.of(0, BATCH_SIZE)
                );

        for (NotificationOutbox notification : notifications) {
            processNotification(notification);
        }
    }


    private void processNotification(NotificationOutbox notification) {
        String id = notification.getId();
        boolean claimed = false;

        try {
            claimed = notificationOutboxProcessor.claim(id);

            if (!claimed) {
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

            notificationOutboxProcessor.markSent(id);

            log.info(
                    "Notification accepted by service. id={}, scenario={}",
                    id,
                    notification.getScenario()
            );

        } catch (Exception exception) {
            log.error("Notification processing failed. id={}", id, exception);

            if (claimed) {
                try {
                    notificationOutboxProcessor.markFailed(
                            id,
                            exception.getMessage()
                    );
                } catch (Exception updateException) {
                    log.error(
                            "Failed to update notification failure state. id={}",
                            id,
                            updateException
                    );
                }
            }
        }
    }

}
