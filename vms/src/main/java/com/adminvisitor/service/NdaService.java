package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.dto.responsedto.NdaExtensionResult;
import com.adminvisitor.dto.responsedto.NdaLatestResult;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.repository.DocumentMetadataRepository;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class NdaService {

    private final DocumentRepository documentRepository;
    private final VisitorRepository visitorRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final VisitRepository visitRepository;
    private final IdGeneratorService idGeneratorService;

    @Value("${vms.document.nda-upload-dir}")
    private String ndaUploadDir;

    @Value("${vms.document.upload-dir}")
    private String documentUploadDir;


    @Transactional
    public DocumentMetadata uploadNewNda(
            String visitorId,
            MultipartFile ndaFile,
            LocalDate validUntil
    ) {

        // 1. Validate NDA file
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

        // 4. Verify that the visitor is a vendor
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
        String metadataId =
                idGeneratorService.generateId(
                        "DOCUMENT_METADATA",
                        "DMD"
                );

        try {

            // 8. Create visitor-specific NDA directory
            Path visitorDirectory =
                    Paths.get(ndaUploadDir, visitorId);

            Files.createDirectories(visitorDirectory);

            // 9. Create unique NDA filename
            String fileName = metadataId + ".pdf";

            Path filePath =
                    visitorDirectory.resolve(fileName);

            // 10. Save NDA file
            Files.write(
                    filePath,
                    ndaFile.getBytes()
            );

            // 11. Create NDA metadata
            DocumentMetadata metadata =
                    new DocumentMetadata();

            metadata.setId(metadataId);
            metadata.setDocument(document);
            metadata.setDocumentType(DocumentType.NDA);
            metadata.setDocumentPath(filePath.toString());

            metadata.setValidUntil(
                    validUntil.atTime(LocalTime.MAX)
            );

            metadata.setOverwrittenBy(null);

            // 12. Save metadata
            return documentMetadataRepository.save(metadata);

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save NDA file",
                    exception
            );
        }
    }


    @Transactional
    public NdaExtensionResult extendNdaValidity(
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
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
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

        // Find existing Document
        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found for visitor: " + visitorId
                        )
                );

        // Generate new metadata ID
        String newNdaMetadataId =
                idGeneratorService.generateId(
                        "DOCUMENT_METADATA",
                        "DMD"
                );

        try {

            // Create visitor-specific NDA directory
            Path visitorDirectory =
                    Paths.get(ndaUploadDir, visitorId);

            Files.createDirectories(visitorDirectory);

            // Save supporting document as the new NDA
            String fileName = newNdaMetadataId + ".pdf";

            Path filePath =
                    visitorDirectory.resolve(fileName);

            Files.write(
                    filePath,
                    supportingDocument.getBytes()
            );

            // Create new NDA metadata
            DocumentMetadata newNdaMetadata =
                    new DocumentMetadata();

            newNdaMetadata.setId(newNdaMetadataId);
            newNdaMetadata.setDocument(document);
            newNdaMetadata.setDocumentType(DocumentType.NDA);
            newNdaMetadata.setDocumentPath(filePath.toString());
            newNdaMetadata.setValidUntil(
                    newValidUntil.atTime(LocalTime.MAX)
            );
            newNdaMetadata.setOverwrittenBy(null);

            // Mark old NDA as overwritten by the new NDA
            existingNda.setOverwrittenBy(
                    newNdaMetadata.getId()
            );

            documentMetadataRepository.save(existingNda);
            DocumentMetadata savedNda =
                    documentMetadataRepository.save(newNdaMetadata);

            return new NdaExtensionResult(
                    savedNda,
                    existingNda.getCreatedAt()
            );

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save supporting document",
                    exception
            );
        }
    }



    @Transactional(readOnly = true)
    public NdaLatestResult getLatestNda(String visitorId) {

        visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId));

        List<DocumentMetadata> ndaHistory =
                documentMetadataRepository
                        .findByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                                visitorId,
                                DocumentType.NDA);

        DocumentMetadata latestNda = ndaHistory.stream()
                .filter(nda -> nda.getOverwrittenBy() == null)
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No active NDA found for visitor: " + visitorId));

        LocalDateTime validFrom =
                ndaHistory.get(ndaHistory.size() - 1).getCreatedAt();

        return new NdaLatestResult(
                latestNda,
                validFrom
        );
    }


    @Transactional(readOnly = true)
    public List<DocumentResponse> getNdaHistory(String visitorId) {

        // Verify visitor exists
        visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );

        List<DocumentMetadata> ndaHistory =
                documentMetadataRepository
                        .findByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                                visitorId,
                                DocumentType.NDA
                        );

        if (ndaHistory.isEmpty()) {
            return List.of();
        }


        LocalDateTime originalNdaCreatedAt =
                ndaHistory.get(ndaHistory.size() - 1).getCreatedAt();

        return ndaHistory.stream()
                .map(metadata -> {

                    LocalDateTime validUntil =
                            metadata.getValidUntil();

                    return new DocumentResponse(
                            metadata.getId(),
                            metadata.getDocumentPath(),

                            // Actual creation/upload time of THIS document
                            metadata.getCreatedAt() != null
                                    ? metadata.getCreatedAt().toString()
                                    : null,

                            // Valid From = original NDA's createdAt
                            originalNdaCreatedAt != null
                                    ? originalNdaCreatedAt.toString()
                                    : null,

                            validUntil != null
                                    ? validUntil.toString()
                                    : null,

                            metadata.getOverwrittenBy()
                    );


                })
                .toList();
    }


    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        boolean validContentType = "application/pdf".equalsIgnoreCase(contentType);
        boolean validExtension = fileName != null && fileName.toLowerCase().endsWith(".pdf");

        return validContentType && validExtension;
    }
}
