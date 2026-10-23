package com.qrservice.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;

import com.qrservice.dto.QrBadgeRequest;
import com.qrservice.dto.QrBadgeResponse;
import com.qrservice.dto.QrScanResponse;
import com.qrservice.entity.MetaTimeTracking;
import com.qrservice.entity.VisitBadge;
import com.qrservice.enums.BadgeStatus;
import com.qrservice.repository.MetaTimeTrackingRepository;
import com.qrservice.repository.VisitBadgeRepository;
import com.qrservice.service.IdGeneratorService;
import com.qrservice.service.QrService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import com.qrservice.dto.CheckoutBadgeInvalidationRequest;
@Service
@RequiredArgsConstructor
@Slf4j
public class QrServiceImpl implements QrService {

    private static final long DUPLICATE_SCAN_COOLDOWN_SECONDS = 60;

    private final IdGeneratorService idGeneratorService;
    private final VisitBadgeRepository visitBadgeRepository;
    private final MetaTimeTrackingRepository trackingRepository;

    private final SecureRandom secureRandom = new SecureRandom();


    @Override
    @Transactional
    public QrBadgeResponse generateQrBadge(QrBadgeRequest request) {

        if (request == null
                || request.visitId() == null
                || request.visitId().isBlank()
                || request.visitorId() == null
                || request.visitorId().isBlank()
                || request.expectedArrivalAt() == null
                || request.checkedInAt() == null) {

            throw new IllegalArgumentException(
                    "Visit ID, visitor ID, expected arrival time "
                            + "and check-in time are required"
            );
        }

        String visitId = request.visitId().trim();
        String visitorId = request.visitorId().trim();

        if (visitBadgeRepository.findByVisitId(visitId).isPresent()) {
            throw new IllegalStateException(
                    "A badge already exists for visit: " + visitId
            );
        }

        LocalDateTime checkedInAt = request.checkedInAt();

        if (checkedInAt.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Check-in time cannot be in the future"
            );
        }

        String token = generateToken();
        String qrCode = generateQrCode(token);

        VisitBadge badge = new VisitBadge();

        badge.setId(
                idGeneratorService.generateId("VISITBADGE", "VB")
        );
        badge.setVisitId(visitId);
        badge.setQrContainingToken(token);
        badge.setIssuedAt(checkedInAt);
        badge.setValidUntil(
                request.expectedArrivalAt()
                        .toLocalDate()
                        .atTime(23, 59, 59)
        );
        badge.setStatus(BadgeStatus.ACTIVE);

        VisitBadge savedBadge = visitBadgeRepository.saveAndFlush(badge);

        // Create the initial IN session at check-in.
        MetaTimeTracking tracking = new MetaTimeTracking();

        tracking.setVisitorId(visitorId);
        tracking.setVisitId(visitId);
        tracking.setBadgeId(savedBadge.getId());
        tracking.setInTime(checkedInAt);
        tracking.setOutTime(null);

        trackingRepository.save(tracking);

        log.info(
                "QR_BADGE_GENERATED visitId={} badgeId={}",
                visitId,
                savedBadge.getId()
        );
System.out.println(token);
        return new QrBadgeResponse(token, qrCode);
    }

    private String generateToken() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    private String generateQrCode(String payload) {

        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    payload,
                    BarcodeFormat.QR_CODE,
                    300,
                    300,
                    hints
            );

            ByteArrayOutputStream outputStream =
                    new ByteArrayOutputStream();

            MatrixToImageWriter.writeToStream(
                    bitMatrix,
                    "PNG",
                    outputStream
            );

            return Base64.getEncoder()
                    .encodeToString(outputStream.toByteArray());

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate QR code",
                    exception
            );
        }
    }


    @Override
    @Transactional
    public QrScanResponse processScan(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "QR token is required"
            );
        }

        // 1. Lock the badge to serialize scans for this badge.
        VisitBadge badge = visitBadgeRepository
                .findByQrContainingTokenForUpdate(token.trim())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid QR code")
                );

        LocalDateTime scannedAt = LocalDateTime.now();

        // 2. Validate badge status.
        if (badge.getStatus() != BadgeStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "QR code is no longer valid"
            );
        }

        // 3. Validate badge expiry.
        if (badge.getValidUntil() == null
                || scannedAt.isAfter(badge.getValidUntil())) {

            throw new IllegalArgumentException(
                    "QR code has expired"
            );
        }

        String visitId = badge.getVisitId();

        // 4. Validate the VMS visit status.
        String visitStatus = visitBadgeRepository
                .findVisitStatusById(visitId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Visit not found: " + visitId
                        )
                );

        if (!"CHECKED_IN".equals(visitStatus)) {
            throw new IllegalStateException(
                    "Only checked-in visits can be scanned"
            );
        }

        // 5. Get the latest tracking session.
        MetaTimeTracking latestSession = trackingRepository
                .findTopByVisitIdOrderByInTimeDesc(visitId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No tracking session found for visit: "
                                        + visitId
                        )
                );

        // 6. Reject duplicate scans within 60 seconds.
        LocalDateTime lastScanAt =
                latestSession.getOutTime() != null
                        ? latestSession.getOutTime()
                        : latestSession.getInTime();

        if (lastScanAt == null) {
            throw new IllegalStateException(
                    "Latest tracking session has no scan timestamp"
            );
        }

        LocalDateTime cooldownEndsAt = lastScanAt.plusSeconds(
                DUPLICATE_SCAN_COOLDOWN_SECONDS
        );

        if (scannedAt.isBefore(cooldownEndsAt)) {
            throw new IllegalStateException(
                    "Duplicate scan rejected. Please wait "
                            + DUPLICATE_SCAN_COOLDOWN_SECONDS
                            + " seconds before scanning again."
            );
        }

        // 7. Find an open IN session.
        MetaTimeTracking openSession = trackingRepository
                .findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc(
                        visitId
                )
                .orElse(null);

        // 8. Open session exists: record OUT.
        if (openSession != null) {

            openSession.setOutTime(scannedAt);
            trackingRepository.save(openSession);

            log.info(
                    "QR_SCAN_RECORDED action=OUT visitId={} trackingId={}",
                    visitId,
                    openSession.getId()
            );

            return new QrScanResponse(
                    "Visitor temporarily checked out",
                    "OUT",
                    scannedAt
            );
        }

        // 9. No open session: create the next IN session.
        String visitorId = latestSession.getVisitorId();

        if (visitorId == null || visitorId.isBlank()) {
            throw new IllegalStateException(
                    "Visitor ID is missing for visit: " + visitId
            );
        }

        MetaTimeTracking newSession = new MetaTimeTracking();

        newSession.setVisitorId(visitorId);
        newSession.setVisitId(visitId);
        newSession.setBadgeId(badge.getId());
        newSession.setInTime(scannedAt);
        newSession.setOutTime(null);

        MetaTimeTracking savedSession =
                trackingRepository.save(newSession);

        log.info(
                "QR_SCAN_RECORDED action=IN visitId={} trackingId={}",
                visitId,
                savedSession.getId()
        );

        return new QrScanResponse(
                "Visitor checked in again",
                "IN",
                scannedAt
        );
    }



    @Override
    @Transactional
    public void invalidateBadgeOnCheckout(
            CheckoutBadgeInvalidationRequest request
    ) {
        if (request == null
                || request.visitId() == null
                || request.visitId().isBlank()
                || request.checkoutTime() == null) {
            throw new IllegalArgumentException(
                    "Visit ID and checkout time are required"
            );
        }

        String visitId = request.visitId().trim();
        LocalDateTime checkoutTime = request.checkoutTime();

        VisitBadge badge = visitBadgeRepository.findByVisitId(visitId)
                .orElse(null);

        if (badge == null) {
            log.warn(
                    "BADGE_INVALIDATION_SKIPPED visitId={} reason=BADGE_NOT_FOUND",
                    visitId
            );
            return;
        }

        badge.setValidUntil(checkoutTime);
        badge.setStatus(BadgeStatus.INVALID);
        visitBadgeRepository.save(badge);

        MetaTimeTracking openSession = trackingRepository
                .findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc(visitId)
                .orElse(null);

        if (openSession != null) {
            openSession.setOutTime(checkoutTime);
            trackingRepository.save(openSession);
        }

        log.info(
                "VISIT_BADGE_INVALIDATED badgeId={} visitId={} checkoutTime={} openSessionClosed={}",
                badge.getId(),
                visitId,
                checkoutTime,
                openSession != null
        );
    }

}
