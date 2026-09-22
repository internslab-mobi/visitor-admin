package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.exception.BlacklistAlreadyExistsException;
import com.adminvisitor.exception.BlacklistAlreadyRemovedException;
import com.adminvisitor.exception.BlacklistNotFoundException;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.mapper.BlacklistMapper;
import com.adminvisitor.repository.BlacklistRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import com.adminvisitor.enums.ProofType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final VisitorRepository visitorRepository;
    private final BlacklistMapper blacklistMapper;
    private final IdGeneratorService idGeneratorService;

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

        return blacklistRepository
                .findByProofTypeAndProofBlindIndexAndStatus(
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
                blacklistRepository
                        .findByProofTypeAndProofBlindIndexAndStatus(
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

        // 5. Map request → entity
        Blacklist blacklist =
                blacklistMapper.toEntity(
                        request,
                        visitor
                );

        // 6. Store blind index
        blacklist.setProofBlindIndex(proofBlindIndex);

        // 7. Generate blacklist ID
        blacklist.setId(
                idGeneratorService.generateId(
                        "BLACKLIST",
                        "BL"
                )
        );

        // 8. Set active status
        blacklist.setStatus(
                BlacklistStatus.ACTIVE
        );

        // 9. Save
        Blacklist savedBlacklist =
                blacklistRepository.save(blacklist);

        return blacklistMapper.toResponse(
                savedBlacklist
        );
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

        return blacklistMapper.toResponse(updatedBlacklist);
    }

    @Transactional(readOnly = true)
    public List<BlacklistResponse> getAllBlacklistRecords() {

        return blacklistRepository.findAll()
                .stream()
                .map(blacklistMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BlacklistResponse getBlacklistById(String id) {

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