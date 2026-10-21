package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.HostDailyReminderEmailData;
import com.adminvisitor.entity.Employee;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.service.EmailService;
import com.adminvisitor.service.VisitAutomationService;
import com.adminvisitor.service.VisitBadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitAutomationServiceImpl implements VisitAutomationService {

    private final VisitRepository visitRepository;
    private final VisitBadgeService visitBadgeService;
    private final EmailService emailService;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void markNoShowVisits() {

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        List<Visit> visits =
                visitRepository.findByStatusAndExpectedArrivalAtBefore(
                        VisitStatus.REGISTERED,
                        startOfToday
                );

        for (Visit visit : visits) {
            visit.setStatus(VisitStatus.NO_SHOW);
        }

        if (!visits.isEmpty()) {
            visitRepository.saveAll(visits);

            log.info(
                    "VISITS_MARKED_NO_SHOW count={}",
                    visits.size()
            );
        } else {
            log.debug("VISITS_NO_SHOW_NONE_FOUND");
        }
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCheckOutVisits() {

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        List<Visit> visits =
                visitRepository.findByStatusAndExpectedArrivalAtBefore(
                        VisitStatus.CHECKED_IN,
                        startOfToday
                );

        for (Visit visit : visits) {

            visit.setCheckedOutAt(now);
            visit.setStatus(VisitStatus.CHECKED_OUT);

            visitBadgeService.invalidateBadgeOnCheckout(
                    visit.getId(),
                    now
            );
        }

        if (!visits.isEmpty()) {
            visitRepository.saveAll(visits);

            log.info(
                    "VISITS_AUTOMATICALLY_CHECKED_OUT count={}",
                    visits.size()
            );
        } else {
            log.debug("VISITS_AUTO_CHECKOUT_NONE_FOUND");
        }
    }

    @Scheduled(
            cron = "0 0 10 * * *",
            zone = "Asia/Kolkata"
    )
    @Transactional(readOnly = true)
    @Override
    public void sendHostDailyVisitReminders() {

        ZoneId zoneId = ZoneId.of("Asia/Kolkata");
        LocalDate today = LocalDate.now(zoneId);

        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        List<Visit> visits =
                visitRepository
                        .findByStatusAndExpectedArrivalAtGreaterThanEqualAndExpectedArrivalAtLessThan(
                                VisitStatus.REGISTERED,
                                startOfToday,
                                startOfTomorrow
                        );

        if (visits.isEmpty()) {
            log.debug(
                    "HOST_DAILY_REMINDERS_NO_VISITS date={}",
                    today
            );
            return;
        }

        Map<Employee, List<Visit>> visitsByHost =
                visits.stream()
                        .collect(Collectors.groupingBy(Visit::getHost));

        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("dd MMM yyyy");

        DateTimeFormatter timeFormatter =
                DateTimeFormatter.ofPattern("hh:mm a");

        int remindersQueued = 0;

        for (Map.Entry<Employee, List<Visit>> entry : visitsByHost.entrySet()) {

            Employee host = entry.getKey();

            List<HostDailyReminderEmailData.VisitReminderItem> reminderItems =
                    entry.getValue().stream()
                            .map(visit ->
                                    new HostDailyReminderEmailData.VisitReminderItem(
                                            visit.getVisitor().getFirstName()
                                                    + " "
                                                    + visit.getVisitor().getLastName(),
                                            visit.getVisitReference(),
                                            visit.getPurpose(),
                                            visit.getExpectedArrivalAt().format(timeFormatter),
                                            visit.getExpectedDepartureAt() == null
                                                    ? "Not specified"
                                                    : visit.getExpectedDepartureAt().format(timeFormatter),
                                            visit.getVisitorType().name()
                                    )
                            )
                            .toList();

            HostDailyReminderEmailData reminderData =
                    new HostDailyReminderEmailData(
                            host.getEmail(),
                            host.getFirstName() + " " + host.getLastName(),
                            today.format(dateFormatter),
                            reminderItems
                    );

            emailService.sendHostDailyReminderEmail(reminderData);
            remindersQueued++;

            log.info(
                    "HOST_DAILY_REMINDER_SUBMITTED hostId={} visitCount={} date={}",
                    host.getId(),
                    reminderItems.size(),
                    today
            );
        }

        log.info(
                "HOST_DAILY_REMINDER_BATCH_COMPLETED date={} visitCount={} hostCount={}",
                today,
                visits.size(),
                remindersQueued
        );
    }
}