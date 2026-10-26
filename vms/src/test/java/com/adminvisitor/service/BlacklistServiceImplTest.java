package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.BlacklistProof;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.BlacklistAlreadyRemovedException;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.ResourceAlreadyExistsException;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.BlacklistMapper;
import com.adminvisitor.repository.BlacklistProofRepository;
import com.adminvisitor.repository.BlacklistRepository;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import com.adminvisitor.service.IdGeneratorService;
import com.adminvisitor.service.ProofSecurityService;
import com.adminvisitor.service.impl.BlacklistServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceImplTest {

    @Mock
    private BlacklistRepository blacklistRepository;

    @Mock
    private BlacklistProofRepository blacklistProofRepository;

    @Mock
    private VisitorRepository visitorRepository;

    @Mock
    private BlacklistMapper blacklistMapper;

    @Mock
    private IdGeneratorService idGeneratorService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private ProofSecurityService proofSecurityService;

    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private BlacklistServiceImpl blacklistService;

    private Visitor visitor;
    private Document domesticDocument;
    private BlacklistRequest request;
    private BlacklistResponse response;

    @BeforeEach
    void setUp() {
        visitor = Visitor.builder()
                .id("VTR-001")
                .build();

        domesticDocument = Document.builder()
                .nationality(
                        com.adminvisitor.enums.Nationality.DOMESTIC
                )
                .aadharNumber("AADHAAR-INDEX-001")
                .panNumber("PAN-INDEX-001")
                .build();

        request = new BlacklistRequest(
                "Security violation",
                "EMP-001"
        );

        response = mock(BlacklistResponse.class);

        when(blacklistMapper.toResponse(
                any(Blacklist.class),
                nullable(VisitorType.class),
                anyList()
        )).thenReturn(response);

        lenient().when(idGeneratorService.generateId(
                anyString(),
                anyString()
        )).thenAnswer(invocation ->
                invocation.getArgument(1) + "-001"
        );
    }

    // --------------------------------------------------
    // isBlacklisted()
    // --------------------------------------------------

    @Test
    void isBlacklisted_whenActiveRecordExists_returnsTrue() {
        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.of(mock(Blacklist.class)));

        assertTrue(blacklistService.isBlacklisted("VTR-001"));
    }

    @Test
    void isBlacklisted_whenNoActiveRecordExists_returnsFalse() {
        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertFalse(blacklistService.isBlacklisted("VTR-001"));
    }

    // --------------------------------------------------
    // isBlacklistedByProof()
    // --------------------------------------------------

    @Test
    void isBlacklistedByProof_whenMatchingActiveProofExists_returnsTrue() {
        when(proofSecurityService.generateBlindIndex(
                ProofType.AADHAAR, "123456789012"
        )).thenReturn("blind-index");

        when(blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        ProofType.AADHAAR,
                        "blind-index",
                        BlacklistStatus.ACTIVE
                )).thenReturn(Optional.of(mock(BlacklistProof.class)));

        assertTrue(blacklistService.isBlacklistedByProof(
                ProofType.AADHAAR, "123456789012"
        ));
    }

    @Test
    void isBlacklistedByProof_whenNoMatchingProofExists_returnsFalse() {
        when(proofSecurityService.generateBlindIndex(
                ProofType.AADHAAR, "123456789012"
        )).thenReturn("blind-index");

        when(blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        ProofType.AADHAAR,
                        "blind-index",
                        BlacklistStatus.ACTIVE
                )).thenReturn(Optional.empty());

        assertFalse(blacklistService.isBlacklistedByProof(
                ProofType.AADHAAR, "123456789012"
        ));
    }

    // --------------------------------------------------
    // addExistingVisitorToBlacklist()
    // --------------------------------------------------

    @Test
    void addExistingVisitor_whenVisitorDoesNotExist_throwsNotFound() {
        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> blacklistService.addExistingVisitorToBlacklist(
                        "VTR-001", request
                )
        );

        verifyNoInteractions(documentRepository);
        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void addExistingVisitor_whenDocumentDoesNotExist_throwsNotFound() {
        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> blacklistService.addExistingVisitorToBlacklist(
                        "VTR-001", request
                )
        );

        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void addExistingVisitor_whenAlreadyActivelyBlacklisted_throwsConflict() {
        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.of(domesticDocument));

        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.of(mock(Blacklist.class)));

        assertThrows(
                ResourceAlreadyExistsException.class,
                () -> blacklistService.addExistingVisitorToBlacklist(
                        "VTR-001", request
                )
        );

        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void addExistingVisitor_whenDomesticProofAlreadyBlacklisted_throwsConflict() {
        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.of(domesticDocument));

        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.empty());

        when(blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        ProofType.AADHAAR,
                        "AADHAAR-INDEX-001",
                        BlacklistStatus.ACTIVE
                )).thenReturn(Optional.of(mock(BlacklistProof.class)));

        assertThrows(
                ResourceAlreadyExistsException.class,
                () -> blacklistService.addExistingVisitorToBlacklist(
                        "VTR-001", request
                )
        );

        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void addExistingVisitor_whenNoValidProofExists_throwsBusinessRuleException() {
        Document documentWithoutProofs = Document.builder()
                .nationality(
                        com.adminvisitor.enums.Nationality.DOMESTIC
                )
                .build();

        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.of(documentWithoutProofs));

        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThrows(
                BusinessRuleException.class,
                () -> blacklistService.addExistingVisitorToBlacklist(
                        "VTR-001", request
                )
        );

        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void addExistingVisitor_whenDomesticProofsAreAvailable_savesBlacklist() {
        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.of(domesticDocument));

        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.empty());

        when(blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        any(ProofType.class),
                        anyString(),
                        eq(BlacklistStatus.ACTIVE)
                )).thenReturn(Optional.empty());

        when(blacklistRepository.save(any(Blacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BlacklistResponse result =
                blacklistService.addExistingVisitorToBlacklist(
                        "VTR-001", request
                );

        assertSame(response, result);

        verify(blacklistRepository).save(argThat(blacklist ->
                blacklist.getStatus() == BlacklistStatus.ACTIVE
                        && blacklist.getProofs().size() == 2
        ));
    }

    @Test
    void addExistingVisitor_whenInternationalPassportExists_savesPassportProof() {
        Document internationalDocument = Document.builder()
                .nationality(
                        com.adminvisitor.enums.Nationality.INTERNATIONAL
                )
                .passportNumber("PASSPORT-INDEX-001")
                .build();

        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.of(internationalDocument));

        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.empty());

        when(blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        ProofType.PASSPORT,
                        "PASSPORT-INDEX-001",
                        BlacklistStatus.ACTIVE
                )).thenReturn(Optional.empty());

        when(blacklistRepository.save(any(Blacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        blacklistService.addExistingVisitorToBlacklist(
                "VTR-001", request
        );

        verify(blacklistRepository).save(argThat(blacklist ->
                blacklist.getProofs().size() == 1
                        && blacklist.getProofs().get(0).getProofType()
                        == ProofType.PASSPORT
        ));
    }

    // --------------------------------------------------
    // removeFromBlacklist()
    // --------------------------------------------------

    @Test
    void removeFromBlacklist_whenRecordDoesNotExist_throwsNotFound() {
        when(blacklistRepository.findById("BL-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> blacklistService.removeFromBlacklist(
                        "BL-001", "EMP-001"
                )
        );

        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void removeFromBlacklist_whenAlreadyRemoved_throwsConflict() {
        Blacklist blacklist = Blacklist.builder()
                .id("BL-001")
                .visitor(visitor)
                .status(BlacklistStatus.REMOVED)
                .proofs(new ArrayList<>())
                .build();

        when(blacklistRepository.findById("BL-001"))
                .thenReturn(Optional.of(blacklist));

        assertThrows(
                BlacklistAlreadyRemovedException.class,
                () -> blacklistService.removeFromBlacklist(
                        "BL-001", "EMP-001"
                )
        );

        verify(blacklistRepository, never()).save(any());
    }

    @Test
    void removeFromBlacklist_whenActiveRecordExists_marksAsRemoved() {
        Blacklist blacklist = Blacklist.builder()
                .id("BL-001")
                .visitor(visitor)
                .status(BlacklistStatus.ACTIVE)
                .proofs(new ArrayList<>())
                .build();

        when(blacklistRepository.findById("BL-001"))
                .thenReturn(Optional.of(blacklist));

        when(blacklistRepository.save(any(Blacklist.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BlacklistResponse result =
                blacklistService.removeFromBlacklist(
                        "BL-001", "EMP-001"
                );

        assertSame(response, result);
        assertEquals(BlacklistStatus.REMOVED, blacklist.getStatus());
        assertEquals("EMP-001", blacklist.getUpdatedBy());

        verify(blacklistRepository).save(blacklist);
    }

    // --------------------------------------------------
    // getAllBlacklistRecords()
    // --------------------------------------------------

    @Test
    void getAllBlacklistRecords_whenNoRecordsExist_returnsEmptyList() {
        when(blacklistRepository.findAll()).thenReturn(List.of());

        List<BlacklistResponse> result =
                blacklistService.getAllBlacklistRecords();

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyNoInteractions(visitRepository);
        verifyNoInteractions(blacklistProofRepository);
    }

    // --------------------------------------------------
    // getBlacklistById()
    // --------------------------------------------------

    @Test
    void getBlacklistById_whenRecordDoesNotExist_throwsNotFound() {
        when(blacklistRepository.findById("BL-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> blacklistService.getBlacklistById("BL-001")
        );
    }

    @Test
    void getBlacklistById_whenRecordExists_returnsMappedResponse() {
        Blacklist blacklist = Blacklist.builder()
                .id("BL-001")
                .visitor(visitor)
                .status(BlacklistStatus.ACTIVE)
                .proofs(new ArrayList<>())
                .build();

        when(blacklistRepository.findById("BL-001"))
                .thenReturn(Optional.of(blacklist));

        when(blacklistProofRepository.findByBlacklistId("BL-001"))
                .thenReturn(List.of());

        when(visitRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.empty());

        BlacklistResponse result =
                blacklistService.getBlacklistById("BL-001");

        assertSame(response, result);

        verify(blacklistMapper).toResponse(
                eq(blacklist),
                isNull(),
                anyList()
        );
    }
}
