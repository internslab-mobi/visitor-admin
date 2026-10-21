package com.adminvisitor.controller;
import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.mapper.DocumentMapper;
import com.adminvisitor.service.NdaService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.adminvisitor.exception.ErrorResponseFactory;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(NdaController.class)
public class NdaControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private NdaService ndaService;
    @MockitoBean
    private DocumentMapper documentMapper;
    @MockitoBean
    private ErrorResponseFactory errorResponseFactory;



    @Test
    void uploadNewNda_shouldReturnOk() throws Exception {

        String visitorId = "VIS001";
        LocalDate validUntil = LocalDate.now().plusDays(30);

        MockMultipartFile file = new MockMultipartFile(
                "ndaFile",
                "nda.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "sample NDA content".getBytes()
        );

        DocumentMetadata metadata = mock(DocumentMetadata.class);
        DocumentResponse response = mock(DocumentResponse.class);

        when(ndaService.uploadNewNda(
                eq(visitorId), any(), eq(validUntil)
        )).thenReturn(metadata);

        when(documentMapper.toResponse(metadata)).thenReturn(response);

        mockMvc.perform(multipart(
                        "/api/nda/{visitorId}/upload-new",
                        visitorId)
                        .file(file)
                        .param("validUntil", validUntil.toString()))
                .andExpect(status().isOk());

        verify(ndaService).uploadNewNda(
                eq(visitorId), any(), eq(validUntil)
        );

        verify(documentMapper).toResponse(metadata);
    }

    @Test
    void extendNdaValidity_shouldReturnOk() throws Exception {
        String visitorId = "VIS001";
        LocalDate newValidUntil = LocalDate.of(2027, 12, 31);

        MockMultipartFile file = new MockMultipartFile(
                "supportingDocument",
                "support.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "supporting document".getBytes()
        );

        DocumentMetadata metadata = mock(DocumentMetadata.class);
        DocumentResponse response = mock(DocumentResponse.class);

        when(ndaService.extendNdaValidity(
                eq(visitorId), any(), eq(newValidUntil)
        )).thenReturn(metadata);

        when(documentMapper.toResponse(metadata))
                .thenReturn(response);

        mockMvc.perform(multipart(
                        "/api/nda/{visitorId}/extend",
                        visitorId)
                        .file(file)
                        .param("newValidUntil", "2027-12-31"))
                .andExpect(status().isOk());

        verify(ndaService).extendNdaValidity(
                eq(visitorId), any(), eq(newValidUntil)
        );

        verify(documentMapper).toResponse(metadata);
    }

    @Test
    void getLatestNda_shouldReturnOk() throws Exception {
        String visitorId = "VIS001";

        DocumentMetadata metadata = mock(DocumentMetadata.class);
        DocumentResponse response = mock(DocumentResponse.class);

        when(ndaService.getLatestNda(visitorId))
                .thenReturn(metadata);

        when(documentMapper.toResponse(metadata))
                .thenReturn(response);

        mockMvc.perform(get(
                        "/api/nda/{visitorId}/latest",
                        visitorId))
                .andExpect(status().isOk());

        verify(ndaService).getLatestNda(visitorId);
        verify(documentMapper).toResponse(metadata);
    }

    @Test
        void getNdaHistory_shouldReturnOk() throws Exception {
        String visitorId = "VIS001";

        when(ndaService.getNdaHistory(visitorId))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/nda/{visitorId}/history", visitorId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_JSON
                ))
                .andExpect(content().json("[]"));

        verify(ndaService).getNdaHistory(visitorId);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void uploadNewNda_withMissingFile_shouldRejectRequest()
            throws Exception {

        mockMvc.perform(multipart(
                        "/api/nda/{visitorId}/upload-new",
                        "VIS001")
                        .param("validUntil", "2027-12-31"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ndaService);
    }

    @Test
    void uploadNewNda_withInvalidDate_shouldRejectRequest()
            throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "ndaFile",
                "nda.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "sample NDA".getBytes()
        );

        mockMvc.perform(multipart(
                        "/api/nda/{visitorId}/upload-new",
                        "VIS001")
                        .file(file)
                        .param("validUntil", "not-a-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ndaService);
    }

    @Test
    void extendNdaValidity_withMissingFile_shouldRejectRequest()
            throws Exception {

        mockMvc.perform(multipart(
                        "/api/nda/{visitorId}/extend",
                        "VIS001")
                        .param("newValidUntil", "2027-12-31"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ndaService);
    }

    @Test
    void extendNdaValidity_withInvalidDate_shouldRejectRequest()
            throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "supportingDocument",
                "support.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "supporting document".getBytes()
        );

        mockMvc.perform(multipart(
                        "/api/nda/{visitorId}/extend",
                        "VIS001")
                        .file(file)
                        .param("newValidUntil", "invalid-date"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(ndaService);
    }
}
