package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.*;
import com.adminvisitor.mapper.VisitorMapper;
import com.adminvisitor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation for managing visitor-related operations.
 *
 * <p>Provides functionality to retrieve visitor details, list all visitors,
 * update visitor information, and retrieve the details required for editing
 * a visitor profile.</p>
 *
 * <p>Coordinates with visitor, vendor, document, blacklist, and visit
 * repositories to assemble visitor information and maintain synchronized
 * details between visitors and their associated vendor records.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorServiceImpl implements com.adminvisitor.service.VisitorService {

    private final VisitorRepository visitorRepository;
    private final VendorRepository vendorRepository;
    private final DocumentRepository documentRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final BlacklistRepository blacklistRepository;
    private final VisitRepository visitRepository;
    private final VisitManagementServiceImpl visitManagementService;
    private final VisitorMapper visitorMapper;
    /**
     * Retrieves a visitor by their unique identifier.
     *
     * @param visitorId the unique identifier of the visitor
     * @return the visitor response containing visitor details and latest visit information
     * @throws ResourceNotFoundException if no visitor exists with the given ID
     */
    
    public VisitorResponse getVisitor(String visitorId) {

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found with id: " + visitorId
                        )
                );

        return toVisitorResponse(visitor);
    }

    /**
     * Retrieves all visitors along with their latest visit information.
     *
     * <p>Fetches the visitors and retrieves their latest visit details in a
     * batch to avoid querying the latest visit separately for each visitor.</p>
     *
     * @return a list of visitor responses, or an empty list if no visitors exist
     */
    
    public List<VisitorResponse> getAllVisitors() {

        List<Visitor> visitors = visitorRepository.findAll();

        if (visitors.isEmpty()) {
            log.debug("VISITOR_LIST_RETRIEVED count=0");
            return List.of();
        }

        List<String> visitorIds = visitors.stream()
                .map(Visitor::getId)
                .toList();

        Map<String, Object[]> latestVisits = visitRepository
                .findLatestVisitsByVisitorIds(visitorIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> row,
                        (first, second) -> first
                ));

        List<VisitorResponse> responses = visitors.stream()
                .map(visitor -> {
                    Object[] latest = latestVisits.get(visitor.getId());

                    return new VisitorResponse(
                            visitor.getId(),
                            visitor.getFirstName(),
                            visitor.getLastName(),
                            visitor.getEmail(),
                            visitor.getMobileNumber(),
                            visitor.getCompanyName(),
                            latest != null && latest[1] != null
                                    ? latest[1].toString()
                                    : null,
                            latest != null
                                    ? (LocalDateTime) latest[2]
                                    : null
                    );
                })
                .toList();

        log.debug("VISITOR_LIST_RETRIEVED count={}", responses.size());

        return responses;
    }

    /**
     * Updates an existing visitor's information and synchronizes the
     * corresponding vendor details when the visitor is associated with a vendor.
     *
     * <p>Validates that the requested email address and mobile number are not
     * already assigned to another visitor before saving the changes.</p>
     *
     * @param visitorId the unique identifier of the visitor to update
     * @param request the request containing the updated visitor information
     * @return the updated visitor response
     * @throws ResourceNotFoundException if no visitor exists with the given ID
     * @throws ResourceAlreadyExistsException if the email or mobile number
     *         belongs to another visitor
     */

    @Transactional
    public VisitorResponse updateVisitor(
            String visitorId,
            UpdateVisitorRequest request
    ) {

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found with id: " + visitorId
                        )
                );

        // Check whether email belongs to another visitor.
        visitorRepository.findByEmail(request.email())
                .ifPresent(existingVisitor -> {
                    if (!existingVisitor.getId().equals(visitorId)) {
                        throw new ResourceAlreadyExistsException(
                                "This email is already associated with another visitor"
                        );
                    }
                });

        // Check whether mobile number belongs to another visitor.
        visitorRepository.findByMobileNumber(request.mobileNumber())
                .ifPresent(existingVisitor -> {
                    if (!existingVisitor.getId().equals(visitorId)) {
                        throw new ResourceAlreadyExistsException(
                                "This mobile number is already associated with another visitor"
                        );
                    }
                });

        visitor.setFirstName(request.firstName());
        visitor.setLastName(request.lastName());
        visitor.setEmail(request.email());
        visitor.setMobileNumber(request.mobileNumber());
        visitor.setCompanyName(request.companyName());
        // visitor.setValidity(request.validity());

        Visitor updatedVisitor = visitorRepository.save(visitor);

        Optional<Vendor> vendorOptional =
                vendorRepository.findByVisitorId(visitorId);

        if (vendorOptional.isPresent()) {

            Vendor vendor = vendorOptional.get();

            vendor.setFirstName(request.firstName());
            vendor.setLastName(request.lastName());
            vendor.setEmail(request.email());
            vendor.setMobileNumber(request.mobileNumber());
            vendor.setCompanyName(request.companyName());

            vendorRepository.save(vendor);

            log.info(
                    "VENDOR_DETAILS_SYNCHRONIZED visitorId={} vendorId={}",
                    visitorId,
                    vendor.getId()
            );
        }

        log.info(
                "VISITOR_UPDATED visitorId={}",
                updatedVisitor.getId()
        );

        return toVisitorResponse(updatedVisitor);
    }

    /**
     * Maps a visitor entity to a response DTO and includes the latest visit
     * type and expected arrival time when visit information is available.
     *
     * @param visitor the visitor entity to map
     * @return the visitor response containing visitor and latest visit information
     */
//    private VisitorResponse toVisitorResponse(Visitor visitor) {
//
//        Visit latestVisit = visitRepository
//                .findTopByVisitorIdOrderByCreatedAtDesc(visitor.getId())
//                .orElse(null);
//
//        return new VisitorResponse(
//                visitor.getId(),
//                visitor.getFirstName(),
//                visitor.getLastName(),
//                visitor.getEmail(),
//                visitor.getMobileNumber(),
//                visitor.getCompanyName(),
//                latestVisit != null
//                        ? latestVisit.getVisitorType().name()
//                        : null,
//                latestVisit != null
//                        ? latestVisit.getExpectedArrivalAt()
//                        : null
//        );
//    }


    private VisitorResponse toVisitorResponse(Visitor visitor) {

        Visit latestVisit = visitRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitor.getId())
                .orElse(null);

        return visitorMapper.toResponse(visitor, latestVisit);
    }

    /**
     * Retrieves the details required to edit a visitor profile.
     *
     * <p>Collects visitor information, the latest visit type, nationality,
     * associated vendor information, eligible document metadata, visit history,
     * and current blacklist information.</p>
     *
     * @param visitorId the unique identifier of the visitor
     * @return the visitor edit response containing visitor, vendor, document,
     *         visit history, and blacklist details
     * @throws ResourceNotFoundException if no visitor exists with the given ID
     */
    
    public VisitorEditResponse getVisitorEditDetails(String visitorId) {

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found with id: " + visitorId
                        )
                );

        Visit latestVisit = visitRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElse(null);

        VisitorType visitorType = latestVisit != null
                ? latestVisit.getVisitorType()
                : null;

        Nationality nationality = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .map(Document::getNationality)
                .orElse(null);

        VisitorEditResponse.VisitorInfo visitorInfo =
                new VisitorEditResponse.VisitorInfo(
                        visitor.getId(),
                        visitor.getFirstName(),
                        visitor.getLastName(),
                        visitor.getEmail(),
                        visitor.getMobileNumber(),
                        visitor.getCompanyName(),
                        visitorType,
                        nationality
                );

        VisitorEditResponse.VendorInfo vendorInfo = null;

        Optional<Vendor> vendor =
                vendorRepository.findByVisitorId(visitorId);

        if (vendor.isPresent()) {
            vendorInfo = new VisitorEditResponse.VendorInfo(
                    vendor.get().getId()
            );
        }

        List<Document> visitorDocuments =
                documentRepository.findByVisitorId(visitorId);

        List<String> documentIds = visitorDocuments.stream()
                .map(Document::getId)
                .toList();

        List<VisitorEditResponse.DocumentInfo> documents =
                documentIds.isEmpty()
                        ? List.of()
                        : documentMetadataRepository
                          .findByDocumentIdIn(documentIds)
                          .stream()
                          .filter(metadata ->
                                  metadata.getDocumentType() == null
                                  || (metadata.getDocumentType() != DocumentType.NDA
                                      && metadata.getDocumentType() != DocumentType.VISITOR_PHOTO)
                          )
                          .map(metadata ->
                               new VisitorEditResponse.DocumentInfo(
                                       metadata.getId(),
                                       metadata.getDocumentType(),
                                       metadata.getCreatedAt() != null
                                       ? metadata.getCreatedAt().toString()
                                       : null
                               )
                          )
                          .toList();

        // Visit history.
        List<VisitorEditResponse.VisitInfo> visits =
                visitRepository
                        .findByVisitorIdOrderByExpectedArrivalAtDesc(visitorId)
                        .stream()
                        .map(visit -> {

                            Employee host = visit.getHost();

                            String hostName = host.getFirstName()
                                    + " "
                                    + host.getLastName();

                            String departmentName =
                                    host.getDepartment().getDepartmentName();

                            return new VisitorEditResponse.VisitInfo(
                                    visit.getId(),
                                    visit.getVisitReference(),
                                    visit.getVisitorType(),
                                    visit.getRegistrationType(),
                                    visit.getPurpose(),
                                    host.getId(),
                                    hostName,
                                    host.getDepartment().getId(),
                                    departmentName,
                                    visit.getExpectedArrivalAt(),
                                    visit.getExpectedDepartureAt(),
                                    visit.getCheckedInAt(),
                                    visit.getCheckedOutAt(),
                                    visitManagementService.calculateDuration(visit),
                                    visit.getRemarks(),
                                    visit.getStatus()
                            );
                        })
                        .toList();

        // Current blacklist.
        VisitorEditResponse.BlacklistInfo blacklistInfo = null;

        Optional<Blacklist> activeBlacklist =
                blacklistRepository.findByVisitorIdAndStatus(
                        visitorId,
                        BlacklistStatus.ACTIVE
                );

        if (activeBlacklist.isPresent()) {

            Blacklist blacklist = activeBlacklist.get();

            blacklistInfo = new VisitorEditResponse.BlacklistInfo(
                    blacklist.getId(),
                    blacklist.getReason(),
                    blacklist.getStatus(),
                    blacklist.getCreatedAt()
            );
        }

        log.debug(
                "VISITOR_EDIT_DETAILS_RETRIEVED visitorId={} documentCount={} visitCount={} blacklisted={}",
                visitorId,
                documents.size(),
                visits.size(),
                activeBlacklist.isPresent()
        );

        return new VisitorEditResponse(
                visitorInfo,
                vendorInfo,
                documents,
                visits,
                blacklistInfo,
                activeBlacklist.isPresent()
        );
    }
}