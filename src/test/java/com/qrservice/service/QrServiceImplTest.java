package com.qrservice.service;

import com.qrservice.dto.CheckoutBadgeInvalidationRequest;
import com.qrservice.dto.QrBadgeRequest;
import com.qrservice.dto.QrBadgeResponse;
import com.qrservice.dto.QrScanResponse;
import com.qrservice.entity.MetaTimeTracking;
import com.qrservice.entity.VisitBadge;
import com.qrservice.enums.BadgeStatus;
import com.qrservice.repository.MetaTimeTrackingRepository;
import com.qrservice.repository.VisitBadgeRepository;

import com.qrservice.service.impl.QrServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link QrServiceImpl}.
 *
 * <p>Verifies QR badge generation, QR scan processing,
 * visitor time tracking, and badge invalidation.</p>
 *
 * <p>Uses Mockito to mock repository and identifier dependencies
 * and JUnit 5 to validate service behavior.</p>
 *
 * @author Anisha Z
 */
@ExtendWith(MockitoExtension.class)
class QrServiceImplTest {

    @Mock
    private IdGeneratorService idGeneratorService;

    @Mock
    private VisitBadgeRepository visitBadgeRepository;

    @Mock
    private MetaTimeTrackingRepository trackingRepository;

    @InjectMocks
    private QrServiceImpl qrService;

    private LocalDateTime now;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    /**
     * Initializes common test data before each test.
     */
    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().minusMinutes(2);
    }

    /**
     * Tests successful QR badge generation.
     *
     * <p>Verifies that the badge is saved with ACTIVE status
     * and an initial IN tracking session is created.</p>
     */
    @Test
    void generateQrBadge_ShouldCreateBadgeAndTrackingSession() {

        QrBadgeRequest request = new QrBadgeRequest(
                "VIS001",
                now.plusHours(2),
                "VISITOR001",

                now
        );

        when(visitBadgeRepository.findByVisitId("VIS001"))
                .thenReturn(Optional.empty());

        when(idGeneratorService.generateId("VISITBADGE", "VB"))
                .thenReturn("VB001");

        when(visitBadgeRepository.saveAndFlush(any(VisitBadge.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(trackingRepository.save(any(MetaTimeTracking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QrBadgeResponse response = qrService.generateQrBadge(request);

        assertNotNull(response);
        assertNotNull(response.token());
        assertNotNull(response.qrCode());
        assertFalse(response.token().isBlank());
        assertFalse(response.qrCode().isBlank());

        verify(visitBadgeRepository).saveAndFlush(
                argThat(badge ->
                        badge.getStatus() == BadgeStatus.ACTIVE
                                && "VIS001".equals(badge.getVisitId())
                                && "VB001".equals(badge.getId())
                )
        );

        verify(trackingRepository).save(
                argThat(tracking ->
                        "VIS001".equals(tracking.getVisitId())
                                && "VISITOR001".equals(tracking.getVisitorId())
                                && "VB001".equals(tracking.getBadgeId())
                                && tracking.getInTime().equals(now)
                                && tracking.getOutTime() == null
                )
        );
    }

    /**
     * Tests QR badge generation with a missing request.
     *
     * <p>Verifies that an IllegalArgumentException is thrown
     * when the request is null.</p>
     */
    @Test
    void generateQrBadge_ShouldThrowException_WhenRequestIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> qrService.generateQrBadge(null)
        );

        verifyNoInteractions(
                idGeneratorService,
                visitBadgeRepository,
                trackingRepository
        );
    }

    /**
     * Tests duplicate badge prevention.
     *
     * <p>Verifies that badge generation is rejected when
     * a badge already exists for the visit.</p>
     */
    @Test
    void generateQrBadge_ShouldThrowException_WhenBadgeAlreadyExists() {

        QrBadgeRequest request = new QrBadgeRequest(
                "VIS001",
                now.plusHours(2),"VISITOR001",

                now
        );

        when(visitBadgeRepository.findByVisitId("VIS001"))
                .thenReturn(Optional.of(new VisitBadge()));

        assertThrows(
                IllegalStateException.class,
                () -> qrService.generateQrBadge(request)
        );

        verify(visitBadgeRepository, never())
                .saveAndFlush(any(VisitBadge.class));

        verifyNoInteractions(idGeneratorService, trackingRepository);
    }

    /**
     * Tests QR scan processing when an open tracking session exists.
     *
     * <p>Verifies that the scan records an OUT timestamp
     * and returns the OUT action.</p>
     */
    @Test
    void processScan_ShouldRecordOut_WhenOpenSessionExists() {

        VisitBadge badge = new VisitBadge();
        badge.setId("VB001");
        badge.setVisitId("VIS001");
        badge.setStatus(BadgeStatus.ACTIVE);
        badge.setValidUntil(LocalDateTime.now().plusHours(2));

        MetaTimeTracking session = new MetaTimeTracking();
        session.setId(1L);
        session.setVisitId("VIS001");
        session.setVisitorId("VISITOR001");
        session.setBadgeId("VB001");
        session.setInTime(now);
        session.setOutTime(null);

        when(visitBadgeRepository.findByQrContainingTokenForUpdate("token123"))
                .thenReturn(Optional.of(badge));

        when(visitBadgeRepository.findVisitStatusById("VIS001"))
                .thenReturn(Optional.of("CHECKED_IN"));

        when(trackingRepository.findTopByVisitIdOrderByInTimeDesc("VIS001"))
                .thenReturn(Optional.of(session));

        when(trackingRepository
                .findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc("VIS001"))
                .thenReturn(Optional.of(session));

        when(trackingRepository.save(session)).thenReturn(session);

        QrScanResponse response = qrService.processScan("token123");

        assertNotNull(response);
        assertEquals("OUT", response.action());
        assertEquals("Visitor temporarily checked out", response.message());
        assertNotNull(session.getOutTime());

        verify(trackingRepository).save(session);
    }

    /**
     * Tests QR scan processing when no open tracking session exists.
     *
     * <p>Verifies that a new IN tracking session is created
     * and the response contains the IN action.</p>
     */
    @Test
    void processScan_ShouldCreateNewInSession_WhenNoOpenSessionExists() {

        VisitBadge badge = new VisitBadge();
        badge.setId("VB001");
        badge.setVisitId("VIS001");
        badge.setStatus(BadgeStatus.ACTIVE);
        badge.setValidUntil(LocalDateTime.now().plusHours(2));

        MetaTimeTracking latestSession = new MetaTimeTracking();
        latestSession.setId(1L);
        latestSession.setVisitId("VIS001");
        latestSession.setVisitorId("VISITOR001");
        latestSession.setBadgeId("VB001");
        latestSession.setInTime(now.minusMinutes(5));
        latestSession.setOutTime(
                LocalDateTime.now().minusSeconds(70)
        );

        when(visitBadgeRepository.findByQrContainingTokenForUpdate("token123"))
                .thenReturn(Optional.of(badge));

        when(visitBadgeRepository.findVisitStatusById("VIS001"))
                .thenReturn(Optional.of("CHECKED_IN"));

        when(trackingRepository.findTopByVisitIdOrderByInTimeDesc("VIS001"))
                .thenReturn(Optional.of(latestSession));

        when(trackingRepository
                .findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc("VIS001"))
                .thenReturn(Optional.empty());

        when(trackingRepository.save(any(MetaTimeTracking.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QrScanResponse response = qrService.processScan("token123");

        assertNotNull(response);
        assertEquals("IN", response.action());
        assertEquals("Visitor checked in again", response.message());

        verify(trackingRepository).save(
                argThat(tracking ->
                        "VIS001".equals(tracking.getVisitId())
                                && "VISITOR001".equals(tracking.getVisitorId())
                                && "VB001".equals(tracking.getBadgeId())
                                && tracking.getInTime() != null
                                && tracking.getOutTime() == null
                )
        );
    }

    /**
     * Tests QR scan processing with a blank token.
     *
     * <p>Verifies that an IllegalArgumentException is thrown
     * before any repository interaction occurs.</p>
     */
    @Test
    void processScan_ShouldThrowException_WhenTokenIsBlank() {

        assertThrows(
                IllegalArgumentException.class,
                () -> qrService.processScan(" ")
        );

        verifyNoInteractions(visitBadgeRepository, trackingRepository);
    }

    /**
     * Tests QR scan processing with an invalid token.
     *
     * <p>Verifies that an IllegalArgumentException is thrown
     * when no badge matches the supplied token.</p>
     */
    @Test
    void processScan_ShouldThrowException_WhenTokenIsInvalid() {

        when(visitBadgeRepository.findByQrContainingTokenForUpdate("invalid"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> qrService.processScan("invalid")
        );

        verify(visitBadgeRepository)
                .findByQrContainingTokenForUpdate("invalid");

        verifyNoInteractions(trackingRepository);
    }

    /**
     * Tests QR badge invalidation during checkout.
     *
     * <p>Verifies that the badge status becomes INVALID,
     * its validity ends at checkout time, and any open
     * tracking session is closed.</p>
     */
    @Test
    void invalidateBadgeOnCheckout_ShouldInvalidateBadgeAndCloseSession() {

        LocalDateTime checkoutTime = LocalDateTime.now();

        CheckoutBadgeInvalidationRequest request =
                new CheckoutBadgeInvalidationRequest(
                        "VIS001",
                        checkoutTime
                );

        VisitBadge badge = new VisitBadge();
        badge.setId("VB001");
        badge.setVisitId("VIS001");
        badge.setStatus(BadgeStatus.ACTIVE);
        badge.setValidUntil(checkoutTime.plusHours(1));

        MetaTimeTracking session = new MetaTimeTracking();
        session.setId(1L);
        session.setVisitId("VIS001");
        session.setInTime(checkoutTime.minusHours(1));
        session.setOutTime(null);

        when(visitBadgeRepository.findByVisitId("VIS001"))
                .thenReturn(Optional.of(badge));

        when(trackingRepository
                .findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc("VIS001"))
                .thenReturn(Optional.of(session));

        qrService.invalidateBadgeOnCheckout(request);

        assertEquals(BadgeStatus.INVALID, badge.getStatus());
        assertEquals(checkoutTime, badge.getValidUntil());
        assertEquals(checkoutTime, session.getOutTime());

        verify(visitBadgeRepository).save(badge);
        verify(trackingRepository).save(session);
    }

    /**
     * Tests checkout when no badge exists for the visit.
     *
     * <p>Verifies that the method returns without updating
     * the badge or tracking repositories.</p>
     */
    @Test
    void invalidateBadgeOnCheckout_ShouldSkip_WhenBadgeDoesNotExist() {

        CheckoutBadgeInvalidationRequest request =
                new CheckoutBadgeInvalidationRequest(
                        "VIS001",
                        LocalDateTime.now()
                );

        when(visitBadgeRepository.findByVisitId("VIS001"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(
                () -> qrService.invalidateBadgeOnCheckout(request)
        );

        verify(visitBadgeRepository, never()).save(any(VisitBadge.class));
        verifyNoInteractions(trackingRepository);
    }

    /**
     * Tests checkout invalidation with a missing request.
     *
     * <p>Verifies that an IllegalArgumentException is thrown
     * when the request is null.</p>
     */
    @Test
    void invalidateBadgeOnCheckout_ShouldThrowException_WhenRequestIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> qrService.invalidateBadgeOnCheckout(null)
        );

        verifyNoInteractions(visitBadgeRepository, trackingRepository);
    }
}
