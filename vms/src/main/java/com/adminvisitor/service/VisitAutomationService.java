package com.adminvisitor.service;

import com.adminvisitor.repository.VisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.enums.VisitStatus;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitAutomationService {

    private final VisitRepository visitRepository;
    private final VisitBadgeService visitBadgeService;

    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void markNoShowVisits() {

        LocalDateTime now = LocalDateTime.now();

        List<Visit> visits =
                visitRepository.findByStatusAndExpectedArrivalAtBefore(
                        VisitStatus.REGISTERED,
                        now
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

    @Scheduled(cron = "0 59 23 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void autoCheckOutVisits() {

        LocalDateTime now = LocalDateTime.now();

        List<Visit> visits =
                visitRepository.findByStatusAndCheckedInAtBefore(
                        VisitStatus.CHECKED_IN,
                        now
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
}