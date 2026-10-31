package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.NotificationRequest;
import com.adminvisitor.entity.NotificationOutbox;
import com.adminvisitor.enums.NotificationOutboxStatus;
import com.adminvisitor.enums.NotificationScenario;
import com.adminvisitor.repository.NotificationOutboxRepository;
import com.adminvisitor.service.impl.NotificationOutboxServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NotificationOutboxServiceImplTest {

    private NotificationOutboxRepository repository;
    private IdGeneratorService idGenerator;
    private NotificationClientService client;
    private PlatformTransactionManager transactionManager;
    private NotificationOutboxServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationOutboxRepository.class);
        idGenerator = mock(IdGeneratorService.class);
        client = mock(NotificationClientService.class);
        transactionManager = mock(PlatformTransactionManager.class);

        when(transactionManager.getTransaction(
                any(TransactionDefinition.class)))
                .thenReturn(mock(TransactionStatus.class));

        service = new NotificationOutboxServiceImpl(
                repository,
                idGenerator,
                client,
                new ObjectMapper(),
                transactionManager
        );

        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "maxAttempts", 3
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "batchSize", 10
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                service, "retryDelaySeconds", 30L
        );
    }

    private NotificationRequest createRequest() {
        return new NotificationRequest(
                "notification-1",
                "VISIT_CONFIRMATION",
                "visitor@test.com",
                "host@test.com",
                Map.of("visitorName", "John")
        );
    }

    private NotificationOutbox createOutbox() {
        NotificationOutbox outbox = new NotificationOutbox();
        outbox.setId("NTF-001");
        outbox.setNotificationId("notification-1");
        outbox.setScenario(NotificationScenario.VISIT_CONFIRMATION);
        outbox.setRecipient("visitor@test.com");
        outbox.setCc("host@test.com");
        outbox.setPayload("{\"visitorName\":\"John\"}");
        outbox.setStatus(NotificationOutboxStatus.PENDING);
        outbox.setAttemptCount(1);
        outbox.setMaxAttempts(3);
        outbox.setNextAttemptAt(LocalDateTime.now());
        return outbox;
    }

    @Test
    void enqueue_shouldSaveOutboxSuccessfully() {
        when(repository.existsByNotificationId("notification-1"))
                .thenReturn(false);
        when(idGenerator.generateId("NOTIFICATION_OUTBOX", "NTF"))
                .thenReturn("NTF-001");
        when(repository.save(any(NotificationOutbox.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.enqueue(createRequest());

        verify(repository).save(argThat(outbox ->
                outbox.getId().equals("NTF-001")
                        && outbox.getStatus() == NotificationOutboxStatus.PENDING
                        && outbox.getAttemptCount() == 0
                        && outbox.getMaxAttempts() == 3
                        && outbox.getPayload().contains("John")
        ));
    }

    @Test
    void enqueue_shouldRejectNullRequest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.enqueue(null)
        );

        verifyNoInteractions(idGenerator);
    }

    @Test
    void enqueue_shouldRejectBlankNotificationId() {
        NotificationRequest request = new NotificationRequest(
                " ", "VISIT_CONFIRMATION", "visitor@test.com",
                null, Map.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.enqueue(request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void enqueue_shouldSkipDuplicateNotification() {
        when(repository.existsByNotificationId("notification-1"))
                .thenReturn(true);

        service.enqueue(createRequest());

        verify(repository, never()).save(any());
        verifyNoInteractions(idGenerator);
    }

    @Test
    void enqueue_shouldRejectUnknownScenario() {
        when(repository.existsByNotificationId("notification-1"))
                .thenReturn(false);

        NotificationRequest request = new NotificationRequest(
                "notification-1", "UNKNOWN_SCENARIO",
                "visitor@test.com", null, Map.of()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.enqueue(request)
        );

        verify(repository, never()).save(any());
    }

    @Test
    void claim_shouldReturnTrueWhenOneRecordIsUpdated() {
        when(repository.claimPendingNotification(
                eq("NTF-001"),
                eq(NotificationOutboxStatus.PENDING),
                eq(NotificationOutboxStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(1);

        assertTrue(service.claim("NTF-001"));
    }

    @Test
    void claim_shouldReturnFalseWhenNoRecordIsUpdated() {
        when(repository.claimPendingNotification(
                anyString(), any(), any(), any(LocalDateTime.class)
        )).thenReturn(0);

        assertFalse(service.claim("NTF-001"));
    }

    @Test
    void markSent_shouldUpdateStatusAndClearError() {
        NotificationOutbox outbox = createOutbox();
        outbox.setLastError("Previous error");

        when(repository.findById("NTF-001"))
                .thenReturn(Optional.of(outbox));

        service.markSent("NTF-001");

        assertEquals(NotificationOutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getSentAt());
        assertNull(outbox.getLastError());

        verify(repository).save(outbox);
    }

    @Test
    void markSent_shouldThrowWhenRecordDoesNotExist() {
        when(repository.findById("NTF-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.markSent("NTF-001")
        );
    }


    @Test
    void markFailed_shouldScheduleRetry() {
        NotificationOutbox outbox = createOutbox();
        outbox.setAttemptCount(1);
        outbox.setMaxAttempts(3);

        when(repository.findById("NTF-001"))
                .thenReturn(Optional.of(outbox));

        service.markFailed("NTF-001", "Connection refused");

        assertEquals(NotificationOutboxStatus.PENDING, outbox.getStatus());
        assertEquals("Notification service is unreachable", outbox.getLastError());
        assertNotNull(outbox.getNextAttemptAt());

        verify(repository).findById("NTF-001");
        verify(repository, never()).save(any());
    }

    @Test
    void markFailed_shouldMarkPermanentlyFailedAtMaxAttempts() {
        NotificationOutbox outbox = createOutbox();
        outbox.setAttemptCount(3);
        outbox.setMaxAttempts(3);

        when(repository.findById("NTF-001"))
                .thenReturn(Optional.of(outbox));

        service.markFailed("NTF-001", "Connection refused");

        assertEquals(NotificationOutboxStatus.FAILED, outbox.getStatus());
        assertEquals("Notification service is unreachable", outbox.getLastError());

        verify(repository).findById("NTF-001");
        verify(repository, never()).save(any());
    }


    @Test
    void markFailed_shouldHandleBlankError() {
        NotificationOutbox outbox = createOutbox();

        when(repository.findById("NTF-001"))
                .thenReturn(Optional.of(outbox));

        service.markFailed("NTF-001", " ");

        assertEquals(
                "Notification delivery failed",
                outbox.getLastError()
        );
    }

    @Test
    void markFailed_shouldNormalizeConnectionErrors() {
        NotificationOutbox outbox = createOutbox();

        when(repository.findById("NTF-001"))
                .thenReturn(Optional.of(outbox));

        service.markFailed("NTF-001", "Connection refused");

        assertEquals(
                "Notification service is unreachable",
                outbox.getLastError()
        );
    }

    @Test
    void processPendingNotifications_shouldDoNothingWhenEmpty() {
        when(repository.findReadyToProcess(
                eq(NotificationOutboxStatus.PENDING),
                any(LocalDateTime.class),
                any(PageRequest.class)
        )).thenReturn(List.of());

        service.processPendingNotifications();

        verifyNoInteractions(client);
    }

    @Test
    void processPendingNotifications_shouldSendAndMarkSent() {
        NotificationOutbox outbox = createOutbox();

        when(repository.findReadyToProcess(
                eq(NotificationOutboxStatus.PENDING),
                any(LocalDateTime.class),
                any(PageRequest.class)
        )).thenReturn(List.of(outbox));

        when(repository.claimPendingNotification(
                eq("NTF-001"),
                eq(NotificationOutboxStatus.PENDING),
                eq(NotificationOutboxStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(1);

        when(repository.findById("NTF-001"))
                .thenReturn(Optional.of(outbox));

        service.processPendingNotifications();

        verify(client).sendEmail(argThat(request ->
                request.notificationId().equals("notification-1")
                        && request.scenario().equals("VISIT_CONFIRMATION")
                        && request.recipient().equals("visitor@test.com")
                        && request.data().get("visitorName").equals("John")
        ));

        assertEquals(NotificationOutboxStatus.SENT, outbox.getStatus());
    }

    @Test
    void processPendingNotifications_shouldNotSendWhenClaimFails() {
        NotificationOutbox outbox = createOutbox();

        when(repository.findReadyToProcess(
                eq(NotificationOutboxStatus.PENDING),
                any(LocalDateTime.class),
                any(PageRequest.class)
        )).thenReturn(List.of(outbox));

        when(repository.claimPendingNotification(
                eq("NTF-001"),
                eq(NotificationOutboxStatus.PENDING),
                eq(NotificationOutboxStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(0);

        service.processPendingNotifications();

        verifyNoInteractions(client);
    }

    @Test
    void processPendingNotifications_shouldRecordFailureWhenDeliveryFails() {
        NotificationOutbox outbox = createOutbox();

        when(repository.findReadyToProcess(
                eq(NotificationOutboxStatus.PENDING),
                any(LocalDateTime.class),
                any(PageRequest.class)
        )).thenReturn(List.of(outbox));

        when(repository.claimPendingNotification(
                eq("NTF-001"),
                eq(NotificationOutboxStatus.PENDING),
                eq(NotificationOutboxStatus.PROCESSING),
                any(LocalDateTime.class)
        )).thenReturn(1);

        when(repository.findById("NTF-001"))
                .thenReturn(Optional.of(outbox));

        doThrow(new RuntimeException("Connection refused"))
                .when(client).sendEmail(any(NotificationRequest.class));

        service.processPendingNotifications();

        assertEquals(NotificationOutboxStatus.PENDING, outbox.getStatus());
        assertEquals(
                "Notification service is unreachable",
                outbox.getLastError()
        );
    }
}