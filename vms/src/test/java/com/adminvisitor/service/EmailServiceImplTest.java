package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.BadgeEmailData;
import com.adminvisitor.dto.responsedto.CancellationEmailData;
import com.adminvisitor.dto.responsedto.HostCheckInEmailData;
import com.adminvisitor.dto.responsedto.HostDailyReminderEmailData;
import com.adminvisitor.entity.Department;
import com.adminvisitor.entity.Employee;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.service.impl.EmailServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailServiceImplTest {

    private NotificationOutboxService outboxService;
    private EmailServiceImpl emailService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        outboxService = mock(NotificationOutboxService.class);
        emailService = new EmailServiceImpl(outboxService);

        ReflectionTestUtils.setField(
                emailService, "photoUploadDir", tempDir.toString()
        );
    }

    @AfterEach
    void cleanUpTransaction() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void sendVisitConfirmationEmail_shouldEnqueueRequest() {
        Visit visit = createVisit();

        emailService.sendVisitConfirmationEmail(visit);

        verify(outboxService).enqueue(argThat(request ->
                request.scenario().equals("VISIT_CONFIRMATION")
                        && request.recipient().equals("visitor@test.com")
                        && request.cc().equals("host@test.com")
                        && request.data().get("visitorName").equals("John Doe")
        ));
    }

    @Test
    void sendVisitConfirmationEmail_shouldEnqueueAfterTransactionCommit() {
        Visit visit = createVisit();

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);

        emailService.sendVisitConfirmationEmail(visit);

        verify(outboxService, never()).enqueue(any());

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(outboxService).enqueue(argThat(request ->
                request.scenario().equals("VISIT_CONFIRMATION")
        ));
    }

    @Test
    void sendVisitConfirmationEmail_shouldNotThrowWhenEnqueueFails() {
        Visit visit = createVisit();
        doThrow(new RuntimeException("Database error"))
                .when(outboxService).enqueue(any());

        assertDoesNotThrow(
                () -> emailService.sendVisitConfirmationEmail(visit)
        );
    }

    @Test
    void sendVisitBadgeEmail_shouldEnqueueRequestWithRelativePhotoPath()
            throws Exception {
        Path photo = tempDir.resolve("visitor.jpg");
        Files.write(photo, new byte[]{1, 2, 3});

        BadgeEmailData data = mock(BadgeEmailData.class);
        when(data.visitorPhoto()).thenReturn(photo.toString());
        when(data.visitorName()).thenReturn("John Doe");
        when(data.visitReference()).thenReturn("VIS-001");
        when(data.visitorEmail()).thenReturn("visitor@test.com");
        when(data.hostEmail()).thenReturn("host@test.com");

        emailService.sendVisitBadgeEmail(data);

        verify(outboxService).enqueue(argThat(request ->
                request.scenario().equals("VISIT_BADGE")
                        && request.data().get("visitorPhotoFileName")
                        .equals("visitor.jpg")
        ));
    }

    @Test
    void sendVisitBadgeEmail_shouldRejectMissingPhoto() {
        BadgeEmailData data = mock(BadgeEmailData.class);
        when(data.visitorPhoto())
                .thenReturn(tempDir.resolve("missing.jpg").toString());

        assertThrows(
                IllegalStateException.class,
                () -> emailService.sendVisitBadgeEmail(data)
        );

        verifyNoInteractions(outboxService);
    }

    @Test
    void sendVisitBadgeEmail_shouldRejectPhotoOutsideConfiguredDirectory()
            throws Exception {
        Path outsideDir = Files.createTempDirectory("outside");
        Path photo = Files.write(outsideDir.resolve("visitor.jpg"),
                new byte[]{1, 2, 3});

        BadgeEmailData data = mock(BadgeEmailData.class);
        when(data.visitorPhoto()).thenReturn(photo.toString());

        assertThrows(
                IllegalStateException.class,
                () -> emailService.sendVisitBadgeEmail(data)
        );

        verifyNoInteractions(outboxService);
    }

    @Test
    void sendVisitBadgeEmail_shouldRejectPhotoLargerThan2MB()
            throws Exception {
        Path photo = tempDir.resolve("large.jpg");

        try (var file = new java.io.RandomAccessFile(photo.toFile(), "rw")) {
            file.setLength(2L * 1024 * 1024 + 1);
        }

        BadgeEmailData data = mock(BadgeEmailData.class);
        when(data.visitorPhoto()).thenReturn(photo.toString());

        assertThrows(
                IllegalStateException.class,
                () -> emailService.sendVisitBadgeEmail(data)
        );

        verifyNoInteractions(outboxService);
    }

    @Test
    void sendVisitBadgeEmail_shouldWrapOutboxFailure() throws Exception {
        Path photo = Files.write(
                tempDir.resolve("visitor.jpg"), new byte[]{1, 2, 3}
        );

        BadgeEmailData data = mock(BadgeEmailData.class);
        when(data.visitorPhoto()).thenReturn(photo.toString());

        doThrow(new RuntimeException("Outbox error"))
                .when(outboxService).enqueue(any());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> emailService.sendVisitBadgeEmail(data)
        );

        assertEquals(
                "Failed to send visitor badge email request",
                exception.getMessage()
        );
    }

    @Test
    void sendHostCheckInNotification_shouldEnqueueRequest() {
        HostCheckInEmailData data = mock(HostCheckInEmailData.class);
        when(data.hostEmail()).thenReturn("host@test.com");
        when(data.visitReference()).thenReturn("VIS-001");

        emailService.sendHostCheckInNotification(data);

        verify(outboxService).enqueue(argThat(request ->
                request.scenario().equals("HOST_CHECK_IN")
                        && request.recipient().equals("host@test.com")
                        && request.cc() == null
        ));
    }

    @Test
    void sendHostCheckInNotification_shouldWrapOutboxFailure() {
        HostCheckInEmailData data = mock(HostCheckInEmailData.class);
        doThrow(new RuntimeException("Outbox error"))
                .when(outboxService).enqueue(any());

        assertThrows(
                IllegalStateException.class,
                () -> emailService.sendHostCheckInNotification(data)
        );
    }

    @Test
    void sendVisitCancellationEmail_shouldEnqueueRequest() {
        CancellationEmailData data = mock(CancellationEmailData.class);
        when(data.visitorEmail()).thenReturn("visitor@test.com");
        when(data.visitReference()).thenReturn("VIS-001");

        emailService.sendVisitCancellationEmail(data);

        verify(outboxService).enqueue(argThat(request ->
                request.scenario().equals("VISIT_CANCELLATION")
                        && request.recipient().equals("visitor@test.com")
                        && request.cc() == null
        ));
    }

    @Test
    void sendVisitCancellationEmail_shouldWrapOutboxFailure() {
        CancellationEmailData data = mock(CancellationEmailData.class);
        doThrow(new RuntimeException("Outbox error"))
                .when(outboxService).enqueue(any());

        assertThrows(
                IllegalStateException.class,
                () -> emailService.sendVisitCancellationEmail(data)
        );
    }

    @Test
    void sendHostDailyReminderEmail_shouldEnqueueRequest() {
        HostDailyReminderEmailData data =
                mock(HostDailyReminderEmailData.class);

        when(data.hostEmail()).thenReturn("host@test.com");
        when(data.visits()).thenReturn(List.of());

        emailService.sendHostDailyReminderEmail(data);

        verify(outboxService).enqueue(argThat(request ->
                request.scenario().equals("HOST_DAILY_REMINDER")
                        && request.recipient().equals("host@test.com")
                        && request.cc() == null
        ));
    }

    @Test
    void sendHostDailyReminderEmail_shouldWrapOutboxFailure() {
        HostDailyReminderEmailData data =
                mock(HostDailyReminderEmailData.class);

        doThrow(new RuntimeException("Outbox error"))
                .when(outboxService).enqueue(any());

        assertThrows(
                IllegalStateException.class,
                () -> emailService.sendHostDailyReminderEmail(data)
        );
    }

    private Visit createVisit() {
        Visit visit = mock(Visit.class);
        Visitor visitor = mock(Visitor.class);
        Employee host = mock(Employee.class);
        Department department = mock(Department.class);

        when(visit.getVisitor()).thenReturn(visitor);
        when(visit.getHost()).thenReturn(host);

        when(visitor.getFirstName()).thenReturn("John");
        when(visitor.getLastName()).thenReturn("Doe");
        when(visitor.getEmail()).thenReturn("visitor@test.com");

        when(host.getFirstName()).thenReturn("Jane");
        when(host.getLastName()).thenReturn("Smith");
        when(host.getEmail()).thenReturn("host@test.com");
        when(host.getDepartment()).thenReturn(department);

        when(department.getDepartmentName()).thenReturn("IT");
        when(visit.getVisitReference()).thenReturn("VIS-001");
        when(visit.getPurpose()).thenReturn("Meeting");
        when(visit.getExpectedArrivalAt())
                .thenReturn(LocalDateTime.now());
        when(visit.getExpectedDepartureAt())
                .thenReturn(LocalDateTime.now().plusHours(1));

        return visit;
    }
}