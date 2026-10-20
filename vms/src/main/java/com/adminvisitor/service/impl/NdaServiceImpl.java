package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.DocumentResponse;

import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.NdaStatus;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.mapper.DocumentMapper;
import com.adminvisitor.repository.DocumentMetadataRepository;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import com.adminvisitor.service.NdaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
//import java.nio.file.Files;
import java.nio.file.Files;
import java.nio.file.Path;
//import java.nio.file.Paths;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import com.adminvisitor.service.DocumentStorageService;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
@Service
@RequiredArgsConstructor
public class NdaServiceImpl implements NdaService {

    private final DocumentRepository documentRepository;
    private final VisitorRepository visitorRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final VisitRepository visitRepository;
    private final com.adminvisitor.service.IdGeneratorService idGeneratorService;
    private final DocumentStorageService documentStorageService;
    private final DocumentMapper documentMapper;
    @Value("${vms.document.nda-upload-dir}")
    private String ndaUploadDir;



    @Transactional
    public DocumentMetadata uploadNewNda(
            String visitorId,
            MultipartFile ndaFile,
            LocalDate validUntil
    ) {
        if (ndaFile == null || ndaFile.isEmpty()) {
            throw new IllegalArgumentException("NDA file is required");
        }

        if (!isPdf(ndaFile)) {
            throw new IllegalArgumentException("Only PDF files are allowed for NDA");
        }

        // 2. Validate valid-until date
        if (validUntil == null) {
            throw new IllegalArgumentException("Valid until date is required");
        }

        if (validUntil.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Valid until date cannot be in the past"
            );
        }

        // 3. Find visitor
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );


        Visit visit = visitRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No visit found for visitor: " + visitorId
                        )
                );

        if (visit.getVisitorType() != VisitorType.VENDOR) {
            throw new BusinessRuleException(
                    "NDA can only be uploaded for vendor visitors"
            );
        }

        // 5. Check the latest existing NDA
        Optional<DocumentMetadata> existingNda =
                documentMetadataRepository
                        .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                                visitorId,
                                DocumentType.NDA
                        );

        if (existingNda.isPresent()) {

            LocalDateTime existingValidUntil =
                    existingNda.get().getValidUntil();

            if (existingValidUntil != null
                    && existingValidUntil.isAfter(LocalDateTime.now())) {

                throw new BusinessRuleException(
                        "Existing NDA is still valid. "
                                + "Use NDA extension endpoint with supporting document instead."
                );
            }
        }

        // 6. Find the visitor's Document record
        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found for visitor: " + visitorId
                        )
                );

        // 7. Generate unique metadata ID
        String metadataId = idGeneratorService.generateId("DOCUMENT_METADATA","DMD");

        try {

            // 8. Store NDA using the shared storage service
            Path filePath = documentStorageService.store(
                    ndaFile,
                    ndaUploadDir,
                    visitorId,
                    metadataId
            );

            // 9. Create NDA metadata

            DocumentMetadata metadata = createNdaMetadata(
                    metadataId,
                    document,
                    filePath,
                    LocalDateTime.now(),
                    validUntil,
                    DocumentType.NDA
            );

            return documentMetadataRepository.save(metadata);

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save NDA file",
                    exception
            );
        }
    }



    @Transactional
    public  DocumentMetadata extendNdaValidity(
            String visitorId,
            MultipartFile supportingDocument,
            LocalDate newValidUntil
    ) {

        // Validate supporting document
        if (supportingDocument == null || supportingDocument.isEmpty()) {
            throw new IllegalArgumentException(
                    "Supporting document is required for NDA extension"
            );
        }

        if (!isPdf(supportingDocument)) {
            throw new IllegalArgumentException(
                    "Only PDF files are allowed for supporting document"
            );
        }

        // Validate new validity date
        if (newValidUntil == null) {
            throw new IllegalArgumentException(
                    "New valid until date is required"
            );
        }

        if (newValidUntil.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "New valid until date cannot be in the past"
            );
        }

        // Find visitor
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );

        // Find existing NDA
        DocumentMetadata existingNda = documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        visitorId,
                        DocumentType.NDA
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existing NDA found for visitor: " + visitorId
                        )
                );

        // Existing NDA must still be valid
        LocalDateTime existingValidUntil = existingNda.getValidUntil();

        if (existingValidUntil == null
                || existingValidUntil.isBefore(LocalDateTime.now())) {

            throw new BusinessRuleException(
                    "Existing NDA has expired. Use upload new NDA endpoint instead."
            );
        }

        // New validity must extend the existing NDA
        if (!newValidUntil.isAfter(existingValidUntil.toLocalDate())) {
            throw new BusinessRuleException(
                    "New valid until date must be after the existing NDA expiry date"
            );
        }

        // Determine document type based on the extension period
        LocalDate existingExpiryDate = existingValidUntil.toLocalDate();

        DocumentType extensionDocumentType;

        if (newValidUntil.isBefore(existingExpiryDate.plusYears(1))) {

            // Less than 1 year
            extensionDocumentType = DocumentType.NDA_SUPPORTING;

        } else if (newValidUntil.isBefore(existingExpiryDate.plusYears(5))) {

            // 1 year or more, but less than 5 years
            extensionDocumentType = DocumentType.NDA_RENEWAL;

        } else {

            // 5 years or more: extension is not allowed
            throw new BusinessRuleException(
                    "NDA cannot be extended for 5 years or more. "
                            + "Please upload a new NDA instead."
            );
        }


        // Generate new metadata ID
        String newNdaMetadataId =
                idGeneratorService.generateId(
                        "DOCUMENT_METADATA",
                        "DMD"
                );

        try {

            // Store supporting document using the shared storage service
            Path filePath = documentStorageService.store(
                    supportingDocument,
                    ndaUploadDir,
                    visitorId,
                    newNdaMetadataId
            );
            Document document = existingNda.getDocument();
            // Create new NDA metadata
            DocumentMetadata newNdaMetadata = createNdaMetadata(
                    newNdaMetadataId,
                    document,
                    filePath,
                    existingNda.getCreatedAt(),
                    newValidUntil,
                    extensionDocumentType
            );


            // Mark old NDA as overwritten by the new NDA
            existingNda.setOverwrittenBy(
                    newNdaMetadata.getId()
            );
            existingNda.setStatus(NdaStatus.SUPERSEDED);
            documentMetadataRepository.save(existingNda);

            DocumentMetadata savedNda =
                    documentMetadataRepository.save(newNdaMetadata);

            return savedNda;


        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save supporting document",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public DocumentMetadata getLatestNda(String visitorId) {

        visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        ));

//        return documentMetadataRepository
//                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
//                        visitorId,
//                        DocumentType.NDA
//                )
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "No NDA found for visitor: " + visitorId
//                        ));

        return documentMetadataRepository
                .findFirstByDocument_Visitor_IdAndDocumentTypeInAndOverwrittenByIsNullOrderByCreatedAtDesc(
                        visitorId,
                        List.of(
                                DocumentType.NDA,
                                DocumentType.NDA_SUPPORTING,
                                DocumentType.NDA_RENEWAL
                        )
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No NDA found for visitor: " + visitorId
                        ));
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getNdaHistory(String visitorId) {

        // Verify visitor exists
        visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        ));

        // Fetch NDA history, newest first
        List<DocumentMetadata> ndaHistory =
                documentMetadataRepository
                        .findByDocument_Visitor_IdAndDocumentTypeInOrderByCreatedAtDesc(
                                visitorId,
                                List.of(
                                        DocumentType.NDA,
                                        DocumentType.NDA_SUPPORTING,
                                        DocumentType.NDA_RENEWAL
                                )
                        );

        // Map entity records to API response DTOs
        return ndaHistory.stream()
                .map(documentMapper::toResponse)
                .toList();
    }




    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        boolean validContentType = "application/pdf".equalsIgnoreCase(contentType);
        boolean validExtension = fileName != null && fileName.toLowerCase().endsWith(".pdf");

        return validContentType && validExtension;
    }



    private DocumentMetadata createNdaMetadata(
            String metadataId,
            Document document,
            Path filePath,
            LocalDateTime validFrom,
            LocalDate validUntil,
            DocumentType documentType
    ) {
        DocumentMetadata metadata = new DocumentMetadata();

        metadata.setId(metadataId);
        metadata.setDocument(document);
//        metadata.setDocumentType(DocumentType.NDA);
        metadata.setDocumentType(documentType);
        metadata.setDocumentPath(filePath.toString());
        metadata.setEffective_validity(validFrom);
        metadata.setValidUntil(validUntil.atTime(LocalTime.MAX));
        metadata.setOverwrittenBy(null);
        metadata.setStatus(NdaStatus.ACTIVE);

        return metadata;
    }


    public boolean isNdaRequired(Visitor visitor) {

        Optional<DocumentMetadata> latestNda =
                documentMetadataRepository
                        .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                                visitor.getId(),
                                DocumentType.NDA
                        );

        // No NDA exists
        if (latestNda.isEmpty()) {
            return true;
        }

        LocalDateTime validUntil =
                latestNda.get().getValidUntil();

        // NDA has no validity date
        if (validUntil == null) {
            return true;
        }

        // NDA is expired
        return !validUntil.isAfter(LocalDateTime.now());
    }

    public Resource getNdaFile(String visitorId) {

        DocumentMetadata metadata = getLatestNda(visitorId);

        Path filePath = Paths.get(
                metadata.getDocumentPath()
        );

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "NDA file not found"
            );
        }

        return new FileSystemResource(filePath);
    }



    @Scheduled(fixedRate = 300000)
    @Transactional
    public void updateExpiredNdaStatuses() {

        LocalDateTime now = LocalDateTime.now();

        List<DocumentMetadata> expiredNdas =
                documentMetadataRepository
                        .findByDocumentTypeAndStatusAndValidUntilLessThanEqual(
                                DocumentType.NDA,
                                NdaStatus.ACTIVE,
                                now
                        );

        for (DocumentMetadata nda : expiredNdas) {
            nda.setStatus(NdaStatus.EXPIRED);


        }

        documentMetadataRepository.saveAll(expiredNdas);
    }

}

