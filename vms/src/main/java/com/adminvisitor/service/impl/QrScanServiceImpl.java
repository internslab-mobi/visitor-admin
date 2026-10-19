package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.QrScanResponse;
import com.adminvisitor.entity.MetaTimeTracking;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.repository.MetaTimeTrackingRepository;
import com.adminvisitor.service.QrScanService;
import com.adminvisitor.service.VisitBadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class QrScanServiceImpl implements QrScanService {

    private final VisitBadgeService visitBadgeService;
    private final MetaTimeTrackingRepository trackingRepository;

    @Override
    @Transactional
    public QrScanResponse processScan(String token) {

        //VisitBadge badge = visitBadgeService.validateQrToken(token);

        VisitBadge badge = visitBadgeService.validateQrTokenForUpdate(token);
        Visit visit = badge.getVisit();

        if (visit.getStatus() != VisitStatus.CHECKED_IN) {
            throw new BusinessRuleException(
                    "Only checked-in visits can be scanned"
            );
        }

        LocalDateTime scannedAt = LocalDateTime.now();

        // Find the latest tracking session for this visit.
        MetaTimeTracking latestSession = trackingRepository
                .findTopByVisitIdOrderByInTimeDesc(visit.getId())
                .orElse(null);

        // Reject repeated scans within 60 seconds.
        if (latestSession != null) {

            LocalDateTime lastScanAt =
                    latestSession.getOutTime() != null
                            ? latestSession.getOutTime()
                            : latestSession.getInTime();

            if (lastScanAt != null &&
                    scannedAt.isBefore(
                            lastScanAt.plusSeconds(60)
                    )) {

                throw new BusinessRuleException(
                        "Duplicate scan rejected. Please wait 60 seconds before scanning again."
                );
            }
        }

        // Find an active IN session.
        MetaTimeTracking openSession = trackingRepository
                .findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc(
                        visit.getId()
                )
                .orElse(null);

        // An active session means the visitor is currently IN.
        // The next accepted scan records OUT.
        if (openSession != null) {

            openSession.setOutTime(scannedAt);
            trackingRepository.save(openSession);

            return new QrScanResponse(
                    "Visitor temporarily checked out",
                    "OUT",
                    scannedAt
            );
        }

        // No open session means the next accepted scan records IN.
        MetaTimeTracking newSession = new MetaTimeTracking();

        newSession.setVisitorId(visit.getVisitor().getId());
        newSession.setVisitId(visit.getId());
        newSession.setBadgeId(badge.getId());
        newSession.setInTime(scannedAt);

        trackingRepository.save(newSession);

        return new QrScanResponse(
                "Visitor checked in again",
                "IN",
                scannedAt
        );
    }
}
