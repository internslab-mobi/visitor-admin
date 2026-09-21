//package com.adminvisitor.controller;
//
//import com.adminvisitor.entity.Visit;
//import com.adminvisitor.entity.VisitBadge;
//import com.adminvisitor.enums.BadgeStatus;
//import com.adminvisitor.exception.BadgeAlreadyExistsException;
//import com.adminvisitor.exception.GlobalExceptionHandler;
//import com.adminvisitor.exception.ResourceNotFoundException;
//import com.adminvisitor.repository.VisitRepository;
//import com.adminvisitor.service.EmailService;
//import com.adminvisitor.service.QrCodeService;
//import com.adminvisitor.service.VisitBadgeService;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
//import org.springframework.context.annotation.Import;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
//import org.springframework.test.web.servlet.MockMvc;
//
//import java.time.LocalDateTime;
//import java.util.Optional;
//
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(VisitBadgeController.class)
//@Import(GlobalExceptionHandler.class)
//class VisitBadgeControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockitoBean
//    private VisitRepository visitRepository;
//
//    @MockitoBean
//    private VisitBadgeService visitBadgeService;
//
//    @MockitoBean
//    private QrCodeService qrCodeService;
//
//    @MockitoBean
//    private EmailService emailService;
//
//
//    // =========================================================
//    // CREATE BADGE - SUCCESS
//    // =========================================================
//
//    @Test
//    void createBadge_shouldReturn200_whenBadgeCreatedSuccessfully()
//            throws Exception {
//
//        String visitId = "vt-001";
//
//        Visit visit = mock(Visit.class);
//        VisitBadge badge = mock(VisitBadge.class);
//
//        //When my code asks the repository for this visit ID,
//        // pretend that the visit was found and return this fake visit object.
//        when(visitRepository.findById(visitId))
//                .thenReturn(Optional.of(visit));
//
//        when(visitBadgeService.createBadge(visit))
//                .thenReturn(badge);
//
//        when(badge.getId())
//                .thenReturn("VB-001");
//
//        when(badge.getVisit())
//                .thenReturn(visit);
//
//        when(visit.getId())
//                .thenReturn(visitId);
//
//        when(badge.getQrContainingToken())
//                .thenReturn("qr-token-001");
//
//        when(qrCodeService.generateQrCode("qr-token-001"))
//                .thenReturn("base64-qr-code");
//
//        LocalDateTime issuedAt =
//                LocalDateTime.of(2026, 9, 21, 10, 0);
//
//        LocalDateTime validUntil =
//                LocalDateTime.of(2026, 9, 21, 23, 59, 59);
//
//        when(badge.getIssuedAt())
//                .thenReturn(issuedAt);
//
//        when(badge.getValidUntil())
//                .thenReturn(validUntil);
//
//        when(badge.getStatus())
//                .thenReturn(BadgeStatus.ACTIVE);
//
//
//        mockMvc.perform(
//                        post("/api/visit-badges/{visitId}", visitId)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.badgeId").value("VB-001"))
//                .andExpect(jsonPath("$.visitId").value("vt-001"))
//                .andExpect(jsonPath("$.qrContainingToken")
//                        .value("qr-token-001"))
//                .andExpect(jsonPath("$.qrCode")
//                        .value("base64-qr-code"))
//                .andExpect(jsonPath("$.status")
//                        .value("ACTIVE"));
//
//
//        verify(visitRepository)
//                .findById(visitId);
//
//        verify(visitBadgeService)
//                .createBadge(visit);
//
//        verify(qrCodeService)
//                .generateQrCode("qr-token-001");
//
//        verify(emailService)
//                .sendVisitBadgeEmail(
//                        visit,
//                        badge,
//                        "base64-qr-code",
//                        null
//                );
//    }
//
//
//    // =========================================================
//    // CREATE BADGE - VISIT NOT FOUND
//    // =========================================================
//
//    @Test
//    void createBadge_shouldReturn404_whenVisitDoesNotExist()
//            throws Exception {
//
//        String visitId = "vt-999";
//
//        when(visitRepository.findById(visitId))
//                .thenReturn(Optional.empty());
//
//
//        mockMvc.perform(
//                        post("/api/visit-badges/{visitId}", visitId)
//                )
//                .andExpect(status().isNotFound())
//                .andExpect(jsonPath("$.status")
//                        .value(404))
//                .andExpect(jsonPath("$.error")
//                        .value("Not Found"))
//                .andExpect(jsonPath("$.message")
//                        .value("Visit not found with id: vt-999"));
//
//
//        verify(visitRepository)
//                .findById(visitId);
//
//        verifyNoInteractions(visitBadgeService);
//        verifyNoInteractions(qrCodeService);
//        verifyNoInteractions(emailService);
//    }
//
//
//    // =========================================================
//    // CREATE BADGE - BADGE ALREADY EXISTS
//    // =========================================================
//
//    @Test
//    void createBadge_shouldReturn409_whenBadgeAlreadyExists()
//            throws Exception {
//
//        String visitId = "vt-001";
//
//        Visit visit = mock(Visit.class);
//
//        when(visitRepository.findById(visitId))
//                .thenReturn(Optional.of(visit));
//
//        when(visitBadgeService.createBadge(visit))
//                .thenThrow(
//                        new BadgeAlreadyExistsException(
//                                "Badge already exists for visit: " + visitId
//                        )
//                );
//
//
//        mockMvc.perform(
//                        post("/api/visit-badges/{visitId}", visitId)
//                )
//                .andExpect(status().isConflict())
//                .andExpect(jsonPath("$.status")
//                        .value(409))
//                .andExpect(jsonPath("$.error")
//                        .value("Conflict"))
//                .andExpect(jsonPath("$.message")
//                        .value("Badge already exists for visit: vt-001"));
//
//
//        verify(visitRepository)
//                .findById(visitId);
//
//        verify(visitBadgeService)
//                .createBadge(visit);
//
//        verifyNoInteractions(qrCodeService);
//        verifyNoInteractions(emailService);
//    }
//
//
//    // =========================================================
//    // CREATE BADGE - UNEXPECTED ERROR
//    // =========================================================
//
//    @Test
//    void createBadge_shouldReturn500_whenUnexpectedErrorOccurs()
//            throws Exception {
//
//        String visitId = "vt-001";
//
//        Visit visit = mock(Visit.class);
//
//        when(visitRepository.findById(visitId))
//                .thenReturn(Optional.of(visit));
//
//        when(visitBadgeService.createBadge(visit))
//                .thenThrow(
//                        new RuntimeException("Unexpected error")
//                );
//
//
//        mockMvc.perform(
//                        post("/api/visit-badges/{visitId}", visitId)
//                )
//                .andExpect(status().isInternalServerError())
//                .andExpect(jsonPath("$.status")
//                        .value(500))
//                .andExpect(jsonPath("$.error")
//                        .value("Internal Server Error"))
//                .andExpect(jsonPath("$.message")
//                        .value("An unexpected error occurred"));
//
//
//        verify(visitRepository)
//                .findById(visitId);
//
//        verify(visitBadgeService)
//                .createBadge(visit);
//
//        verifyNoInteractions(qrCodeService);
//        verifyNoInteractions(emailService);
//    }
//
//
//    // =========================================================
//    // VALIDATE QR - ACTIVE
//    // =========================================================
//
//    @Test
//    void validateQr_shouldReturn200_whenQrIsActive()
//            throws Exception {
//
//        String token = "qr-token-001";
//
//        when(visitBadgeService.validateQrToken(token))
//                .thenReturn(BadgeStatus.ACTIVE);
//
//
//        mockMvc.perform(
//                        get("/api/visit-badges/validate")
//                                .param("token", token)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status")
//                        .value("ACTIVE"))
//                .andExpect(jsonPath("$.message")
//                        .value("QR code is valid"));
//
//
//        verify(visitBadgeService)
//                .validateQrToken(token);
//    }
//
//
//    // =========================================================
//    // VALIDATE QR - INVALID
//    // =========================================================
//
//    @Test
//    void validateQr_shouldReturn200_whenQrIsInvalid()
//            throws Exception {
//
//        String token = "qr-token-expired";
//
//        when(visitBadgeService.validateQrToken(token))
//                .thenReturn(BadgeStatus.INVALID);
//
//
//        mockMvc.perform(
//                        get("/api/visit-badges/validate")
//                                .param("token", token)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status")
//                        .value("INVALID"))
//                .andExpect(jsonPath("$.message")
//                        .value("QR code is no longer valid"));
//
//
//        verify(visitBadgeService)
//                .validateQrToken(token);
//    }
//
//
//    // =========================================================
//    // VALIDATE QR - TOKEN DOES NOT EXIST / INVALID TOKEN
//    // =========================================================
//
//    @Test
//    void validateQr_shouldReturn200_whenTokenDoesNotExist()
//            throws Exception {
//
//        String token = "invalid-token";
//
//        when(visitBadgeService.validateQrToken(token))
//                .thenThrow(
//                        new IllegalArgumentException(
//                                "Invalid QR token"
//                        )
//                );
//
//
//        mockMvc.perform(
//                        get("/api/visit-badges/validate")
//                                .param("token", token)
//                )
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status")
//                        .value("INVALID"))
//                .andExpect(jsonPath("$.message")
//                        .value("Invalid QR code"));
//
//
//        verify(visitBadgeService)
//                .validateQrToken(token);
//    }
//}