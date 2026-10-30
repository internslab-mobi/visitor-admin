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

/**
 * Service implementation for managing vendor-related operations.
 *
 * <p>Provides functionality to retrieve vendor information, load vendor
 * edit details, and retrieve NDA history associated with a vendor.</p>
 *
 * <p>Uses {@link VendorRepository} for vendor data access,
 * {@link VendorMapper} for mapping entities to response DTOs,
 * {@link VisitorService} for retrieving visitor details, and
 * {@link NdaService} for retrieving NDA history.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VendorServiceImpl implements VendorService {

    private final VendorMapper vendorMapper;
    private final VendorRepository vendorRepository;
    private final VisitorService visitorService;
    private final NdaService ndaService;

    /**
     * Retrieves all vendors and maps them to response DTOs.
     *
     * @return a list of vendor response objects
     */
    public List<VendorResponse> getAllVendors() {

        List<VendorResponse> vendors = vendorRepository.findAll()
                .stream()
                .map(vendorMapper::toResponse)
                .toList();

        log.debug("VENDOR_LIST_RETRIEVED count={}", vendors.size());

        return vendors;
    }

    /**
     * Retrieves a vendor by its unique identifier.
     *
     * @param id the unique identifier of the vendor
     * @return the vendor response object
     * @throws ResourceNotFoundException if no vendor exists with the given ID
     */
    public VendorResponse getVendorById(String id) {

        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Vendor not found with id: " + id
                        )
                );

        return mapToResponse(vendor);
    }
    /**
     * Maps a vendor entity to its response DTO.
     *
     * @param vendor the vendor entity to map
     * @return the mapped vendor response object
     */
    private VendorResponse mapToResponse(Vendor vendor) {
        return vendorMapper.toResponse(vendor);
    }


    /**
     * Retrieves the details required to edit a vendor.
     *
     * <p>Loads the vendor, retrieves the associated visitor's edit details,
     * and collects the vendor's NDA history before mapping the information
     * into a vendor edit response.</p>
     *
     * @param vendorId the unique identifier of the vendor
     * @return the vendor edit response containing vendor, visitor, and NDA details
     * @throws ResourceNotFoundException if no vendor exists with the given ID
     */
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

    /**
     * Retrieves and maps the NDA history associated with a vendor's visitor.
     *
     * <p>Filters the NDA history to include only documents whose type is
     * {@link DocumentType#NDA}, then maps the document details into
     * {@link VendorEditResponse.NdaInfo} objects.</p>
     *
     * @param visitorId the unique identifier of the visitor associated with the vendor
     * @return a list of NDA information objects
     */
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