
package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.DocumentType;

import com.adminvisitor.enums.ProofType;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.DocumentMapper;
import com.adminvisitor.repository.*;
import com.adminvisitor.service.impl.DocumentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceImplTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private VisitorRepository visitorRepository;

    @Mock
    private IdGeneratorService idGeneratorService;

    @Mock
    private VisitRepository visitRepository;

    @Mock
    private DocumentMetadataRepository documentMetadataRepository;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private DocumentStorageService documentStorageService;

    @Mock
    private ProofSecurityService proofSecurityService;

    @InjectMocks
    private DocumentServiceImpl documentService;

    private Visitor visitor;
    private Document document;
    private DocumentMetadata metadata;
    private Visit visit;

    @BeforeEach
    void setUp() {

        visitor = mock(Visitor.class);

        document = mock(Document.class);

        visit = mock(Visit.class);

        metadata = mock(DocumentMetadata.class);
    }
    @Test
    void getLatestVisitorPhoto_shouldReturnMetadata_whenPhotoExists() {

        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        "VTR-001", DocumentType.VISITOR_PHOTO))
                .thenReturn(Optional.of(metadata));

        DocumentMetadata result =
                documentService.getLatestVisitorPhoto("VTR-001");

        assertNotNull(result);
        assertSame(metadata, result);

        verify(documentMetadataRepository)
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        "VTR-001", DocumentType.VISITOR_PHOTO);
    }

    @Test
    void getLatestVisitorPhoto_shouldThrowException_whenPhotoDoesNotExist() {

        when(documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        "VTR-001", DocumentType.VISITOR_PHOTO))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.getLatestVisitorPhoto("VTR-001")
        );
    }



    @Test
    void getLatestIdentityDocument_shouldReturnDocument_whenExists() {

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.of(document));

        Document result =
                documentService.getLatestIdentityDocument("VTR-001");

        assertNotNull(result);
        assertSame(document, result);
    }

    @Test
    void getLatestIdentityDocument_shouldThrowException_whenNotExists() {

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.getLatestIdentityDocument("VTR-001")
        );
    }




    @Test
    void verifyIdentityProof_shouldThrowException_whenVisitorIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.verifyIdentityProof(
                        null, ProofType.AADHAAR, "123456789012")
        );

        verifyNoInteractions(proofSecurityService);
    }

    @Test
    void verifyIdentityProof_shouldThrowException_whenProofTypeIsNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.verifyIdentityProof(
                        visitor, null, "123456789012")
        );
    }

    @Test
    void verifyIdentityProof_shouldThrowException_whenProofNumberIsBlank() {

        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.verifyIdentityProof(
                        visitor, ProofType.AADHAAR, " ")
        );
    }
    @Test
    void getVisitPhoto_shouldReturnMetadata_whenPhotoExists() {

        when(documentMetadataRepository
                .findTopByVisit_IdAndDocumentTypeOrderByCreatedAtDesc(
                        "VIS-001", DocumentType.VISITOR_PHOTO))
                .thenReturn(Optional.of(metadata));

        DocumentMetadata result =
                documentService.getVisitPhoto("VIS-001");

        assertNotNull(result);
        assertSame(metadata, result);
    }

    @Test
    void getVisitPhoto_shouldThrowException_whenPhotoDoesNotExist() {

        when(documentMetadataRepository
                .findTopByVisit_IdAndDocumentTypeOrderByCreatedAtDesc(
                        "VIS-001", DocumentType.VISITOR_PHOTO))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.getVisitPhoto("VIS-001")
        );
    }


    @Test
    void getAllDocuments_shouldReturnDocuments_whenVisitorExists() {

        DocumentResponse response = mock(DocumentResponse.class);

        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentMetadataRepository
                .findByDocument_Visitor_IdAndDocumentTypeIsNull("VTR-001"))
                .thenReturn(List.of(metadata));

        when(documentMapper.toResponse(metadata))
                .thenReturn(response);

        List<DocumentResponse> result =
                documentService.getAllDocuments("VTR-001");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(response, result.get(0));

        verify(visitorRepository).findById("VTR-001");
        verify(documentMapper).toResponse(metadata);
    }

    @Test
    void getAllDocuments_shouldThrowException_whenVisitorDoesNotExist() {

        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.getAllDocuments("VTR-001")
        );

        verifyNoInteractions(documentMetadataRepository);
        verifyNoInteractions(documentMapper);
    }

    @Test
    void getDocumentMetadata_shouldReturnMetadata_whenExists() {

        when(documentMetadataRepository.findById("DMD-001"))
                .thenReturn(Optional.of(metadata));

        DocumentMetadata result =
                documentService.getDocumentMetadata("DMD-001");

        assertNotNull(result);
        assertSame(metadata, result);
    }

    @Test
    void getDocumentMetadata_shouldThrowException_whenNotExists() {

        when(documentMetadataRepository.findById("DMD-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.getDocumentMetadata("DMD-001")
        );
    }


    @Test
    void uploadVisitorPhoto_shouldThrowException_whenFileIsEmpty() {

        MultipartFile file = new MockMultipartFile(
                "photo", "photo.png", "image/png", new byte[0]);

        assertThrows(
                IllegalArgumentException.class,
                () -> documentService.uploadVisitorPhoto(
                        "VTR-001", "VIS-001", file)
        );

        verifyNoInteractions(visitorRepository);
    }

    @Test
    void uploadVisitorPhoto_shouldThrowException_whenVisitorDoesNotExist() {

        MultipartFile file = new MockMultipartFile(
                "photo", "photo.png", "image/png", "image".getBytes());

        when(visitorRepository.existsById("VTR-001"))
                .thenReturn(false);

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.uploadVisitorPhoto(
                        "VTR-001", "VIS-001", file)
        );

        verifyNoInteractions(visitRepository);
    }



    @Test
    void deleteDocument_shouldDeleteMetadata_whenDocumentExists() {

        Document associatedDocument = mock(Document.class);

        when(metadata.getDocumentPath()).thenReturn(" ");
        when(metadata.getDocument()).thenReturn(associatedDocument);

        when(documentMetadataRepository.findById("DMD-001"))
                .thenReturn(Optional.of(metadata));

        documentService.deleteDocument("DMD-001");

        verify(documentMetadataRepository).delete(metadata);
    }

    @Test
    void deleteDocument_shouldThrowException_whenMetadataDoesNotExist() {

        when(documentMetadataRepository.findById("DMD-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> documentService.deleteDocument("DMD-001")
        );

        verify(documentMetadataRepository, never())
                .delete(any());
    }
}
