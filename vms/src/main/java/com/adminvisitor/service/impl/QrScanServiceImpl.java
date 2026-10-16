package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.QrScanResponse;
import com.adminvisitor.entity.MetaTimeTracking;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.enums.VisitStatus;
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

        VisitBadge badge = visitBadgeService.validateQrToken(token);
        Visit visit = badge.getVisit();

        if (visit.getStatus() != VisitStatus.CHECKED_IN) {
            throw new IllegalStateException(
                    "Only checked-in visits can be scanned"
            );
        }

        LocalDateTime scannedAt = LocalDateTime.now();

        MetaTimeTracking openSession = trackingRepository
                .findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc(
                        visit.getId()
                )
                .orElse(null);

        if (openSession != null) {

            openSession.setOutTime(scannedAt);
            trackingRepository.save(openSession);

            return new QrScanResponse(
                    "Visitor temporarily checked out",
                    "OUT",
                    scannedAt
            );
        }

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
