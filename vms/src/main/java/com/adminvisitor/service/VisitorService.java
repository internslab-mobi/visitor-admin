package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.UpdateVisitorRequest;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.dto.responsedto.VisitorResponse;
import com.adminvisitor.entity.*;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.EmailAlreadyExistsException;
import com.adminvisitor.exception.MobileNumberAlreadyExistsException;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VisitorService {

    private final VisitorRepository visitorRepository;
    private final VendorRepository vendorRepository;
    private final DocumentRepository documentRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final BlacklistRepository blacklistRepository;
    private final VisitRepository visitRepository;

    @Transactional(readOnly = true)
    public VisitorResponse getVisitor(String visitorId) {

        log.info("Fetching visitor. visitorId={}", visitorId);

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new VisitorNotFoundException(
                                "Visitor not found with id: " + visitorId));

        return toVisitorResponse(visitor);
    }

    @Transactional(readOnly = true)
    public List<VisitorResponse> getAllVisitors() {

        log.info("Fetching all visitors");

        return visitorRepository.findAll()
                .stream()
                .map(this::toVisitorResponse)
                .toList();
    }

    @Transactional
    public VisitorResponse updateVisitor(
            String visitorId,
            UpdateVisitorRequest request) {

        log.info("Updating visitor. visitorId={}", visitorId);

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new VisitorNotFoundException(
                                "Visitor not found with id: " + visitorId));

        // Check whether email belongs to another visitor
        visitorRepository.findByEmail(request.email())
                .ifPresent(existingVisitor -> {

                    if (!existingVisitor.getId().equals(visitorId)) {
                        throw new EmailAlreadyExistsException(
                                "This email is already associated with another visitor");
                    }
                });

        // Check whether mobile number belongs to another visitor
        visitorRepository.findByMobileNumber(request.mobileNumber())
                .ifPresent(existingVisitor -> {

                    if (!existingVisitor.getId().equals(visitorId)) {
                        throw new MobileNumberAlreadyExistsException(
                                "This mobile number is already associated with another visitor");
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
                    "Vendor details synchronized. visitorId={}, vendorId={}",
                    visitorId,
                    vendor.getId()
            );
        }
        log.info(
                "Visitor updated successfully. visitorId={}",
                updatedVisitor.getId()
        );

        return toVisitorResponse(updatedVisitor);
    }

    private VisitorResponse toVisitorResponse(Visitor visitor) {

        Visit latestVisit =
                visitRepository
                        .findTopByVisitorIdOrderByCreatedAtDesc(
                                visitor.getId()
                        )
                        .orElse(null);

        return new VisitorResponse(

                visitor.getId(),

                visitor.getFirstName(),

                visitor.getLastName(),

                visitor.getEmail(),

                visitor.getMobileNumber(),

                visitor.getCompanyName(),

                latestVisit != null
                        ? latestVisit.getVisitorType().name()
                        : null,

                latestVisit != null
                        ? latestVisit.getExpectedArrivalAt()
                        : null
        );
    }

    @Transactional(readOnly = true)
    public VisitorEditResponse getVisitorEditDetails(
            String visitorId
    ) {

        Visitor visitor =
                visitorRepository.findById(visitorId)
                        .orElseThrow(() ->
                                new VisitorNotFoundException(
                                        "Visitor not found with id: "
                                                + visitorId
                                )
                        );


        // ==========================================
        // VISITOR TYPE + NATIONALITY
        // ==========================================

        Visit latestVisit =
                visitRepository
                        .findTopByVisitorIdOrderByCreatedAtDesc(
                                visitorId
                        )
                        .orElse(null);

        VisitorType visitorType =
                latestVisit != null
                        ? latestVisit.getVisitorType()
                        : null;


        Nationality nationality =
                documentRepository
                        .findTopByVisitorIdOrderByCreatedAtDesc(
                                visitorId
                        )
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


        // ==========================================
        // VENDOR
        // ==========================================

        VisitorEditResponse.VendorInfo vendorInfo = null;

        Optional<Vendor> vendor =
                vendorRepository.findByVisitorId(visitorId);

        if (vendor.isPresent()) {

            vendorInfo =
                    new VisitorEditResponse.VendorInfo(
                            vendor.get().getId()
                    );
        }


        // ==========================================
        // DOCUMENTS
        // ==========================================

        List<VisitorEditResponse.DocumentInfo> documents =

                documentRepository
                        .findByVisitorId(visitorId)
                        .stream()

                        .flatMap(document ->

                                documentMetadataRepository
                                        .findByDocumentId(
                                                document.getId()
                                        )
                                        .stream()
                        )

                        .filter(metadata ->
                                metadata.getDocumentType() != null
                                        &&
                                        metadata.getDocumentType()
                                                != DocumentType.NDA
                                        &&
                                        metadata.getDocumentType()
                                                != DocumentType.VISITOR_PHOTO
                        )

                        .map(metadata ->
                                new VisitorEditResponse.DocumentInfo(

                                        metadata.getDocument().getId(),

                                        metadata.getDocumentType(),

                                        metadata.getCreatedAt() != null
                                                ? metadata.getCreatedAt().toString()
                                                : null
                                )
                        )

                        .toList();


        // ==========================================
        // VISIT HISTORY
        // ==========================================

        List<VisitorEditResponse.VisitInfo> visits =

                visitRepository
                        .findByVisitorIdOrderByExpectedArrivalAtDesc(
                                visitorId
                        )

                        .stream()

                        .map(visit -> {

                            Employee host =
                                    visit.getHost();

                            String hostName =
                                    host.getFirstName()
                                            + " "
                                            + host.getLastName();

                            String departmentName =
                                    host.getDepartment()
                                            .getDepartmentName();

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

                                    visit.getRemarks(),

                                    visit.getStatus()
                            );

                        })

                        .toList();


        // ==========================================
        // CURRENT BLACKLIST
        // ==========================================

        VisitorEditResponse.BlacklistInfo blacklistInfo = null;

        Optional<Blacklist> activeBlacklist =
                blacklistRepository.findByVisitorIdAndStatus(
                        visitorId,
                        BlacklistStatus.ACTIVE
                );

        if (activeBlacklist.isPresent()) {

            Blacklist blacklist =
                    activeBlacklist.get();

            blacklistInfo =
                    new VisitorEditResponse.BlacklistInfo(

                            blacklist.getId(),

                            blacklist.getReason(),

                            blacklist.getStatus(),

                            blacklist.getCreatedAt()
                    );
        }


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