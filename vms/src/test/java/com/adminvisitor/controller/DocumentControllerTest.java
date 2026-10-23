
package com.adminvisitor.controller;

import com.adminvisitor.VMSApplication;
import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.exception.ErrorResponseFactory;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.DocumentMapper;
import com.adminvisitor.service.DocumentService;
import com.adminvisitor.service.NdaService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
class DocumentControllerTest {

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @MockitoBean
    private NdaService ndaService;

    @MockitoBean
    private DocumentMapper documentMapper;

    @MockitoBean
    private ErrorResponseFactory errorResponseFactory;

    @TempDir
    Path tempDir;

    @Test
    void downloadNda_shouldReturnPdf() throws Exception {
        String visitorId = "VIS001";
        byte[] content = "sample NDA".getBytes();

        Resource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return "nda.pdf";
            }
        };

        when(ndaService.getNdaFile(visitorId))
                .thenReturn(resource);

        mockMvc.perform(get(
                        "/api/documents/{visitorId}/nda/download",
                        visitorId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        MediaType.APPLICATION_PDF))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"nda.pdf\""))
                .andExpect(content().bytes(content));

        verify(ndaService).getNdaFile(visitorId);
    }


    @Test
    void getAllDocuments_shouldReturnDocuments()
            throws Exception {

        String visitorId = "VIS001";

        when(documentService.getAllDocuments(visitorId))
                .thenReturn(List.of());

        mockMvc.perform(get(
                        "/api/documents/visitor/{visitorId}",
                        visitorId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(documentService).getAllDocuments(visitorId);
    }

    @Test
    void uploadProofDocuments_shouldReturnOk()
            throws Exception {

        String visitorId = "VIS001";

        MockMultipartFile file = new MockMultipartFile(
                "files",
                "aadhaar.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "proof document".getBytes()
        );

        DocumentMetadata metadata = mock(DocumentMetadata.class);
        DocumentResponse response = mock(DocumentResponse.class);

        when(documentService.uploadProofDocuments(
                eq(visitorId), anyList()))
                .thenReturn(List.of(metadata));

        when(documentMapper.toResponse(metadata))
                .thenReturn(response);

        mockMvc.perform(multipart(
                        "/api/documents/proof-documents/upload/{visitorId}",
                        visitorId)
                        .file(file))
                .andExpect(status().isOk());

        verify(documentService).uploadProofDocuments(
                eq(visitorId), anyList());

        verify(documentMapper).toResponse(metadata);
    }

    @Test
    void getVisitorPhoto_shouldReturnImage()
            throws Exception {

        String visitorId = "VIS001";
        byte[] content = "photo-content".getBytes();

        Path photo = tempDir.resolve("visitor.png");
        Files.write(photo, content);

        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(documentService.getLatestVisitorPhoto(visitorId))
                .thenReturn(metadata);

        when(metadata.getDocumentPath())
                .thenReturn(photo.toString());

        mockMvc.perform(get(
                        "/api/documents/visitor/{visitorId}/photo",
                        visitorId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        MediaType.IMAGE_PNG))
                .andExpect(content().bytes(content))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"visitor.png\""));

        verify(documentService).getLatestVisitorPhoto(visitorId);
    }

    @Test
    void viewDocument_shouldReturnFile()
            throws Exception {

        String documentId = "DOC001";
        byte[] content = "document-content".getBytes();

        Path document = tempDir.resolve("document.pdf");
        Files.write(document, content);

        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(documentService.getDocumentMetadata(documentId))
                .thenReturn(metadata);

        when(metadata.getDocumentPath())
                .thenReturn(document.toString());

        mockMvc.perform(get(
                        "/api/documents/{documentId}/view",
                        documentId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(
                        MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(content))
                .andExpect(header().string(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"document.pdf\""));

        verify(documentService).getDocumentMetadata(documentId);
    }

    @Test
    void viewDocument_whenFileDoesNotExist_shouldReturnNotFound()
            throws Exception {

        String documentId = "DOC001";

        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(documentService.getDocumentMetadata(documentId))
                .thenReturn(metadata);

        when(metadata.getDocumentPath())
                .thenReturn(tempDir.resolve("missing.pdf").toString());

        mockMvc.perform(get(
                        "/api/documents/{documentId}/view",
                        documentId))
                .andExpect(status().isNotFound());

        verify(documentService).getDocumentMetadata(documentId);
    }

    @Test
    void deleteDocument_shouldReturnNoContent()
            throws Exception {

        String metadataId = "META001";

        doNothing().when(documentService)
                .deleteDocument(metadataId);

        mockMvc.perform(delete(
                        "/api/documents/{metadataId}",
                        metadataId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(documentService).deleteDocument(metadataId);
    }

    @Test
    void getVisitorPhoto_whenFileDoesNotExist_shouldReturnNotFound()
            throws Exception {

        String visitorId = "VIS001";

        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(documentService.getLatestVisitorPhoto(visitorId))
                .thenReturn(metadata);

        when(metadata.getDocumentPath())
                .thenReturn(tempDir.resolve("missing.png").toString());

        mockMvc.perform(get(
                        "/api/documents/visitor/{visitorId}/photo",
                        visitorId))
                .andExpect(status().isNotFound());

        verify(documentService).getLatestVisitorPhoto(visitorId);
    }
}
