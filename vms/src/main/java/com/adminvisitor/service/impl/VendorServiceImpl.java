package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.VendorEditResponse;
import com.adminvisitor.dto.responsedto.VendorResponse;
import com.adminvisitor.dto.responsedto.VisitorEditResponse;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Vendor;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.VendorMapper;
import com.adminvisitor.repository.DocumentMetadataRepository;
import com.adminvisitor.repository.VendorRepository;
import com.adminvisitor.service.NdaService;
import com.adminvisitor.service.VendorService;
import com.adminvisitor.service.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VendorServiceImpl implements VendorService {

    private final VendorMapper vendorMapper;
    private final VendorRepository vendorRepository;
    private final VisitorService visitorService;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final NdaService ndaService;

    public List<VendorResponse> getAllVendors() {

        List<VendorResponse> vendors = vendorRepository.findAll()
                .stream()
                .map(vendorMapper::toResponse)
                .toList();

        log.debug("VENDOR_LIST_RETRIEVED count={}", vendors.size());

        return vendors;
    }

    public VendorResponse getVendorById(String id) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vendor not found with id: " + id
                        )
                );

        return mapToResponse(vendor);
    }

    private VendorResponse mapToResponse(Vendor vendor) {
        return vendorMapper.toResponse(vendor);
    }

//    @Transactional(readOnly = true)
//    public VendorEditResponse getVendorEditDetails(String vendorId) {
//
//        Vendor vendor = vendorRepository.findById(vendorId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Vendor not found with id: " + vendorId
//                        )
//                );
//
//        VisitorEditResponse visitorEdit =
//                visitorService.getVisitorEditDetails(
//                        vendor.getVisitor().getId()
//                );
//
//        List<VendorEditResponse.NdaInfo> ndas =
//                getVendorNdas(vendor.getVisitor().getId());
//
//        log.debug(
//                "VENDOR_EDIT_DETAILS_RETRIEVED vendorId={} ndaCount={}",
//                vendorId,
//                ndas.size()
//        );
//
//        return vendorMapper.toEditResponse(
//                vendor,
//                visitorEdit,
//                ndas
//        );
//    }

    @Transactional(readOnly = true)
    public VendorEditResponse getVendorEditDetails(String vendorId) {

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vendor not found with id: " + vendorId
                        )
                );

        String visitorId = vendor.getVisitor().getId();

        VisitorEditResponse visitorEdit =
                visitorService.getVisitorEditDetails(visitorId);

        List<VendorEditResponse.NdaInfo> ndas =
                getVendorNdas(visitorId);

        log.debug(
                "VENDOR_EDIT_DETAILS_RETRIEVED vendorId={} ndaCount={}",
                vendorId,
                ndas.size()
        );

        return vendorMapper.toEditResponse(
                vendor,
                visitorEdit,
                ndas
        );
    }
    private List<VendorEditResponse.NdaInfo> getVendorNdas(
            String visitorId) {

        return ndaService.getNdaHistory(visitorId)
                .stream()
                .filter(nda -> DocumentType.NDA.name()
                        .equals(nda.documentType()))
                .map(nda -> new VendorEditResponse.NdaInfo(
                        nda.documentId(),
                        nda.documentPath(),
                        nda.createdAt(),
                        nda.validUntil() != null
                                ? LocalDateTime.parse(nda.validUntil())
                                : null,
                        nda.status()
                ))
                .toList();
    }

}