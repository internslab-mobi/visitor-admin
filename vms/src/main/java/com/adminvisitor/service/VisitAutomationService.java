package com.adminvisitor.service;

import com.adminvisitor.entity.Visit;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.repository.VisitRepository;
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
public class VisitAutomationService {

    private final VisitRepository visitRepository;
    private final VisitBadgeService visitBadgeService;

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
}