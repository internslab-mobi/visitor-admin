
package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.NotificationRequest;
import com.adminvisitor.entity.NotificationOutbox;
import com.adminvisitor.enums.NotificationOutboxStatus;
import com.adminvisitor.enums.NotificationScenario;
import com.adminvisitor.repository.NotificationOutboxRepository;
import com.adminvisitor.service.IdGeneratorService;
import com.adminvisitor.service.NotificationOutboxService;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationOutboxServiceImpl implements NotificationOutboxService {

    private static final int MAX_ATTEMPTS = 3;

    private final NotificationOutboxRepository notificationOutboxRepository;
    private final IdGeneratorService idGeneratorService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void enqueue(NotificationRequest request) {
        if (request.notificationId() == null
                || request.notificationId().isBlank()) {
            throw new IllegalArgumentException(
                    "Notification ID must not be blank"
            );
        }
        if (notificationOutboxRepository
                .existsByNotificationId(request.notificationId())) {
            return;
        }
        NotificationOutbox outbox = new NotificationOutbox();

        outbox.setId(
                idGeneratorService.generateId("NOTIFICATION_OUTBOX", "NTF")
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
        outbox.setMaxAttempts(MAX_ATTEMPTS);
        outbox.setNextAttemptAt(LocalDateTime.now());

        notificationOutboxRepository.save(outbox);
    }


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

}
