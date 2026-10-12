package com.adminvisitor.service.impl;

import com.adminvisitor.entity.Visit;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.service.VisitAutomationService;
import com.adminvisitor.dto.responsedto.HostDailyReminderEmailData;
import com.adminvisitor.entity.Employee;
import com.adminvisitor.service.EmailService;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

import com.adminvisitor.service.VisitBadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitAutomationServiceImpl
        implements VisitAutomationService {

    private final VisitRepository visitRepository;
    private final VisitBadgeService visitBadgeService;
    private final EmailService emailService;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void markNoShowVisits() {

        LocalDateTime startOfToday =
                LocalDate.now().atStartOfDay();

        List<Visit> visits =
                visitRepository.findByStatusAndExpectedArrivalAtBefore(
                        VisitStatus.REGISTERED,
                        startOfToday
                );

        for (Visit visit : visits) {

            visit.setStatus(VisitStatus.NO_SHOW);

            log.info(
                    "Visit marked as NO_SHOW. visitId={}, visitReference={}",
                    visit.getId(),
                    visit.getVisitReference()
            );
        }

        visitRepository.saveAll(visits);
    }

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void autoCheckOutVisits() {

        LocalDateTime startOfToday =
                LocalDate.now().atStartOfDay();

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

            log.info(
                    "Visit automatically checked out. visitId={}, visitReference={}",
                    visit.getId(),
                    visit.getVisitReference()
            );
        }

        visitRepository.saveAll(visits);
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
            log.info(
                    "No registered visits found for today's host reminders. date={}",
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

            log.info(
                    "Host daily reminder queued. hostId={}, visitCount={}, date={}",
                    host.getId(),
                    reminderItems.size(),
                    today
            );
        }
    }
}