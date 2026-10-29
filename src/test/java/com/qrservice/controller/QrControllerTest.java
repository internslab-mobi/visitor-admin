package com.qrservice.controller;

import com.qrservice.dto.CheckoutBadgeInvalidationRequest;
import com.qrservice.dto.QrBadgeRequest;
import com.qrservice.dto.QrBadgeResponse;
import com.qrservice.service.QrService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web MVC slice tests for {@link QrController}.
 *
 * <p>Verifies HTTP status codes, request validation, and delegation
 * to the QR service. Requests include the configured API key so that
 * the API key filter allows valid requests to reach the controller.</p>
 */
@WebMvcTest(QrController.class)
@TestPropertySource(properties = "qr.security.api-key=test-qr-key")
class QrControllerTest {

    private static final String TEST_API_KEY = "test-qr-key";
    private static final String API_KEY_HEADER = "X-QR-API-KEY";

    private static final String GENERATE_BADGE_URL =
            "/api/qr/generate-badge";

    private static final String PROCESS_SCAN_URL =
            "/api/qr/process-scan";

    private static final String INVALIDATE_BADGE_URL =
            "/api/qr/invalidate-badge";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QrService qrService;

    /**
     * Mocks JPA metamodel infrastructure if it is required by the
     * application's persistence-related configuration.
     */
    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    private QrBadgeRequest badgeRequest;

    private CheckoutBadgeInvalidationRequest invalidationRequest;

    /**
     * Initializes valid request DTOs used by the controller tests.
     */
    @BeforeEach
    void setUp() {
        badgeRequest = new QrBadgeRequest(
                "V001",
                LocalDateTime.parse("2026-10-26T09:00:00"),
                "Test Visitor",
                LocalDateTime.parse("2026-10-26T09:15:00")
        );

        invalidationRequest = new CheckoutBadgeInvalidationRequest(
                "V001",
                LocalDateTime.parse("2026-10-26T17:00:00")
        );
    }

    /**
     * Verifies that a valid badge-generation request returns HTTP 200
     * and delegates processing to the QR service.
     */
    @Test
    void generateQrBadge_shouldReturn200_whenRequestIsValid()
            throws Exception {

        when(qrService.generateQrBadge(any(QrBadgeRequest.class)))
                .thenReturn(new QrBadgeResponse(
                        "test-qr-token",
                        "test-qr-code"
                ));

        mockMvc.perform(post(GENERATE_BADGE_URL)
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badgeRequest)))
                .andExpect(status().isOk());

        verify(qrService).generateQrBadge(any(QrBadgeRequest.class));
    }

    /**
     * Verifies that a missing request body returns HTTP 400
     * without invoking the QR service.
     */
    @Test
    void generateQrBadge_shouldReturn400_whenRequestBodyIsMissing()
            throws Exception {

        mockMvc.perform(post(GENERATE_BADGE_URL)
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }

    /**
     * Verifies that using GET instead of POST for badge generation
     * returns HTTP 405.
     */
    @Test
    void generateQrBadge_shouldReturn405_whenGetMethodIsUsed()
            throws Exception {

        mockMvc.perform(get(GENERATE_BADGE_URL)
                        .header(API_KEY_HEADER, TEST_API_KEY))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(qrService);
    }

    /**
     * Verifies that a missing scan token returns HTTP 400
     * and does not invoke the QR service.
     */
    @Test
    void processScan_shouldReturn400_whenTokenIsMissing()
            throws Exception {

        mockMvc.perform(post(PROCESS_SCAN_URL)
                        .header(API_KEY_HEADER, TEST_API_KEY))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }

    /**
     * Verifies that using GET instead of POST for QR scanning
     * returns HTTP 405.
     */
    @Test
    void processScan_shouldReturn405_whenGetMethodIsUsed()
            throws Exception {

        mockMvc.perform(get(PROCESS_SCAN_URL)
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .param("token", "test-qr-token"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(qrService);
    }

    /**
     * Verifies that a valid badge-invalidation request returns HTTP 204
     * and delegates invalidation to the QR service.
     */
    @Test
    void invalidateBadge_shouldReturn204_whenRequestIsValid()
            throws Exception {

        doNothing().when(qrService)
                .invalidateBadgeOnCheckout(
                        any(CheckoutBadgeInvalidationRequest.class)
                );

        mockMvc.perform(post(INVALIDATE_BADGE_URL)
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidationRequest
                        )))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(qrService).invalidateBadgeOnCheckout(
                any(CheckoutBadgeInvalidationRequest.class)
        );
    }

    /**
     * Verifies that a missing invalidation request body returns HTTP 400
     * without invoking the QR service.
     */
    @Test
    void invalidateBadge_shouldReturn400_whenRequestBodyIsMissing()
            throws Exception {

        mockMvc.perform(post(INVALIDATE_BADGE_URL)
                        .header(API_KEY_HEADER, TEST_API_KEY)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }

    /**
     * Verifies that using GET instead of POST for badge invalidation
     * returns HTTP 405.
     */
    @Test
    void invalidateBadge_shouldReturn405_whenGetMethodIsUsed()
            throws Exception {

        mockMvc.perform(get(INVALIDATE_BADGE_URL)
                        .header(API_KEY_HEADER, TEST_API_KEY))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(qrService);
    }
}