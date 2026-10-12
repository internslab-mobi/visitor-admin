package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.AddVisitorToBlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.BlacklistAlreadyExistsException;
import com.adminvisitor.exception.BlacklistAlreadyRemovedException;
import com.adminvisitor.exception.BlacklistNotFoundException;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.mapper.BlacklistMapper;
import com.adminvisitor.repository.*;
import com.adminvisitor.service.BlacklistService;
import com.adminvisitor.service.IdGeneratorService;
import com.adminvisitor.service.ProofSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BlacklistServiceImpl implements BlacklistService {

    private final BlacklistRepository blacklistRepository;
    private final BlacklistProofRepository blacklistProofRepository;
    private final VisitorRepository visitorRepository;
    private final BlacklistMapper blacklistMapper;
    private final IdGeneratorService idGeneratorService;
    private final DocumentRepository documentRepository;

    private final ProofSecurityService proofSecurityService;
    private final VisitRepository visitRepository;

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
                proofSecurityService.generateBlindIndex(
                        proofType,
                        proofNumber
                );

        return blacklistProofRepository
                .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                        proofType,
                        proofBlindIndex,
                        BlacklistStatus.ACTIVE
                ).isPresent();
    }

    public BlacklistResponse addExistingVisitorToBlacklist(String visitorId, AddVisitorToBlacklistRequest request) {

        // 1. Find the existing visitor
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->new VisitorNotFoundException("Visitor not found"));

        // 2. Find the latest document of the visitor
        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->new IllegalArgumentException("No document found for visitor"));

        // 3. Prevent duplicate active blacklist for this visitor
        boolean alreadyBlacklisted = blacklistRepository.findByVisitorIdAndStatus(visitorId, BlacklistStatus.ACTIVE).isPresent();

        if (alreadyBlacklisted) {
            throw new BlacklistAlreadyExistsException("Visitor is already in blacklist");
        }

        // 4. Create the parent blacklist record
        Blacklist blacklist = Blacklist.builder()
                .id(idGeneratorService.generateId("BLACKLIST", "BL"))
                .visitor(visitor)
                .nationality(document.getNationality())
                .reason(request.getReason())
                .createdBy(request.getCreatedBy())
                .status(BlacklistStatus.ACTIVE)
                .build();

        // 5. Add proofs based on nationality
        switch (document.getNationality()) {
            case DOMESTIC -> {
                if (document.getAadharNumber() != null
                        && !document.getAadharNumber().isBlank()) {

                    addBlacklistProof(
                            blacklist,
                            ProofType.AADHAAR,
                            document.getAadharNumber(),
                            request.getCreatedBy());
                }

                if (document.getPanNumber() != null
                        && !document.getPanNumber().isBlank()) {

                    addBlacklistProof(
                            blacklist,
                            ProofType.PAN,
                            document.getPanNumber(),
                            request.getCreatedBy());
                }
            }

            case INTERNATIONAL -> {
                if (document.getPassportNumber() != null
                        && !document.getPassportNumber().isBlank()) {

                    addBlacklistProof(
                            blacklist,
                            ProofType.PASSPORT,
                            document.getPassportNumber(),
                            request.getCreatedBy());
                }
            }
        }

        // 6. Required proof must exist
        if (blacklist.getProofs().isEmpty()) {
            throw new IllegalArgumentException( "No valid identity proof found for visitor");
        }

        // 7. Save parent + child proofs
        Blacklist savedBlacklist = blacklistRepository.save(blacklist);
        return toResponse(savedBlacklist);
    }


    private void addBlacklistProof(Blacklist blacklist, ProofType proofType, String proofBlindIndex, String createdBy) {

        boolean alreadyBlacklisted =
                blacklistProofRepository
                        .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                                proofType,
                                proofBlindIndex,
                                BlacklistStatus.ACTIVE
                        ).isPresent();

        if (alreadyBlacklisted) {
            throw new BlacklistAlreadyExistsException(
                    "Person with " + proofType + " proof is already in blacklist");
        }

        BlacklistProof proof = BlacklistProof.builder()
                .id(idGeneratorService.generateId("BLACKLIST_PROOF", "BLP"))
                .blacklist(blacklist)
                .proofType(proofType)
                .proofBlindIndex(proofBlindIndex)
                .createdBy(createdBy)
                .build();

        blacklist.getProofs().add(proof);
    }

    public BlacklistResponse removeFromBlacklist(String blacklistId, String removedBy) {

        Blacklist blacklist = blacklistRepository.findById(blacklistId)
                .orElseThrow(() ->new BlacklistNotFoundException("Blacklist record not found"));

        if (blacklist.getStatus() == BlacklistStatus.REMOVED) {
            throw new BlacklistAlreadyRemovedException("Person is already removed from blacklist");
        }

        blacklist.setStatus(BlacklistStatus.REMOVED);
        blacklist.setUpdatedBy(removedBy);
        Blacklist updatedBlacklist = blacklistRepository.save(blacklist);
        return toResponse(updatedBlacklist);
    }

    @Transactional(readOnly = true)
    public List<BlacklistResponse> getAllBlacklistRecords() {

        List<Blacklist> blacklists = blacklistRepository.findAll();

        if (blacklists.isEmpty()) {
            return List.of();
        }

        List<String> visitorIds = blacklists.stream()
                .map(blacklist -> blacklist.getVisitor().getId())
                .distinct()
                .toList();

        List<String> blacklistIds = blacklists.stream()
                .map(Blacklist::getId)
                .toList();

        Map<String, VisitorType> visitorTypesByVisitorId = new HashMap<>();

        visitRepository.findLatestVisitsByVisitorIds(visitorIds)
                .forEach(row ->
                        visitorTypesByVisitorId.putIfAbsent(
                                (String) row[0],
                                (VisitorType) row[1]
                        )
                );

        Map<String, List<ProofType>> proofTypesByBlacklistId =
                blacklistProofRepository.findByBlacklist_IdIn(blacklistIds)
                        .stream()
                        .collect(Collectors.groupingBy(
                                proof -> proof.getBlacklist().getId(),
                                Collectors.mapping(
                                        BlacklistProof::getProofType,
                                        Collectors.toList()
                                )
                        ));

        return blacklists.stream()
                .map(blacklist -> blacklistMapper.toResponse(
                        blacklist,
                        visitorTypesByVisitorId.get(
                                blacklist.getVisitor().getId()
                        ),
                        proofTypesByBlacklistId.getOrDefault(
                                blacklist.getId(),
                                List.of()
                        )
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public BlacklistResponse getBlacklistById(String id) {
        Blacklist blacklist = blacklistRepository.findById(id)
                .orElseThrow(() ->new BlacklistNotFoundException("Blacklist record not found"));

        return toResponse(blacklist);
    }

    private BlacklistResponse toResponse(Blacklist blacklist) {
        List<ProofType> matchingProofTypes =
                blacklistProofRepository.findByBlacklistId(blacklist.getId())
                        .stream()
                        .map(BlacklistProof::getProofType)
                        .toList();

        VisitorType visitorType =
                visitRepository.findTopByVisitorIdOrderByCreatedAtDesc(
                                blacklist.getVisitor().getId()
                        )
                        .map(Visit::getVisitorType)
                        .orElse(null);

        return blacklistMapper.toResponse(
                blacklist,
                visitorType,
                matchingProofTypes
        );
    }
}