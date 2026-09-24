package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.AddVisitorToBlacklistRequest;
import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.BlacklistProof;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.exception.BlacklistAlreadyExistsException;
import com.adminvisitor.exception.BlacklistAlreadyRemovedException;
import com.adminvisitor.exception.BlacklistNotFoundException;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.mapper.BlacklistMapper;
import com.adminvisitor.repository.BlacklistProofRepository;
import com.adminvisitor.repository.BlacklistRepository;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final BlacklistProofRepository blacklistProofRepository;
    private final VisitorRepository visitorRepository;
    private final BlacklistMapper blacklistMapper;
    private final IdGeneratorService idGeneratorService;
    private final DocumentRepository documentRepository;

    private final HmacBlindIndexService hmacBlindIndexService;
    private final ProofValidationService proofValidationService;

    @Transactional(readOnly = true)
    public boolean isBlacklisted(String visitorId) {

        return blacklistRepository
                .findByVisitorIdAndStatus(
                        visitorId,
                        BlacklistStatus.ACTIVE
                )
                .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isBlacklistedByProof(
            ProofType proofType,
            String proofNumber) {

        String proofBlindIndex =
                hmacBlindIndexService.generateBlindIndex(
                        proofType,
                        proofNumber
                );

        return blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        proofType,
                        proofBlindIndex,
                        BlacklistStatus.ACTIVE
                )
                .isPresent();
    }

    public BlacklistResponse addToBlacklist(
            BlacklistRequest request) {

        // 1. Validate nationality + proof type
        proofValidationService.validate(
                request.getNationality(),
                request.getProofType()
        );

        // 2. Generate blind index from normalized proof number
        String proofBlindIndex =
                hmacBlindIndexService.generateBlindIndex(
                        request.getProofType(),
                        request.getProofNumber()
                );

        // 3. Check whether this proof is already actively blacklisted
        boolean alreadyBlacklisted =
                blacklistProofRepository
                        .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                                request.getProofType(),
                                proofBlindIndex,
                                BlacklistStatus.ACTIVE
                        )
                        .isPresent();

        if (alreadyBlacklisted) {
            throw new BlacklistAlreadyExistsException(
                    "Person is already in blacklist"
            );
        }

        // 4. Find visitor
        Visitor visitor =
                visitorRepository.findById(request.getVisitorId())
                        .orElseThrow(() ->
                                new VisitorNotFoundException(
                                        "Visitor not found"
                                )
                        );

        // 5. Map request → Blacklist
        Blacklist blacklist =
                blacklistMapper.toEntity(
                        request,
                        visitor
                );

        // 6. Generate blacklist ID
        blacklist.setId(
                idGeneratorService.generateId(
                        "BLACKLIST",
                        "BL"
                )
        );

        // 7. Set active status
        blacklist.setStatus(
                BlacklistStatus.ACTIVE
        );

        // 8. Create child proof record
        BlacklistProof blacklistProof =
                new BlacklistProof();

        blacklistProof.setId(
                idGeneratorService.generateId(
                        "BLACKLIST_PROOF",
                        "BLP"
                )
        );

        blacklistProof.setBlacklist(blacklist);
        blacklistProof.setProofType(request.getProofType());
        blacklistProof.setProofBlindIndex(proofBlindIndex);

        // 9. Attach proof to blacklist
        blacklist.getProofs().add(blacklistProof);

        // 10. Save blacklist + proof
        Blacklist savedBlacklist =
                blacklistRepository.save(blacklist);

        return blacklistMapper.toResponse(
                savedBlacklist
        );
    }

    public BlacklistResponse addExistingVisitorToBlacklist(
            String visitorId,
            AddVisitorToBlacklistRequest request) {

        // 1. Find the existing visitor
        Visitor visitor =
                visitorRepository.findById(visitorId)
                        .orElseThrow(() ->
                                new VisitorNotFoundException(
                                        "Visitor not found"
                                )
                        );

        // 2. Find the latest document of the visitor
        Document document =
                documentRepository
                        .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No document found for visitor"
                                )
                        );

        // 3. Prevent duplicate active blacklist for this visitor
        boolean alreadyBlacklisted =
                blacklistRepository
                        .findByVisitorIdAndStatus(
                                visitorId,
                                BlacklistStatus.ACTIVE
                        )
                        .isPresent();

        if (alreadyBlacklisted) {
            throw new BlacklistAlreadyExistsException(
                    "Visitor is already in blacklist"
            );
        }

        // 4. Create the parent blacklist record
        Blacklist blacklist = new Blacklist();

        blacklist.setId(
                idGeneratorService.generateId(
                        "BLACKLIST",
                        "BL"
                )
        );

        blacklist.setVisitor(visitor);
        blacklist.setNationality(document.getNationality());
        blacklist.setReason(request.getReason());
        blacklist.setCreatedBy(request.getCreatedBy());
        blacklist.setStatus(BlacklistStatus.ACTIVE);

        // 5. Add Aadhaar proof if available
        if (document.getAadharNumber() != null
                && !document.getAadharNumber().isBlank()) {

            addBlacklistProof(
                    blacklist,
                    ProofType.AADHAAR,
                    document.getAadharNumber(),
                    request.getCreatedBy()
            );
        }

        // 6. Add PAN proof if available
        if (document.getPanNumber() != null
                && !document.getPanNumber().isBlank()) {

            addBlacklistProof(
                    blacklist,
                    ProofType.PAN,
                    document.getPanNumber(),
                    request.getCreatedBy()
            );
        }

        // 7. Add Passport proof if available
        if (document.getPassportNumber() != null
                && !document.getPassportNumber().isBlank()) {

            addBlacklistProof(
                    blacklist,
                    ProofType.PASSPORT,
                    document.getPassportNumber(),
                    request.getCreatedBy()
            );
        }

        // 8. Make sure at least one proof exists
        if (blacklist.getProofs().isEmpty()) {
            throw new IllegalArgumentException(
                    "No valid proof found for visitor"
            );
        }

        // 9. Save parent + child proofs
        Blacklist savedBlacklist =
                blacklistRepository.save(blacklist);

        return blacklistMapper.toResponse(savedBlacklist);
    }

    private void addBlacklistProof(
            Blacklist blacklist,
            ProofType proofType,
            String proofNumber,
            String createdBy) {

        String proofBlindIndex =
                hmacBlindIndexService.generateBlindIndex(
                        proofType,
                        proofNumber
                );

        boolean alreadyBlacklisted =
                blacklistProofRepository
                        .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                                proofType,
                                proofBlindIndex,
                                BlacklistStatus.ACTIVE
                        )
                        .isPresent();

        if (alreadyBlacklisted) {
            throw new BlacklistAlreadyExistsException(
                    "Person with " + proofType + " proof is already in blacklist"
            );
        }

        BlacklistProof blacklistProof =
                new BlacklistProof();

        blacklistProof.setId(
                idGeneratorService.generateId(
                        "BLACKLIST_PROOF",
                        "BLP"
                )
        );

        blacklistProof.setBlacklist(blacklist);
        blacklistProof.setProofType(proofType);
        blacklistProof.setProofBlindIndex(proofBlindIndex);
        blacklistProof.setCreatedBy(createdBy);

        blacklist.getProofs().add(blacklistProof);
    }

    public BlacklistResponse removeFromBlacklist(
            String blacklistId,
            String removedBy) {

        Blacklist blacklist =
                blacklistRepository.findById(blacklistId)
                        .orElseThrow(() ->
                                new BlacklistNotFoundException(
                                        "Blacklist record not found"
                                )
                        );

        if (blacklist.getStatus() == BlacklistStatus.REMOVED) {
            throw new BlacklistAlreadyRemovedException(
                    "Person is already removed from blacklist"
            );
        }

        blacklist.setStatus(BlacklistStatus.REMOVED);
        blacklist.setUpdatedBy(removedBy);

        Blacklist updatedBlacklist =
                blacklistRepository.save(blacklist);

        return blacklistMapper.toResponse(
                updatedBlacklist
        );
    }

    @Transactional(readOnly = true)
    public List<BlacklistResponse> getAllBlacklistRecords() {

        return blacklistRepository.findAll()
                .stream()
                .map(blacklistMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BlacklistResponse getBlacklistById(
            String id) {

        Blacklist blacklist =
                blacklistRepository.findById(id)
                        .orElseThrow(() ->
                                new BlacklistNotFoundException(
                                        "Blacklist record not found"
                                )
                        );

        return blacklistMapper.toResponse(blacklist);
    }
}