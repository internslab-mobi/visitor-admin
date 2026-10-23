package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.BadgeEmailData;
import com.adminvisitor.dto.responsedto.CancellationEmailData;
import com.adminvisitor.dto.responsedto.HostCheckInEmailData;
import com.adminvisitor.dto.responsedto.HostDailyReminderEmailData;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.service.EmailService;
import com.adminvisitor.service.NotificationOutboxService;
import com.adminvisitor.util.DateTimeFormats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.adminvisitor.dto.requestdto.NotificationRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {
    private final NotificationOutboxService notificationOutboxService;

    private static final long MAX_VISITOR_PHOTO_SIZE = 2 * 1024 * 1024; // 2 MB

    @Async
    @Override
    public void sendVisitConfirmationEmail(Visit visit) {

        String visitorName =
                visit.getVisitor().getFirstName() + " "
                        + visit.getVisitor().getLastName();

        String hostName =
                visit.getHost().getFirstName() + " "
                        + visit.getHost().getLastName();

        String hostEmail = visit.getHost().getEmail();

        String departmentName =
                visit.getHost().getDepartment().getDepartmentName();

        NotificationRequest request = new NotificationRequest(
                UUID.randomUUID().toString(),
                "VISIT_CONFIRMATION",
                visit.getVisitor().getEmail(),
                hostEmail,
                Map.of(
                        "visitorName", visitorName,
                        "visitReference", visit.getVisitReference(),
                        "purpose", visit.getPurpose(),
                        "expectedArrival",
                        visit.getExpectedArrivalAt()
                                .format(DateTimeFormats.DISPLAY_DATE_TIME),
                        "expectedDeparture",
                        visit.getExpectedDepartureAt()
                                .format(DateTimeFormats.DISPLAY_DATE_TIME),
                        "hostName", hostName,
                        "departmentName", departmentName
                )
        );

        Runnable sendNotification = () -> {
            try {
                notificationOutboxService.enqueue(request);

                log.info(
                        "NOTIFICATION_ENQUEUE_REQUESTED scenario=VISIT_CONFIRMATION visitReference={}",
                        visit.getVisitReference()
                );
            } catch (RuntimeException exception) {
                log.error(
                        "NOTIFICATION_ENQUEUE_FAILED scenario=VISIT_CONFIRMATION visitReference={}",
                        visit.getVisitReference(),
                        exception
                );
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            sendNotification.run();
                        }
                    }
            );

            log.debug(
                    "NOTIFICATION_ENQUEUE_DEFERRED scenario=VISIT_CONFIRMATION visitReference={}",
                    visit.getVisitReference()
            );

        } else {
            sendNotification.run();
        }

    }

    @Async
    @Override
    public void sendVisitBadgeEmail(BadgeEmailData data) {
        try {
            Path photoPath = Path.of(data.visitorPhoto())
                    .toAbsolutePath().normalize();

            // Validate the photo before reading it into memory.
            if (!Files.isRegularFile(photoPath) || !Files.isReadable(photoPath)) {
                throw new IllegalStateException(
                        "Visitor photo is missing or unreadable"
                );
            }

            long photoSize = Files.size(photoPath);

            if (photoSize > MAX_VISITOR_PHOTO_SIZE) {
                throw new IllegalArgumentException(
                        "Visitor photo exceeds the maximum allowed size of 2 MB"
                );
            }

            if (!Files.isRegularFile(photoPath) || !Files.isReadable(photoPath)) {
                throw new IllegalArgumentException(
                        "Visitor photo does not exist or is not readable"
                );
            }

            if (Files.size(photoPath) > 2 * 1024 * 1024) {
                throw new IllegalArgumentException(
                        "Visitor photo must not exceed 2 MB"
                );
            }

            // Store only the filename in the outbox payload.
            String visitorPhotoFileName = photoPath.getFileName().toString();

            Map<String, Object> emailData = new HashMap<>();
            emailData.put("visitorName", data.visitorName());
            emailData.put("visitReference", data.visitReference());
            emailData.put("hostName", data.hostName());
            emailData.put("issuedAt", data.issuedAt());
            emailData.put("validUntil", data.validUntil());
            emailData.put("qrCode", data.qrCode());
            emailData.put("visitorPhotoFileName", visitorPhotoFileName);

            NotificationRequest request = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "VISIT_BADGE",
                    data.visitorEmail(),
                    data.hostEmail(),
                    emailData
            );

            notificationOutboxService.enqueue(request);

            log.info(
                    "NOTIFICATION_ENQUEUED scenario=VISIT_BADGE visitReference={} photoSizeBytes={}",
                    data.visitReference(),
                    data.visitorEmail()
            );

        } catch (IOException | RuntimeException exception) {
            log.error(
                    "NOTIFICATION_ENQUEUE_FAILED scenario=VISIT_BADGE visitReference={}",
                    data.visitReference(),
                    data.visitorEmail(),
                    exception
            );

            throw new IllegalStateException(
                    "Failed to send visitor badge email request",
                    exception
            );
        }
    }

    @Async
    @Override
    public void sendHostCheckInNotification(HostCheckInEmailData data) {
        try {
            Map<String, Object> emailData = new HashMap<>();

            emailData.put("hostName", data.hostName());
            emailData.put("visitorName", data.visitorName());
            emailData.put("visitorType", data.visitorType());
            emailData.put("companyName", data.companyName());
            emailData.put("visitReference", data.visitReference());
            emailData.put("purpose", data.purpose());
            emailData.put("checkedInAt", data.checkedInAt());

            NotificationRequest request = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "HOST_CHECK_IN",
                    data.hostEmail(),
                    null,
                    emailData
            );

            notificationOutboxService.enqueue(request);

            log.info(
                    "NOTIFICATION_ENQUEUED scenario=HOST_CHECK_IN visitReference={}",
                    data.visitReference()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "NOTIFICATION_ENQUEUE_FAILED scenario=HOST_CHECK_IN visitReference={}",
                    data.visitReference(),
                    data.hostEmail(),
                    exception
            );

            throw new IllegalStateException(
                    "Failed to send host check-in notification request",
                    exception
            );
        }
    }

    @Async
    @Override
    public void sendVisitCancellationEmail(CancellationEmailData data) {
        try {
            Map<String, Object> emailData = new HashMap<>();

            emailData.put("visitorName", data.visitorName());
            emailData.put("visitReference", data.visitReference());
            emailData.put("purpose", data.purpose());
            emailData.put("expectedArrival", data.expectedArrival());
            emailData.put("expectedDeparture", data.expectedDeparture());
            emailData.put("hostName", data.hostName());

            NotificationRequest request = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "VISIT_CANCELLATION",
                    data.visitorEmail(),
                    null,
                    emailData
            );

            notificationOutboxService.enqueue(request);

            log.info(
                    "NOTIFICATION_ENQUEUED scenario=VISIT_CANCELLATION visitReference={}",
                    data.visitReference()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "NOTIFICATION_ENQUEUE_FAILED scenario=VISIT_CANCELLATION visitReference={}",
                    data.visitReference(),
                    data.visitorEmail(),
                    exception
            );

            throw new IllegalStateException(
                    "Failed to send visit cancellation notification request",
                    exception
            );
        }
    }

    @Async
    @Override
    public void sendHostDailyReminderEmail(HostDailyReminderEmailData data) {
        try {
            Map<String, Object> emailData = new HashMap<>();

            emailData.put("hostName", data.hostName());
            emailData.put("reminderDate", data.reminderDate());
            emailData.put("visits", data.visits());

            NotificationRequest request = new NotificationRequest(
                    UUID.randomUUID().toString(),
                    "HOST_DAILY_REMINDER",
                    data.hostEmail(),
                    null,
                    emailData
            );

            notificationOutboxService.enqueue(request);

            log.info(
                    "NOTIFICATION_ENQUEUED scenario=HOST_DAILY_REMINDER reminderDate={} visitCount={}",
                    data.reminderDate(),
                    data.visits().size()
            );

        } catch (RuntimeException exception) {
            log.error(
                    "NOTIFICATION_ENQUEUE_FAILED scenario=HOST_DAILY_REMINDER reminderDate={}",
                    data.reminderDate(),
                    exception
            );

            throw new IllegalStateException(
                    "Failed to send host daily reminder request",
                    exception
            );
        }
    }
}
