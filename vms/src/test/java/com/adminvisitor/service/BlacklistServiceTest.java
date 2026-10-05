package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.AddVisitorToBlacklistRequest;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.BlacklistProof;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.exception.BlacklistAlreadyExistsException;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.repository.BlacklistProofRepository;
import com.adminvisitor.repository.BlacklistRepository;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitorRepository;
import com.adminvisitor.mapper.BlacklistMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlacklistServiceTest {

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
    private HmacBlindIndexService hmacBlindIndexService;

    @Mock
    private ProofValidationService proofValidationService;

    @InjectMocks
    private BlacklistService blacklistService;

    @Test
    void isBlacklisted_WhenActiveRecordExists_ShouldReturnTrue() {

        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001",
                BlacklistStatus.ACTIVE
        )).thenReturn(Optional.of(new com.adminvisitor.entity.Blacklist()));

        boolean result = blacklistService.isBlacklisted("VTR-001");

        assertTrue(result);
    }

    @Test
    void isBlacklisted_WhenNoActiveRecordExists_ShouldReturnFalse() {

        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-002",
                BlacklistStatus.ACTIVE
        )).thenReturn(Optional.empty());

        boolean result = blacklistService.isBlacklisted("VTR-002");

        assertFalse(result);
    }

    @Test
    void isBlacklistedByProof_WhenActiveProofExists_ShouldReturnTrue() {

        when(hmacBlindIndexService.generateBlindIndex(
                ProofType.AADHAAR,
                "123456789012"
        )).thenReturn("mocked-blind-index");

        when(blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        ProofType.AADHAAR,
                        "mocked-blind-index",
                        BlacklistStatus.ACTIVE
                ))
                .thenReturn(Optional.of(new BlacklistProof()));

        boolean result = blacklistService.isBlacklistedByProof(
                ProofType.AADHAAR,
                "123456789012"
        );

        assertTrue(result);
    }

    @Test
    void isBlacklistedByProof_WhenNoActiveProofExists_ShouldReturnFalse() {

        when(hmacBlindIndexService.generateBlindIndex(
                ProofType.AADHAAR,
                "999999999999"
        )).thenReturn("mocked-blind-index");

        when(blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        ProofType.AADHAAR,
                        "mocked-blind-index",
                        BlacklistStatus.ACTIVE
                ))
                .thenReturn(Optional.empty());

        boolean result = blacklistService.isBlacklistedByProof(
                ProofType.AADHAAR,
                "999999999999"
        );

        assertFalse(result);
    }

    @Test
    void addExistingVisitorToBlacklist_WhenVisitorNotFound_ShouldThrowException() {

        AddVisitorToBlacklistRequest request =
                new AddVisitorToBlacklistRequest();

        request.setReason("Security violation");
        request.setCreatedBy("EMP-001");

        when(visitorRepository.findById("VTR-999"))
                .thenReturn(Optional.empty());

        assertThrows(
                VisitorNotFoundException.class,
                () -> blacklistService.addExistingVisitorToBlacklist(
                        "VTR-999",
                        request
                )
        );
    }

    @Test
    void addExistingVisitorToBlacklist_WhenDocumentNotFound_ShouldThrowException() {

        AddVisitorToBlacklistRequest request =
                new AddVisitorToBlacklistRequest();

        request.setReason("Security violation");
        request.setCreatedBy("EMP-001");

        Visitor visitor = new Visitor();

        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> blacklistService.addExistingVisitorToBlacklist(
                        "VTR-001",
                        request
                )
        );
    }

    @Test
    void addExistingVisitorToBlacklist_WhenAlreadyBlacklisted_ShouldThrowException() {

        AddVisitorToBlacklistRequest request =
                new AddVisitorToBlacklistRequest();

        request.setReason("Security violation");
        request.setCreatedBy("EMP-001");

        Visitor visitor = new Visitor();

        Document document = new Document();
        document.setNationality(Nationality.DOMESTIC);

        when(visitorRepository.findById("VTR-001"))
                .thenReturn(Optional.of(visitor));

        when(documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc("VTR-001"))
                .thenReturn(Optional.of(document));

        when(blacklistRepository.findByVisitorIdAndStatus(
                "VTR-001",
                BlacklistStatus.ACTIVE
        )).thenReturn(Optional.of(new Blacklist()));

        assertThrows(
                BlacklistAlreadyExistsException.class,
                () -> blacklistService.addExistingVisitorToBlacklist(
                        "VTR-001",
                        request
                )
        );
    }
}