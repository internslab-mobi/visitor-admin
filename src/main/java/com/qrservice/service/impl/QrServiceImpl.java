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

/**
 * Service implementation responsible for QR badge generation,
 * QR scan processing, visitor time tracking, and badge invalidation.
 *
 * <p>Generates secure QR tokens, creates QR code images, validates badges
 * during scanning, records visitor entry and exit times, and invalidates
 * badges when a visit is checked out.</p>
 *
 * <p>Coordinates badge persistence through {@link VisitBadgeRepository},
 * visitor time tracking through {@link MetaTimeTrackingRepository},
 * and badge identifier generation through {@link IdGeneratorService}.</p>
 *
 * @author Anisha Z
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QrServiceImpl implements QrService {

    /**
     * Minimum interval, in seconds, required between consecutive scans
     * for the same visit to prevent duplicate scan processing.
     */
    private static final long DUPLICATE_SCAN_COOLDOWN_SECONDS = 60;

    private final IdGeneratorService idGeneratorService;
    private final VisitBadgeRepository visitBadgeRepository;
    private final MetaTimeTrackingRepository trackingRepository;

    /**
     * Cryptographically secure random number generator used to create
     * unpredictable QR badge tokens.
     */
    private final SecureRandom secureRandom = new SecureRandom();


    /**
     * Generates a QR badge for a visitor when the visit is checked in.
     *
     * <p>Validates the request, prevents duplicate badge generation,
     * generates a secure token and QR code, saves the badge, and creates
     * the initial visitor time-tracking session with an IN timestamp.</p>
     *
     * <p>The badge is initially active and its validity ends at
     * 23:59:59 on the expected arrival date.</p>
     *
     * @param request request containing the visit ID, visitor ID,
     *                expected arrival time, and check-in time
     * @return response containing the generated QR token and
     *         Base64-encoded QR code image
     * @throws IllegalArgumentException if the request or required fields
     *                                  are missing, or the check-in time
     *                                  is in the future
     * @throws IllegalStateException if a badge already exists for the visit
     */
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

    /**
     * Generates a cryptographically random token for a QR badge.
     *
     * <p>Creates 32 random bytes and encodes them using URL-safe Base64
     * without padding characters.</p>
     *
     * @return URL-safe Base64-encoded random token
     */
    private String generateToken() {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }

    /**
     * Generates a QR code image from the supplied payload.
     *
     * <p>Uses ZXing to create a 300 × 300 pixel QR code in PNG format.
     * The generated image is returned as a Base64-encoded string.</p>
     *
     * @param payload data to encode in the QR code
     * @return Base64-encoded PNG image containing the QR code
     * @throws IllegalStateException if QR code generation or image
     *                                encoding fails
     */
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

    /**
     * Processes a visitor's QR badge scan and records the corresponding
     * entry or exit timestamp.
     *
     * <p>Validates the token, locks the badge record, checks the badge
     * status and expiry, verifies that the associated visit is checked in,
     * and rejects duplicate scans within the configured cooldown period.</p>
     *
     * <p>If an open tracking session exists, records an OUT timestamp.
     * Otherwise, creates a new tracking session with an IN timestamp.</p>
     *
     * @param token QR badge token received from the scanned QR code
     * @return response containing the scan message, action, and timestamp
     * @throws IllegalArgumentException if the token is missing or invalid,
     *                                  the badge is inactive or expired,
     *                                  or the visit cannot be found
     * @throws IllegalStateException if the visit is not checked in,
     *                               tracking information is missing,
     *                               or the scan occurs within the cooldown
     */
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

    /**
     * Invalidates a visitor's QR badge when the associated visit is checked out.
     *
     * <p>Updates the badge validity end time to the checkout time and changes
     * its status to INVALID. If an open time-tracking session exists, closes
     * the session by recording the checkout timestamp as its OUT time.</p>
     *
     * <p>If no badge exists for the visit, logs a warning and returns
     * without performing any updates.</p>
     *
     * @param request request containing the visit ID and checkout timestamp
     * @throws IllegalArgumentException if the request, visit ID,
     *                                  or checkout timestamp is missing
     */

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
