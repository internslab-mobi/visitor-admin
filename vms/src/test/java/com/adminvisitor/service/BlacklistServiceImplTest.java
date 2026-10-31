
package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.BlacklistProof;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
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

/**
 * Unit tests for {@link BlacklistServiceImpl}.
 *
 * <p>Verifies blacklist lookup, creation, removal, and retrieval behavior
 * using mocked repositories and collaborators.</p>
 */
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

    /**
     * Creates common test data before each test.
     *
     * <p>Shared setup deliberately avoids Mockito stubbing so each test only
     * defines the interactions it actually needs.</p>
     */
    @BeforeEach
    void setUp() {
        visitor = new Visitor();
        visitor.setId("VTR-001");

        domesticDocument = new Document();
        domesticDocument.setNationality(Nationality.DOMESTIC);
        domesticDocument.setAadharNumber("AADHAAR-INDEX-001");
        domesticDocument.setPanNumber("PAN-INDEX-001");

        request = new BlacklistRequest(
                "Security violation",
                "EMP-001"
        );

        response = mock(BlacklistResponse.class);
    }

    /**
     * Verifies that an active blacklist record makes the visitor blacklisted.
     */
    @Test
    void isBlacklisted_whenActiveRecordExists_returnsTrue() {
        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.of(mock(Blacklist.class)));

        assertTrue(blacklistService.isBlacklisted("VTR-001"));
    }

    /**
     * Verifies that a visitor without an active blacklist record is not blacklisted.
     */
    @Test
    void isBlacklisted_whenNoActiveRecordExists_returnsFalse() {
        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001", BlacklistStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertFalse(blacklistService.isBlacklisted("VTR-001"));
    }

    /**
     * Verifies that a matching active proof index identifies a blacklisted visitor.
     */
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

    /**
     * Verifies that a proof without a matching active record is not blacklisted.
     */
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

    /**
     * Verifies that adding a nonexistent visitor to the blacklist throws not found.
     */
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

    /**
     * Verifies that a visitor without a document cannot be blacklisted.
     */
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

    /**
     * Verifies that an already actively blacklisted visitor cannot be added again.
     */
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

    /**
     * Verifies that a domestic proof already used by an active blacklist record
     * causes a conflict.
     */
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

    /**
     * Verifies that a domestic document without valid proof details is rejected.
     */
    @Test
    void addExistingVisitor_whenNoValidProofExists_throwsBusinessRuleException() {
        Document documentWithoutProofs = new Document();
        documentWithoutProofs.setNationality(Nationality.DOMESTIC);

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

    /**
     * Verifies that a visitor with available domestic proofs can be blacklisted.
     */
    @Test
    void addExistingVisitor_whenDomesticProofsAreAvailable_savesBlacklist() {
        when(idGeneratorService.generateId(anyString(), anyString()))
                .thenAnswer(invocation ->
                        invocation.getArgument(1) + "-001"
                );

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

        when(blacklistMapper.toResponse(
                any(Blacklist.class), isNull(), anyList()
        )).thenReturn(response);

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

    /**
     * Verifies that an international visitor's passport is added as the proof.
     */
    @Test
    void addExistingVisitor_whenInternationalPassportExists_savesPassportProof() {
        when(idGeneratorService.generateId(anyString(), anyString()))
                .thenAnswer(invocation ->
                        invocation.getArgument(1) + "-001"
                );

        Document internationalDocument = new Document();
        internationalDocument.setNationality(Nationality.INTERNATIONAL);
        internationalDocument.setPassportNumber("PASSPORT-INDEX-001");

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

        when(blacklistMapper.toResponse(
                any(Blacklist.class), isNull(), anyList()
        )).thenReturn(response);

        blacklistService.addExistingVisitorToBlacklist(
                "VTR-001", request
        );

        verify(blacklistRepository).save(argThat(blacklist ->
                blacklist.getProofs().size() == 1
                        && blacklist.getProofs().get(0).getProofType()
                        == ProofType.PASSPORT
        ));
    }

    /**
     * Verifies that removing a nonexistent blacklist record throws not found.
     */
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

    /**
     * Verifies that a blacklist record already removed cannot be removed again.
     */
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

    /**
     * Verifies that removing an active record changes its status and updater.
     */
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

        when(blacklistMapper.toResponse(
                eq(blacklist), isNull(), anyList()
        )).thenReturn(response);

        BlacklistResponse result =
                blacklistService.removeFromBlacklist(
                        "BL-001", "EMP-001"
                );

        assertSame(response, result);
        assertEquals(BlacklistStatus.REMOVED, blacklist.getStatus());
        assertEquals("EMP-001", blacklist.getUpdatedBy());

        verify(blacklistRepository).save(blacklist);
    }

    /**
     * Verifies that retrieving all blacklist records returns an empty list
     * when no records exist.
     */
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

    /**
     * Verifies that retrieving a nonexistent blacklist record throws not found.
     */
    @Test
    void getBlacklistById_whenRecordDoesNotExist_throwsNotFound() {
        when(blacklistRepository.findById("BL-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> blacklistService.getBlacklistById("BL-001")
        );
    }

    /**
     * Verifies that an existing blacklist record is mapped to its response.
     */
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

        when(blacklistMapper.toResponse(
                eq(blacklist), isNull(), anyList()
        )).thenReturn(response);

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
