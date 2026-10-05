package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.UpdateVendorRequest;
import com.adminvisitor.dto.responsedto.VendorEditResponse;
import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Vendor;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.repository.DocumentMetadataRepository;
import com.adminvisitor.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;
private final VisitorService visitorService;

private final DocumentMetadataRepository documentMetadataRepository;
    public List<VendorResponse> getAllVendors() {

        return vendorRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    public VendorResponse getVendorById(String id) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new VisitorNotFoundException("Vendor not found with id: " + id));

        return mapToResponse(vendor);
    }

    private VendorResponse mapToResponse(Vendor vendor) {

        return new VendorResponse(
                vendor.getId(),
                vendor.getVisitor().getId(),
                vendor.getFirstName(),
                vendor.getLastName(),
                vendor.getEmail(),
                vendor.getMobileNumber(),
                vendor.getCompanyName()
               // vendor.getValidity()
        );
    }

    @Transactional(readOnly = true)
    public VendorEditResponse getVendorEditDetails(String vendorId) {

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found")
                );

        Visitor visitor = vendor.getVisitor();

        // Reuse the existing visitor edit details
        VisitorEditResponse visitorEdit =
                visitorService.getVisitorEditDetails(visitor.getId());

        VisitorEditResponse.VisitorInfo visitorInfo =
                visitorEdit.visitor();

        VendorEditResponse.VendorInfo vendorInfo =
                new VendorEditResponse.VendorInfo(
                        vendor.getId(),
                        visitor.getId()
                        //vendor.getValidity()
                );

        List<VendorEditResponse.DocumentInfo> documents =
                visitorEdit.documents()
                        .stream()
                        .map(document -> new VendorEditResponse.DocumentInfo(
                                document.documentId(),
                                document.documentType(),
                                null,
                                document.createdAt()
                        ))
                        .toList();

        List<VendorEditResponse.VisitInfo> visits =
                visitorEdit.visits()
                        .stream()
                        .map(visit -> new VendorEditResponse.VisitInfo(
                                visit.visitId(),
                                visit.visitReference(),
                                visit.visitorType(),
                                visit.registrationType(),
                                visit.purpose(),
                                visit.hostId(),
                                visit.hostName(),
                                visit.departmentId(),
                                visit.departmentName(),
                                visit.expectedArrivalAt(),
                                visit.expectedDepartureAt(),
                                visit.checkedInAt(),
                                visit.checkedOutAt(),
                                visit.remarks(),
                                visit.status()
                        ))
                        .toList();

        VendorEditResponse.BlacklistInfo blacklist = null;

        if (visitorEdit.blacklist() != null) {
            blacklist = new VendorEditResponse.BlacklistInfo(
                    visitorEdit.blacklist().id(),
                    visitorEdit.blacklist().reason(),
                    visitorEdit.blacklist().status(),
                    visitorEdit.blacklist().createdAt()
            );
        }

        return new VendorEditResponse(
                vendorInfo,
                new VendorEditResponse.VisitorInfo(
                        visitorInfo.id(),
                        visitorInfo.firstName(),
                        visitorInfo.lastName(),
                        visitorInfo.email(),
                        visitorInfo.mobileNumber(),
                        visitorInfo.companyName(),
                        visitorInfo.visitorType(),
                        visitorInfo.nationality()
                ),
                documents,
                List.of(), // NDA history will be populated separately
                visits,
                blacklist,
                visitorEdit.blacklisted()
        );
    }


    private List<VendorEditResponse.NdaInfo> getVendorNdas(
            String visitorId) {

        List<DocumentMetadata> ndaMetadata = documentMetadataRepository
                .findByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        visitorId,
                        DocumentType.NDA
                );

        return ndaMetadata.stream()
                .map(metadata -> {

                    LocalDateTime validUntil =
                            metadata.getValidUntil();

                    String status;

                    if (validUntil == null) {
                        status = "UNKNOWN";
                    } else if (validUntil.isBefore(LocalDateTime.now())) {
                        status = "EXPIRED";
                    } else {
                        status = "ACTIVE";
                    }

                    return new VendorEditResponse.NdaInfo(
                            metadata.getId(),
                            metadata.getDocumentPath(),
                            metadata.getCreatedAt() != null
                                    ? metadata.getCreatedAt().toString()
                                    : null,
                            validUntil,
                            status
                    );
                })
                .toList();
    }
}