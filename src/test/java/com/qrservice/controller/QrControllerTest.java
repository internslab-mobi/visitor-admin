
package com.qrservice.controller;

import tools.jackson.databind.ObjectMapper;
import com.qrservice.dto.CheckoutBadgeInvalidationRequest;
import com.qrservice.dto.QrBadgeRequest;

import com.qrservice.service.QrService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QrController.class)
class QrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QrService qrService;

    private QrBadgeRequest badgeRequest;
    private CheckoutBadgeInvalidationRequest invalidationRequest;

    @BeforeEach
    void setUp() {
        badgeRequest = new QrBadgeRequest(

                "V001",
                LocalDateTime.parse("2026-10-26T09:00:00"),
                "Test Visitor",

                LocalDateTime.parse("2026-10-26T09:15:00")
        );
    }

    @Test
    void generateQrBadge_shouldReturn200_whenRequestIsValid()
            throws Exception {

        when(qrService.generateQrBadge(
                org.mockito.ArgumentMatchers.any(QrBadgeRequest.class)))
                .thenReturn(null);

        mockMvc.perform(post("/api/qr/generate-badge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                badgeRequest)))
                .andExpect(status().isOk());

        verify(qrService).generateQrBadge(
                org.mockito.ArgumentMatchers.any(QrBadgeRequest.class));
    }

    @Test
    void generateQrBadge_shouldReturn400_whenRequestBodyIsMissing()
            throws Exception {

        mockMvc.perform(post("/api/qr/generate-badge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }

    @Test
    void generateQrBadge_shouldReturn405_whenGetMethodIsUsed()
            throws Exception {

        mockMvc.perform(get("/api/qr/generate-badge"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(qrService);
    }


    @Test
    void processScan_shouldReturn200_whenTokenIsProvided()
            throws Exception {

        String token = "test-qr-token";

        when(qrService.processScan(token))
                .thenReturn(null);

        mockMvc.perform(post("/api/qr/process-scan")
                        .param("token", token))
                .andExpect(status().isOk());

        verify(qrService).processScan(token);
    }

    @Test
    void processScan_shouldReturn400_whenTokenIsMissing()
            throws Exception {

        mockMvc.perform(post("/api/qr/process-scan"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }

    @Test
    void processScan_shouldReturn405_whenGetMethodIsUsed()
            throws Exception {

        mockMvc.perform(get("/api/qr/process-scan")
                        .param("token", "test-qr-token"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(qrService);
    }


    @Test
    void invalidateBadge_shouldReturn204_whenRequestIsValid()
            throws Exception {

        doNothing().when(qrService)
                .invalidateBadgeOnCheckout(
                        org.mockito.ArgumentMatchers.any(
                                CheckoutBadgeInvalidationRequest.class));

        mockMvc.perform(post("/api/qr/invalidate-badge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                invalidationRequest)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(qrService).invalidateBadgeOnCheckout(
                org.mockito.ArgumentMatchers.any(
                        CheckoutBadgeInvalidationRequest.class));
    }

    @Test
    void invalidateBadge_shouldReturn400_whenRequestBodyIsMissing()
            throws Exception {

        mockMvc.perform(post("/api/qr/invalidate-badge")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }

    @Test
    void invalidateBadge_shouldReturn405_whenGetMethodIsUsed()
            throws Exception {

        mockMvc.perform(get("/api/qr/invalidate-badge"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(qrService);
    }
}
