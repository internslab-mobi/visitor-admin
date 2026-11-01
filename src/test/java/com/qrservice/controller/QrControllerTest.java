
package com.qrservice.controller;


import com.qrservice.dto.CheckoutBadgeInvalidationRequest;
import com.qrservice.dto.QrBadgeRequest;
import com.qrservice.dto.QrBadgeResponse;
import com.qrservice.dto.QrScanResponse;
import com.qrservice.service.QrService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(QrController.class)
@AutoConfigureMockMvc(addFilters = false)
class QrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QrService qrService;

    // Mock JPA mapping context to avoid auditing initialization failure
    @MockitoBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMappingContext;

    // Common test objects
    private QrBadgeRequest qrBadgeRequest;
    private QrBadgeResponse qrBadgeResponse;
    private QrScanResponse qrScanResponse;
    private CheckoutBadgeInvalidationRequest invalidationRequest;


    @BeforeEach
    void setUp() {

        qrBadgeRequest = new QrBadgeRequest(
                "VIS-123",
                "VTR-001",
                LocalDateTime.now()
        );

        qrBadgeResponse = new QrBadgeResponse(
                "sample-qr-token",
                "sample-qr-code"
        );

        qrScanResponse = new QrScanResponse(
                "Scan successful",
                "CHECK_IN",
                LocalDateTime.now()
        );

        invalidationRequest = new CheckoutBadgeInvalidationRequest(
                "VIS-123",
                LocalDateTime.now()
        );
    }


    @Test
    void generateQrBadge_shouldReturnOk() throws Exception {

        when(qrService.generateQrBadge(any(QrBadgeRequest.class)))
                .thenReturn(qrBadgeResponse);

        mockMvc.perform(post("/api/qr/generate-badge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qrBadgeRequest)))
                .andExpect(status().isOk());

        verify(qrService).generateQrBadge(any(QrBadgeRequest.class));
    }


    @Test
    void generateQrBadge_shouldReturnBadRequest_whenInvalidJson()
            throws Exception {

        mockMvc.perform(post("/api/qr/generate-badge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }


    @Test
    void processScan_shouldReturnOk() throws Exception {

        String token = "valid-qr-token";

        when(qrService.processScan(token))
                .thenReturn(qrScanResponse);

        mockMvc.perform(post("/api/qr/process-scan")
                        .param("token", token))
                .andExpect(status().isOk());

        verify(qrService).processScan(token);
    }


    @Test
    void processScan_shouldReturnBadRequest_whenTokenMissing()
            throws Exception {

        mockMvc.perform(post("/api/qr/process-scan"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }


    @Test
    void processScan_shouldReturnBadRequest_whenInvalidToken()
            throws Exception {

        String token = "invalid-token";

        when(qrService.processScan(token))
                .thenThrow(new IllegalArgumentException("Invalid QR token"));

        mockMvc.perform(post("/api/qr/process-scan")
                        .param("token", token))
                .andExpect(status().isBadRequest());

        verify(qrService).processScan(token);
    }


    @Test
    void invalidateBadge_shouldReturnNoContent() throws Exception {

        mockMvc.perform(post("/api/qr/invalidate-badge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidationRequest)))
                .andExpect(status().isNoContent());

        verify(qrService).invalidateBadgeOnCheckout(any());
    }


    @Test
    void invalidateBadge_shouldReturnBadRequest_whenInvalidJson()
            throws Exception {

        mockMvc.perform(post("/api/qr/invalidate-badge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(qrService);
    }

}
