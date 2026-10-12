package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.VendorEditResponse;
import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Vendor;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.exception.VisitorNotFoundException;
import com.adminvisitor.mapper.VendorMapper;
import com.adminvisitor.repository.DocumentMetadataRepository;
import com.adminvisitor.repository.VendorRepository;
import com.adminvisitor.service.VendorService;

import com.adminvisitor.service.VisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorMapper vendorMapper;
    private final VendorRepository vendorRepository;
    private final VisitorService visitorService;
    private final DocumentMetadataRepository documentMetadataRepository;

    public List<VendorResponse> getAllVendors() {

        return vendorRepository.findAll()
                .stream()
                .map(vendorMapper::toResponse)
                .toList();
    }

    public VendorResponse getVendorById(String id) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new VisitorNotFoundException(
                                "Vendor not found with id: " + id
                        )
                );

        return mapToResponse(vendor);
    }

//    private VendorResponse mapToResponse(Vendor vendor) {
//
//        return new VendorResponse(
//                vendor.getId(),
//                vendor.getVisitor().getId(),
//                vendor.getFirstName(),
//                vendor.getLastName(),
//                vendor.getEmail(),
//                vendor.getMobileNumber(),
//                vendor.getCompanyName()
//        );
//    }


    private VendorResponse mapToResponse(Vendor vendor) {
        return vendorMapper.toResponse(vendor);
    }

    @Transactional(readOnly = true)
    public VendorEditResponse getVendorEditDetails(String vendorId) {

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() ->
                        new VisitorNotFoundException(
                                "Vendor not found with id: " + vendorId
                        )
                );

        VisitorEditResponse visitorEdit =
                visitorService.getVisitorEditDetails(
                        vendor.getVisitor().getId()
                );

        List<VendorEditResponse.NdaInfo> ndas =
                getVendorNdas(vendor.getVisitor().getId());

        return vendorMapper.toEditResponse(
                vendor,
                visitorEdit,
                ndas
        );
    }

    private List<VendorEditResponse.NdaInfo> getVendorNdas(
            String visitorId
    ) {
        List<DocumentMetadata> ndaMetadata =
                documentMetadataRepository
                        .findByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                                visitorId,
                                DocumentType.NDA
                        );

        LocalDateTime now = LocalDateTime.now();

        return ndaMetadata.stream()
                .map(metadata -> {
                    LocalDateTime validUntil = metadata.getValidUntil();

                    return new VendorEditResponse.NdaInfo(
                            metadata.getId(),
                            metadata.getDocumentPath(),
                            metadata.getCreatedAt() != null
                                    ? metadata.getCreatedAt().toString()
                                    : null,
                            validUntil,
                            getNdaStatus(validUntil, now)
                    );
                })
                .toList();
    }

    private String getNdaStatus(
            LocalDateTime validUntil,
            LocalDateTime now
    ) {
        if (validUntil == null) {
            return "UNKNOWN";
        }

        return validUntil.isBefore(now)
                ? "EXPIRED"
                : "ACTIVE";
    }
}