package com.adminvisitor.service;

import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.enums.BadgeStatus;
import com.adminvisitor.exception.BadgeAlreadyExistsException;
import com.adminvisitor.repository.VisitBadgeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

//This allows us to capture the object that our service sends to a mocked method.
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VisitBadgeServiceTest {

    @Mock
    private VisitBadgeRepository visitBadgeRepository;

    @Mock
    private IdGeneratorService idGeneratorService;

    @Mock
    private QrTokenService qrTokenService;

    @InjectMocks
    private VisitBadgeService visitBadgeService;

    // CREATE BADGE - SUCCESS

    @Test
    void createBadge_shouldCreateActiveBadgeWithCorrectExpiry() {
        Visit visit = mock(Visit.class);
        LocalDateTime arrival = LocalDateTime.of(2026, 9, 28, 10, 30);

        when(visit.getId()).thenReturn("VIS-001");
        when(visit.getExpectedArrivalAt()).thenReturn(arrival);
        when(visitBadgeRepository.findByVisit_Id("VIS-001"))
                .thenReturn(Optional.empty());
        when(idGeneratorService.generateId("VISITBADGE", "VB"))
                .thenReturn("VB-001");
        when(qrTokenService.generateToken())
                .thenReturn("qr-token-001");

        VisitBadge savedBadge = new VisitBadge();
        //the below line is whenever the save() is called with any badge return the saved badge
        when(visitBadgeRepository.save(any(VisitBadge.class)))
                .thenReturn(savedBadge);

        VisitBadge result = visitBadgeService.createBadge(visit);

        //It catch the exact VisitBadge object that the service sends to the repository
        ArgumentCaptor<VisitBadge> captor =
                ArgumentCaptor.forClass(VisitBadge.class);


        //iyt does 2 things
        //Check that the repository's save() method was actually called
        //Capture the badge object that was passed to save()
        verify(visitBadgeRepository).save(captor.capture());

        VisitBadge badge = captor.getValue();

        assertSame(savedBadge, result);
        assertEquals("VB-001", badge.getId());
        assertSame(visit, badge.getVisit());
        assertEquals("qr-token-001", badge.getQrContainingToken());
        assertNotNull(badge.getIssuedAt());
        assertEquals(
                LocalDateTime.of(2026, 9, 28, 23, 59, 59),
                badge.getValidUntil()
        );
        assertEquals(BadgeStatus.ACTIVE, badge.getStatus());

        verify(idGeneratorService).generateId("VISITBADGE", "VB");
        verify(qrTokenService).generateToken();
    }

    // CREATE BADGE - DUPLICATE

    @Test
    void createBadge_shouldThrow_whenBadgeAlreadyExists() {
        Visit visit = mock(Visit.class);
        VisitBadge existingBadge = new VisitBadge();

        when(visit.getId()).thenReturn("VIS-001");
        when(visitBadgeRepository.findByVisit_Id("VIS-001"))
                .thenReturn(Optional.of(existingBadge));

        BadgeAlreadyExistsException exception = assertThrows(
                BadgeAlreadyExistsException.class,
                () -> visitBadgeService.createBadge(visit)
        );

        assertEquals(
                "A visitor badge already exists for visit: VIS-001",
                exception.getMessage()
        );

        verify(visitBadgeRepository).findByVisit_Id("VIS-001");
        verifyNoInteractions(idGeneratorService, qrTokenService);
        verify(visitBadgeRepository, never()).save(any());
    }

    // CREATE BADGE - EXPIRY USES ARRIVAL DATE

    @Test
    void createBadge_shouldExpireAtEndOfArrivalDate_evenWhenDepartureIsNextDay() {
        Visit visit = mock(Visit.class);
        LocalDateTime arrival = LocalDateTime.of(2026, 9, 28, 23, 30);
        LocalDateTime departure = LocalDateTime.of(2026, 9, 29, 2, 0);

        when(visit.getId()).thenReturn("VIS-002");
        when(visit.getExpectedArrivalAt()).thenReturn(arrival);

        when(visitBadgeRepository.findByVisit_Id("VIS-002"))
                .thenReturn(Optional.empty());
        when(idGeneratorService.generateId("VISITBADGE", "VB"))
                .thenReturn("VB-002");
        when(qrTokenService.generateToken())
                .thenReturn("qr-token-002");

        visitBadgeService.createBadge(visit);

        ArgumentCaptor<VisitBadge> captor =
                ArgumentCaptor.forClass(VisitBadge.class);
        verify(visitBadgeRepository).save(captor.capture());

        assertEquals(
                LocalDateTime.of(2026, 9, 28, 23, 59, 59),
                captor.getValue().getValidUntil()
        );
    }

    // CREATE BADGE - ID GENERATION FAILURE

    @Test
    void createBadge_shouldPropagateException_whenIdGenerationFails() {

        Visit visit = new Visit();
        visit.setId("VIS-003");

        when(visitBadgeRepository.findByVisit_Id("VIS-003"))
                .thenReturn(Optional.empty());

        when(idGeneratorService.generateId("VISITBADGE", "VB"))
                .thenThrow(
                        new IllegalStateException(
                                "ID sequence not configured"
                        )
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> visitBadgeService.createBadge(visit)
        );

        assertEquals(
                "ID sequence not configured",
                exception.getMessage()
        );

        verifyNoInteractions(qrTokenService);

        verify(visitBadgeRepository, never())
                .save(any());
    }
    // CREATE BADGE - QR TOKEN GENERATION FAILURE

    @Test
    void createBadge_shouldPropagateException_whenQrTokenGenerationFails() {

        Visit visit = new Visit();
        visit.setId("VIS-004");

        when(visitBadgeRepository.findByVisit_Id("VIS-004"))
                .thenReturn(Optional.empty());

        when(idGeneratorService.generateId("VISITBADGE", "VB"))
                .thenReturn("VB-004");

        when(qrTokenService.generateToken())
                .thenThrow(
                        new IllegalStateException(
                                "Failed to generate QR token"
                        )
                );

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> visitBadgeService.createBadge(visit)
        );

        assertEquals(
                "Failed to generate QR token",
                exception.getMessage()
        );

        verify(visitBadgeRepository, never())
                .save(any());
    }
    // CHECKOUT INVALIDATION - SUCCESS

    @Test
    void invalidateBadgeOnCheckout_shouldInvalidateBadgeAndSetCheckoutTime() {
        VisitBadge badge = new VisitBadge();
        badge.setId("VB-001");
        badge.setStatus(BadgeStatus.ACTIVE);

        LocalDateTime checkoutTime =
                LocalDateTime.of(2026, 9, 28, 18, 45);

        when(visitBadgeRepository.findByVisit_Id("VIS-001"))
                .thenReturn(Optional.of(badge));
        when(visitBadgeRepository.save(badge))
                .thenReturn(badge);

        visitBadgeService.invalidateBadgeOnCheckout(
                "VIS-001",
                checkoutTime
        );

        assertEquals(BadgeStatus.INVALID, badge.getStatus());
        assertEquals(checkoutTime, badge.getValidUntil());

        verify(visitBadgeRepository).findByVisit_Id("VIS-001");
        verify(visitBadgeRepository).save(badge);
    }

    // CHECKOUT INVALIDATION - NO BADGE

    @Test
    void invalidateBadgeOnCheckout_shouldDoNothing_whenBadgeDoesNotExist() {
        LocalDateTime checkoutTime =
                LocalDateTime.of(2026, 9, 28, 18, 45);

        when(visitBadgeRepository.findByVisit_Id("VIS-999"))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() ->
                visitBadgeService.invalidateBadgeOnCheckout(
                        "VIS-999",
                        checkoutTime
                )
        );

        verify(visitBadgeRepository).findByVisit_Id("VIS-999");
        verify(visitBadgeRepository, never()).save(any());
    }

    // CHECKOUT INVALIDATION - ALREADY INVALID BADGE

    @Test
    void invalidateBadgeOnCheckout_shouldStillUpdateBadge_whenAlreadyInvalid() {
        VisitBadge badge = new VisitBadge();
        badge.setId("VB-002");
        badge.setStatus(BadgeStatus.INVALID);

        LocalDateTime checkoutTime =
                LocalDateTime.of(2026, 9, 28, 19, 0);

        when(visitBadgeRepository.findByVisit_Id("VIS-002"))
                .thenReturn(Optional.of(badge));

        visitBadgeService.invalidateBadgeOnCheckout(
                "VIS-002",
                checkoutTime
        );

        assertEquals(BadgeStatus.INVALID, badge.getStatus());
        assertEquals(checkoutTime, badge.getValidUntil());
        verify(visitBadgeRepository).save(badge);
    }

    // QR VALIDATION - ACTIVE AND NOT EXPIRED

    @Test
    void validateQrToken_shouldReturnActive_whenTokenIsActiveAndNotExpired() {
        VisitBadge badge = new VisitBadge();
        badge.setStatus(BadgeStatus.ACTIVE);
        badge.setValidUntil(LocalDateTime.now().plusMinutes(5));

        when(visitBadgeRepository.findByQrContainingToken("token-active"))
                .thenReturn(Optional.of(badge));

        BadgeStatus result =
                visitBadgeService.validateQrToken("token-active");

        assertEquals(BadgeStatus.ACTIVE, result);
    }

    // QR VALIDATION - EXPIRED

    @Test
    void validateQrToken_shouldReturnInvalid_whenTokenIsExpired() {
        VisitBadge badge = new VisitBadge();
        badge.setStatus(BadgeStatus.ACTIVE);
        badge.setValidUntil(LocalDateTime.now().minusSeconds(1));

        when(visitBadgeRepository.findByQrContainingToken("token-expired"))
                .thenReturn(Optional.of(badge));

        BadgeStatus result =
                visitBadgeService.validateQrToken("token-expired");

        assertEquals(BadgeStatus.INVALID, result);
    }

    // QR VALIDATION - ALREADY INVALID STATUS

    @Test
    void validateQrToken_shouldReturnInvalid_whenBadgeStatusIsInvalid() {
        VisitBadge badge = new VisitBadge();
        badge.setStatus(BadgeStatus.INVALID);
        badge.setValidUntil(LocalDateTime.now().plusHours(1));

        when(visitBadgeRepository.findByQrContainingToken("token-invalid"))
                .thenReturn(Optional.of(badge));

        BadgeStatus result =
                visitBadgeService.validateQrToken("token-invalid");

        assertEquals(BadgeStatus.INVALID, result);
    }

    // QR VALIDATION - TOKEN NOT FOUND

    @Test
    void validateQrToken_shouldThrow_whenTokenDoesNotExist() {
        when(visitBadgeRepository.findByQrContainingToken("unknown-token"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitBadgeService.validateQrToken("unknown-token")
        );

        assertEquals("Invalid QR code", exception.getMessage());
    }

    // QR VALIDATION - BLANK TOKEN

    @Test
    void validateQrToken_shouldThrow_whenTokenIsBlankAndNotFound() {
        when(visitBadgeRepository.findByQrContainingToken(""))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> visitBadgeService.validateQrToken("")
        );

        assertEquals("Invalid QR code", exception.getMessage());
        verify(visitBadgeRepository).findByQrContainingToken("");
    }
}
