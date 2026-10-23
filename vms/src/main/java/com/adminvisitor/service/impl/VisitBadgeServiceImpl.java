//package com.adminvisitor.service.impl;
//
//import com.adminvisitor.dto.responsedto.QrScanResponse;
//import com.adminvisitor.entity.MetaTimeTracking;
//import com.adminvisitor.entity.Visit;
//import com.adminvisitor.entity.VisitBadge;
//import com.adminvisitor.enums.BadgeStatus;
//import com.adminvisitor.enums.VisitStatus;
//import com.adminvisitor.exception.BusinessRuleException;
//import com.adminvisitor.exception.ResourceAlreadyExistsException;
//import com.adminvisitor.repository.MetaTimeTrackingRepository;
//import com.adminvisitor.repository.VisitBadgeRepository;
//import com.adminvisitor.service.VisitBadgeService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class VisitBadgeServiceImpl implements VisitBadgeService {
//
//    private final VisitBadgeRepository visitBadgeRepository;
//    private final IdGeneratorServiceImpl idGeneratorService;
//    private final MetaTimeTrackingRepository trackingRepository;
//
//    @Override
//    @Transactional
//    public VisitBadge createBadge(Visit visit, String qrToken) {
//
//        if (visitBadgeRepository.findByVisit_Id(visit.getId()).isPresent()) {
//            throw new ResourceAlreadyExistsException(
//                    "A visitor badge already exists for visit: "
//                            + visit.getId()
//            );
//        }
//
//        if (qrToken == null || qrToken.isBlank()) {
//            throw new IllegalArgumentException(
//                    "QR token must not be empty"
//            );
//        }
//
//        String badgeId = idGeneratorService.generateId(
//                "VISITBADGE",
//                "VB"
//        );
//
//        LocalDateTime issuedAt = LocalDateTime.now();
//
//        LocalDateTime validUntil = visit.getExpectedArrivalAt()
//                .toLocalDate()
//                .atTime(23, 59, 59);
//
//        VisitBadge badge = new VisitBadge();
//
//        badge.setId(badgeId);
//        badge.setVisit(visit);
//        badge.setQrContainingToken(qrToken);
//        badge.setIssuedAt(issuedAt);
//        badge.setValidUntil(validUntil);
//        badge.setStatus(BadgeStatus.ACTIVE);
//
//        VisitBadge savedBadge = visitBadgeRepository.save(badge);
//
//        log.info(
//                "VISIT_BADGE_CREATED badgeId={} visitId={} validUntil={}",
//                savedBadge.getId(),
//                visit.getId(),
//                validUntil
//        );
//
//        return savedBadge;
//    }
//
//
//
//    @Override
//    @Transactional
//    public QrScanResponse processScan(String token) {
//
//        VisitBadge badge =validateQrTokenForUpdate(token);
//        Visit visit = badge.getVisit();
//
//        if (visit.getStatus() != VisitStatus.CHECKED_IN) {
//            throw new BusinessRuleException(
//                    "Only checked-in visits can be scanned"
//            );
//        }
//
//        LocalDateTime scannedAt = LocalDateTime.now();
//
//        // Find the latest tracking session for this visit.
//        MetaTimeTracking latestSession = trackingRepository
//                .findTopByVisitIdOrderByInTimeDesc(visit.getId())
//                .orElse(null);
//
//        // Reject repeated scans within 60 seconds.
//        if (latestSession != null) {
//
//            LocalDateTime lastScanAt =
//                    latestSession.getOutTime() != null
//                            ? latestSession.getOutTime()
//                            : latestSession.getInTime();
//
//            if (lastScanAt != null &&
//                    scannedAt.isBefore(lastScanAt.plusSeconds(60))) {
//
//                throw new BusinessRuleException(
//                        "Duplicate scan rejected. Please wait 60 seconds before scanning again."
//                );
//            }
//        }
//
//        // Find an active IN session.
//        MetaTimeTracking openSession = trackingRepository
//                .findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc(
//                        visit.getId()
//                )
//                .orElse(null);
//
//        // An active session means the visitor is currently IN.
//        // The next accepted scan records OUT.
//        if (openSession != null) {
//
//            openSession.setOutTime(scannedAt);
//            trackingRepository.save(openSession);
//
//            log.info(
//                    "QR_SCAN_RECORDED action=OUT visitId={} trackingId={}",
//                    visit.getId(),
//                    openSession.getId()
//            );
//
//            return new QrScanResponse(
//                    "Visitor temporarily checked out",
//                    "OUT",
//                    scannedAt
//            );
//        }
//
//        // No open session means the next accepted scan records IN.
//        MetaTimeTracking newSession = new MetaTimeTracking();
//
//        newSession.setVisitorId(visit.getVisitor().getId());
//        newSession.setVisitId(visit.getId());
//        newSession.setBadgeId(badge.getId());
//        newSession.setInTime(scannedAt);
//
//        trackingRepository.save(newSession);
//
//        log.info(
//                "QR_SCAN_RECORDED action=IN visitId={} trackingId={}",
//                visit.getId(),
//                newSession.getId()
//        );
//
//        return new QrScanResponse(
//                "Visitor checked in again",
//                "IN",
//                scannedAt
//        );
//    }
//
//    @Override
//    @Transactional
//    public void invalidateBadgeOnCheckout(
//            String visitId,
//            LocalDateTime checkoutTime
//    ) {
//
//        VisitBadge badge = visitBadgeRepository.findByVisit_Id(visitId)
//                .orElse(null);
//
//        if (badge == null) {
//            log.debug(
//                    "VISIT_BADGE_INVALIDATION_SKIPPED visitId={} reason=BADGE_NOT_FOUND",
//                    visitId
//            );
//            return;
//        }
//
//        badge.setValidUntil(checkoutTime);
//        badge.setStatus(BadgeStatus.INVALID);
//
//        visitBadgeRepository.save(badge);
//
//        log.info(
//                "VISIT_BADGE_INVALIDATED badgeId={} visitId={} checkoutTime={}",
//                badge.getId(),
//                visitId,
//                checkoutTime
//        );
//    }
//
//    @Override
//    @Transactional(readOnly = true)
//    public VisitBadge validateQrTokenForUpdate(String qrToken) {
//
//        VisitBadge badge = visitBadgeRepository
//                .findByQrContainingToken(qrToken)
//                .orElseThrow(() ->
//                        new IllegalArgumentException("Invalid QR code")
//                );
//
//        LocalDateTime now = LocalDateTime.now();
//
//        if (badge.getStatus() != BadgeStatus.ACTIVE) {
//            throw new IllegalArgumentException(
//                    "QR code is no longer valid"
//            );
//        }
//
//        if (now.isAfter(badge.getValidUntil())) {
//            throw new IllegalArgumentException(
//                    "QR code has expired"
//            );
//        }
//
//        return badge;
//    }
//}