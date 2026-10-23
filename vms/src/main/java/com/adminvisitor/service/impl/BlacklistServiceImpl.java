package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.BlacklistRequest;
import com.adminvisitor.dto.responsedto.BlacklistResponse;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.*;
import com.adminvisitor.mapper.BlacklistMapper;
import com.adminvisitor.repository.*;
import com.adminvisitor.service.BlacklistService;
import com.adminvisitor.service.IdGeneratorService;
import com.adminvisitor.service.ProofSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implements visitor blacklist operations, including adding visitors to the blacklist,
 * checking identity proofs, removing blacklist records, and retrieving blacklist details.
 *
 * @author VMS Development Team
 */
@Service
@Slf4j
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

    /**
     * Checks whether a visitor has an active blacklist record.
     *
     * @param visitorId the unique identifier of the visitor
     * @return {@code true} if the visitor is actively blacklisted; otherwise, {@code false}
     */
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String visitorId) {

        return blacklistRepository
                .findByVisitorIdAndStatus(
                        visitorId,
                        BlacklistStatus.ACTIVE
                )
                .isPresent();
    }

    /**
     * Checks whether an identity proof matches an active blacklist proof.
     * The supplied proof number is converted to a blind index before lookup.
     *
     * @param proofType the type of identity proof
     * @param proofNumber the identity proof number to check
     * @return {@code true} if an active blacklist record matches the proof; otherwise, {@code false}
     */
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
                )
                .isPresent();
    }

    /**
     * Adds an existing visitor to the blacklist using the visitor's stored identity document.
     * Prevents duplicate active blacklist records and creates blacklist proof entries
     * according to the visitor's nationality.
     *
     * @param visitorId the unique identifier of the visitor to blacklist
     * @param request the blacklist reason and creator details
     * @return the response containing the saved blacklist record and matching proof types
     * @throws ResourceNotFoundException if the visitor or an identity document cannot be found
     * @throws ResourceAlreadyExistsException if the visitor or an identity proof is already actively blacklisted
     * @throws BusinessRuleException if no valid identity proof is available
     */
    public BlacklistResponse addExistingVisitorToBlacklist(
            String visitorId,
            BlacklistRequest request) {

        // 1. Find the existing visitor
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Visitor not found"));

        // 2. Find the latest document of the visitor
        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No identity document found for visitor"
                        ));

        // 3. Prevent duplicate active blacklist for this visitor
        boolean alreadyBlacklisted =
                blacklistRepository
                        .findByVisitorIdAndStatus(
                                visitorId,
                                BlacklistStatus.ACTIVE
                        )
                        .isPresent();

        if (alreadyBlacklisted) {
            throw new ResourceAlreadyExistsException(
                    "Visitor is already in blacklist"
            );
        }

        // 4. Create the parent blacklist record
        Blacklist blacklist = Blacklist.builder()
                .id(idGeneratorService.generateId("BLACKLIST", "BL"))
                .visitor(visitor)
                .nationality(document.getNationality())
                .reason(request.reason())
                .createdBy(request.createdBy())
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
                            request.createdBy()
                    );
                }

                if (document.getPanNumber() != null
                        && !document.getPanNumber().isBlank()) {

                    addBlacklistProof(
                            blacklist,
                            ProofType.PAN,
                            document.getPanNumber(),
                            request.createdBy()
                    );
                }
            }

            case INTERNATIONAL -> {
                if (document.getPassportNumber() != null
                        && !document.getPassportNumber().isBlank()) {

                    addBlacklistProof(
                            blacklist,
                            ProofType.PASSPORT,
                            document.getPassportNumber(),
                            request.createdBy()
                    );
                }
            }
        }

        // 6. Required proof must exist
        if (blacklist.getProofs().isEmpty()) {
            throw new BusinessRuleException(
                    "No valid identity proof found for visitor"
            );
        }

        // 7. Save parent + child proofs
        Blacklist savedBlacklist = blacklistRepository.save(blacklist);

        log.info(
                "BLACKLIST_RECORD_CREATED blacklistId={} visitorId={} proofCount={}",
                savedBlacklist.getId(),
                visitor.getId(),
                savedBlacklist.getProofs().size()
        );

        return toResponse(savedBlacklist);
    }

    /**
     * Adds an identity proof to a blacklist record after checking for an existing
     * active blacklist entry for the same proof.
     *
     * @param blacklist the parent blacklist record
     * @param proofType the type of identity proof
     * @param proofBlindIndex the stored blind-index value for the proof
     * @param createdBy the identifier of the person creating the blacklist proof
     * @throws ResourceAlreadyExistsException if the identity proof is already actively blacklisted
     */
    private void addBlacklistProof(
            Blacklist blacklist,
            ProofType proofType,
            String proofBlindIndex,
            String createdBy) {

        boolean alreadyBlacklisted =
                blacklistProofRepository
                        .findByProofTypeAndProofBlindIndexAndBlacklistStatus(
                                proofType,
                                proofBlindIndex,
                                BlacklistStatus.ACTIVE
                        )
                        .isPresent();

        if (alreadyBlacklisted) {
            throw new ResourceAlreadyExistsException(
                    "Person with " + proofType
                            + " proof is already in blacklist"
            );
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

    /**
     * Removes a blacklist record by changing its status to REMOVED.
     * The record is retained rather than deleted.
     *
     * @param blacklistId the unique identifier of the blacklist record
     * @param removedBy the identifier of the person removing the record
     * @return the response containing the updated blacklist record
     * @throws ResourceNotFoundException if the blacklist record does not exist
     * @throws BlacklistAlreadyRemovedException if the record has already been removed
     */
    public BlacklistResponse removeFromBlacklist(
            String blacklistId,
            String removedBy) {

        Blacklist blacklist = blacklistRepository.findById(blacklistId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Blacklist record not found"
                        ));

        if (blacklist.getStatus() == BlacklistStatus.REMOVED) {
            throw new BlacklistAlreadyRemovedException(
                    "Person is already removed from blacklist"
            );
        }

        blacklist.setStatus(BlacklistStatus.REMOVED);
        blacklist.setUpdatedBy(removedBy);

        Blacklist updatedBlacklist = blacklistRepository.save(blacklist);

        log.info(
                "BLACKLIST_RECORD_REMOVED blacklistId={} removedBy={}",
                updatedBlacklist.getId(),
                removedBy
        );

        return toResponse(updatedBlacklist);
    }

    /**
     * Retrieves all blacklist records and maps them to response objects.
     * Loads visitor types and proof types in batches to avoid querying them separately
     * for every blacklist record.
     *
     * @return a list of blacklist responses, or an empty list if no records exist
     */
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

    /**
     * Retrieves a blacklist record by its unique identifier.
     *
     * @param id the unique identifier of the blacklist record
     * @return the response containing the blacklist record and related details
     * @throws ResourceNotFoundException if the blacklist record does not exist
     */
    @Transactional(readOnly = true)
    public BlacklistResponse getBlacklistById(String id) {

        Blacklist blacklist = blacklistRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Blacklist record not found"
                        ));

        return toResponse(blacklist);
    }

    /**
     * Converts a blacklist entity into its response representation, including
     * the visitor type from the latest visit and the associated proof types.
     *
     * @param blacklist the blacklist entity to convert
     * @return the mapped blacklist response
     */
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