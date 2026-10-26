
package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.NdaStatus;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.ErrorResponseFactory;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.DocumentMapper;
import com.adminvisitor.repository.DocumentMetadataRepository;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import com.adminvisitor.service.DocumentStorageService;
import com.adminvisitor.service.IdGeneratorService;
import com.adminvisitor.service.impl.NdaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NdaServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private VisitorRepository visitorRepository;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private IdGeneratorService idGeneratorService;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private DocumentMapper documentMapper;



    @MockitoBean
    private ErrorResponseFactory errorResponseFactory;

    @InjectMocks
    private NdaServiceImpl ndaService;

    private static final String VISITOR_ID = "VIS001";
    private static final String METADATA_ID = "DMD001";

    private MockMultipartFile validPdf;
    private Visitor visitor;
    private Visit visit;
    private Document document;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(
                ndaService, "ndaUploadDir", "uploads/nda"
        );

        validPdf = new MockMultipartFile(
                "ndaFile",
                "nda.pdf",
                "application/pdf",
                "sample pdf content".getBytes()
        );

        visitor = mock(Visitor.class);
        visit = mock(Visit.class);
        document = mock(Document.class);
    }
    @Test
    void uploadNewNda_shouldSaveMetadataSuccessfully() throws IOException {
        LocalDate validUntil = LocalDate.now().plusDays(30);
        Path storedPath = Path.of("uploads/nda/nda.pdf");

        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(visitRepository.findTopByVisitorIdOrderByCreatedAtDesc(VISITOR_ID))
                .thenReturn(Optional.of(visit));
        when(visit.getVisitorType()).thenReturn(VisitorType.VENDOR);
        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.empty());
        when(documentRepository.findTopByVisitorIdOrderByCreatedAtDesc(VISITOR_ID))
                .thenReturn(Optional.of(document));
        when(idGeneratorService.generateId("DOCUMENT_METADATA", "DMD"))
                .thenReturn(METADATA_ID);
        when(documentStorageService.store(
                validPdf, "uploads/nda", VISITOR_ID, METADATA_ID))
                .thenReturn(storedPath);
        when(documentMetadataRepository.save(any(DocumentMetadata.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentMetadata result = ndaService.uploadNewNda(
                VISITOR_ID, validPdf, validUntil
        );

        assertNotNull(result);
        assertEquals(METADATA_ID, result.getId());
        assertEquals(DocumentType.NDA, result.getDocumentType());
        assertEquals(NdaStatus.ACTIVE, result.getStatus());
        assertEquals(validUntil.atTime(java.time.LocalTime.MAX), result.getValidUntil());
        assertEquals(storedPath.toString(), result.getDocumentPath());

        verify(documentMetadataRepository).save(any(DocumentMetadata.class));
    }

    @Test
    void uploadNewNda_shouldThrowWhenFileIsMissing() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.uploadNewNda(
                        VISITOR_ID, null, LocalDate.now().plusDays(10)
                )
        );

        verifyNoInteractions(visitorRepository, visitRepository);
    }

    @Test
    void uploadNewNda_shouldThrowWhenFileIsNotPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "ndaFile", "nda.txt", "text/plain", "content".getBytes()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.uploadNewNda(
                        VISITOR_ID, file, LocalDate.now().plusDays(10)
                )
        );

        verifyNoInteractions(visitorRepository);
    }

    @Test
    void uploadNewNda_shouldThrowWhenValidUntilIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.uploadNewNda(VISITOR_ID, validPdf, null)
        );

        verifyNoInteractions(visitorRepository);
    }

    @Test
    void uploadNewNda_shouldThrowWhenValidUntilIsInPast() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.uploadNewNda(
                        VISITOR_ID, validPdf, LocalDate.now().minusDays(1)
                )
        );

        verifyNoInteractions(visitorRepository);
    }

    @Test
    void uploadNewNda_shouldThrowWhenVisitorDoesNotExist() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> ndaService.uploadNewNda(
                        VISITOR_ID, validPdf, LocalDate.now().plusDays(10)
                )
        );

        verifyNoInteractions(visitRepository);
    }

    @Test
    void uploadNewNda_shouldThrowWhenVisitDoesNotExist() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(visitRepository.findTopByVisitorIdOrderByCreatedAtDesc(VISITOR_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> ndaService.uploadNewNda(
                        VISITOR_ID, validPdf, LocalDate.now().plusDays(10)
                )
        );
    }

    @Test
    void uploadNewNda_shouldThrowWhenVisitorIsNotVendor() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(visitRepository.findTopByVisitorIdOrderByCreatedAtDesc(VISITOR_ID))
                .thenReturn(Optional.of(visit));
        when(visit.getVisitorType()).thenReturn(VisitorType.VISITOR);

        assertThrows(
                BusinessRuleException.class,
                () -> ndaService.uploadNewNda(
                        VISITOR_ID, validPdf, LocalDate.now().plusDays(10)
                )
        );
    }

    @Test
    void uploadNewNda_shouldThrowWhenExistingNdaIsStillValid() {
        DocumentMetadata existingNda = mock(DocumentMetadata.class);

        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(visitRepository.findTopByVisitorIdOrderByCreatedAtDesc(VISITOR_ID))
                .thenReturn(Optional.of(visit));
        when(visit.getVisitorType()).thenReturn(VisitorType.VENDOR);
        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.of(existingNda));
        when(existingNda.getValidUntil())
                .thenReturn(LocalDateTime.now().plusDays(30));

        assertThrows(
                BusinessRuleException.class,
                () -> ndaService.uploadNewNda(
                        VISITOR_ID, validPdf, LocalDate.now().plusDays(60)
                )
        );

        verify(documentMetadataRepository, never())
                .save(any(DocumentMetadata.class));
    }

    @Test
    void uploadNewNda_shouldThrowWhenDocumentDoesNotExist() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(visitRepository.findTopByVisitorIdOrderByCreatedAtDesc(VISITOR_ID))
                .thenReturn(Optional.of(visit));
        when(visit.getVisitorType()).thenReturn(VisitorType.VENDOR);
        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.empty());
        when(documentRepository.findTopByVisitorIdOrderByCreatedAtDesc(VISITOR_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> ndaService.uploadNewNda(
                        VISITOR_ID, validPdf, LocalDate.now().plusDays(10)
                )
        );
    }

    @Test
    void extendNdaValidity_shouldThrowWhenSupportingDocumentIsMissing() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.extendNdaValidity(
                        VISITOR_ID, null, LocalDate.now().plusDays(30)
                )
        );

        verifyNoInteractions(visitorRepository);
    }

    @Test
    void extendNdaValidity_shouldThrowWhenNewExpiryDateIsInPast() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.extendNdaValidity(
                        VISITOR_ID, validPdf, LocalDate.now().minusDays(1)
                )
        );

        verifyNoInteractions(visitorRepository);
    }

    @Test
    void extendNdaValidity_shouldThrowWhenVisitorDoesNotExist() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> ndaService.extendNdaValidity(
                        VISITOR_ID, validPdf, LocalDate.now().plusDays(30)
                )
        );
    }

    @Test
    void extendNdaValidity_shouldThrowWhenExistingNdaDoesNotExist() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> ndaService.extendNdaValidity(
                        VISITOR_ID, validPdf, LocalDate.now().plusDays(30)
                )
        );
    }

    @Test
    void extendNdaValidity_shouldThrowWhenExistingNdaIsExpired() {
        DocumentMetadata existingNda = mock(DocumentMetadata.class);

        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.of(existingNda));
        when(existingNda.getValidUntil())
                .thenReturn(LocalDateTime.now().minusDays(1));

        assertThrows(
                BusinessRuleException.class,
                () -> ndaService.extendNdaValidity(
                        VISITOR_ID, validPdf, LocalDate.now().plusDays(60)
                )
        );
    }

    @Test
    void extendNdaValidity_shouldThrowWhenNewExpiryIsNotLater() {
        DocumentMetadata existingNda = mock(DocumentMetadata.class);
        LocalDateTime existingExpiry = LocalDateTime.now().plusDays(30);

        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.of(existingNda));
        when(existingNda.getValidUntil()).thenReturn(existingExpiry);

        assertThrows(
                BusinessRuleException.class,
                () -> ndaService.extendNdaValidity(
                        VISITOR_ID, validPdf, existingExpiry.toLocalDate()
                )
        );
    }

    @Test
    void extendNdaValidity_shouldThrowWhenExtensionIsFiveYearsOrMore() {
        DocumentMetadata existingNda = mock(DocumentMetadata.class);
        LocalDateTime existingExpiry = LocalDateTime.now().plusDays(30);

        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.of(existingNda));
        when(existingNda.getValidUntil()).thenReturn(existingExpiry);

        assertThrows(
                BusinessRuleException.class,
                () -> ndaService.extendNdaValidity(
                        VISITOR_ID, validPdf,
                        existingExpiry.toLocalDate().plusYears(5)
                )
        );
    }

    @Test
    void getLatestNda_shouldReturnLatestMetadata() {
        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findFirstByDocument_Visitor_IdAndDocumentTypeInAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        eq(VISITOR_ID), anyList()))
                .thenReturn(Optional.of(metadata));

        assertSame(metadata, ndaService.getLatestNda(VISITOR_ID));
    }

    @Test
    void getLatestNda_shouldThrowWhenVisitorDoesNotExist() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> ndaService.getLatestNda(VISITOR_ID)
        );
    }

    @Test
    void getLatestNda_shouldThrowWhenNoNdaExists() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findFirstByDocument_Visitor_IdAndDocumentTypeInAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        eq(VISITOR_ID), anyList()))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> ndaService.getLatestNda(VISITOR_ID)
        );
    }

    @Test
    void getNdaHistory_shouldReturnMappedHistory() {
        DocumentMetadata metadata = mock(DocumentMetadata.class);
        DocumentResponse response = mock(DocumentResponse.class);

        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findByDocument_Visitor_IdAndDocumentTypeInOrderByCreatedAtDesc(
                        eq(VISITOR_ID), anyList()))
                .thenReturn(List.of(metadata));
        when(documentMapper.toResponse(metadata)).thenReturn(response);

        List<DocumentResponse> result = ndaService.getNdaHistory(VISITOR_ID);

        assertEquals(1, result.size());
        assertSame(response, result.get(0));
    }

    @Test
    void getNdaHistory_shouldReturnEmptyListWhenNoHistoryExists() {
        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findByDocument_Visitor_IdAndDocumentTypeInOrderByCreatedAtDesc(
                        eq(VISITOR_ID), anyList()))
                .thenReturn(List.of());

        List<DocumentResponse> result = ndaService.getNdaHistory(VISITOR_ID);

        assertTrue(result.isEmpty());
        verifyNoInteractions(documentMapper);
    }

    @Test
    void isNdaRequired_shouldReturnTrueWhenNoNdaExists() {
        when(visitor.getId()).thenReturn(VISITOR_ID);

        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.empty());

        assertTrue(ndaService.isNdaRequired(visitor));
    }

    @Test
    void isNdaRequired_shouldReturnTrueWhenNdaIsExpired() {
        when(visitor.getId()).thenReturn(VISITOR_ID);
        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.of(metadata));
        when(metadata.getValidUntil())
                .thenReturn(LocalDateTime.now().minusDays(1));

        assertTrue(ndaService.isNdaRequired(visitor));
    }

    @Test
    void isNdaRequired_shouldReturnFalseWhenNdaIsValid() {

        when(visitor.getId()).thenReturn(VISITOR_ID);
        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        VISITOR_ID, DocumentType.NDA))
                .thenReturn(Optional.of(metadata));
        when(metadata.getValidUntil())
                .thenReturn(LocalDateTime.now().plusDays(10));

        assertFalse(ndaService.isNdaRequired(visitor));
    }

    @Test
    void getNdaFile_shouldReturnResourceWhenFileExists() throws IOException {
        Path tempFile = Files.createTempFile("nda-test", ".pdf");

        try {
            DocumentMetadata metadata = mock(DocumentMetadata.class);

            when(visitorRepository.findById(VISITOR_ID))
                    .thenReturn(Optional.of(visitor));
            when(documentMetadataRepository
                    .findFirstByDocument_Visitor_IdAndDocumentTypeInAndOverwrittenByIsNullOrderByCreatedAtDesc(
                            eq(VISITOR_ID), anyList()))
                    .thenReturn(Optional.of(metadata));
            when(metadata.getDocumentPath()).thenReturn(tempFile.toString());

            Resource result = ndaService.getNdaFile(VISITOR_ID);

            assertNotNull(result);
            assertTrue(result.exists());
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void getNdaFile_shouldThrowWhenFileDoesNotExist() {
        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(visitorRepository.findById(VISITOR_ID))
                .thenReturn(Optional.of(visitor));
        when(documentMetadataRepository
                .findFirstByDocument_Visitor_IdAndDocumentTypeInAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        eq(VISITOR_ID), anyList()))
                .thenReturn(Optional.of(metadata));
        when(metadata.getDocumentPath())
                .thenReturn("non-existent-nda-file.pdf");

        assertThrows(
                IllegalArgumentException.class,
                () -> ndaService.getNdaFile(VISITOR_ID)
        );
    }

    @Test
    void updateExpiredNdaStatuses_shouldMarkExpiredRecordsAndSaveThem() {
        DocumentMetadata metadata = mock(DocumentMetadata.class);

        when(documentMetadataRepository
                .findByDocumentTypeAndStatusAndValidUntilLessThanEqual(
                        eq(DocumentType.NDA),
                        eq(NdaStatus.ACTIVE),
                        any(LocalDateTime.class)))
                .thenReturn(List.of(metadata));

        ndaService.updateExpiredNdaStatuses();

        verify(metadata).setStatus(NdaStatus.EXPIRED);
        verify(documentMetadataRepository).saveAll(List.of(metadata));
    }

    @Test
    void updateExpiredNdaStatuses_shouldSaveEmptyListWhenNoRecordsAreExpired() {
        when(documentMetadataRepository
                .findByDocumentTypeAndStatusAndValidUntilLessThanEqual(
                        eq(DocumentType.NDA),
                        eq(NdaStatus.ACTIVE),
                        any(LocalDateTime.class)))
                .thenReturn(List.of());

        ndaService.updateExpiredNdaStatuses();

        verify(documentMetadataRepository).saveAll(List.of());
    }
}
