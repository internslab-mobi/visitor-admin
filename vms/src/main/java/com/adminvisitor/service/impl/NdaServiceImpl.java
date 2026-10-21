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
import com.adminvisitor.service.DocumentStorageService;
import com.adminvisitor.service.IdGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
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
@Slf4j
public class NdaServiceImpl implements NdaService {

    private final DocumentRepository documentRepository;
    private final VisitorRepository visitorRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final VisitRepository visitRepository;
    private final IdGeneratorService idGeneratorService;
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

        if (validUntil == null) {
            throw new IllegalArgumentException("Valid until date is required");
        }

        if (validUntil.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Valid until date cannot be in the past"
            );
        }

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

        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found for visitor: " + visitorId
                        )
                );

        String metadataId =
                idGeneratorService.generateId("DOCUMENT_METADATA", "DMD");

        try {
            Path filePath = documentStorageService.store(
                    ndaFile,
                    ndaUploadDir,
                    visitorId,
                    metadataId
            );

            DocumentMetadata metadata = createNdaMetadata(
                    metadataId,
                    document,
                    filePath,
                    LocalDateTime.now(),
                    validUntil,
                    DocumentType.NDA
            );

            DocumentMetadata savedMetadata =
                    documentMetadataRepository.save(metadata);

            log.info(
                    "NDA_UPLOADED metadataId={} visitorId={} documentType={} validUntil={}",
                    savedMetadata.getId(),
                    visitorId,
                    savedMetadata.getDocumentType(),
                    savedMetadata.getValidUntil()
            );

            return savedMetadata;

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Failed to save NDA file",
                    exception
            );
        }
    }

    @Transactional
    public DocumentMetadata extendNdaValidity(
            String visitorId,
            MultipartFile supportingDocument,
            LocalDate newValidUntil
    ) {
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

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );

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

        LocalDateTime existingValidUntil = existingNda.getValidUntil();

        if (existingValidUntil == null
                || existingValidUntil.isBefore(LocalDateTime.now())) {

            throw new BusinessRuleException(
                    "Existing NDA has expired. Use upload new NDA endpoint instead."
            );
        }

        if (!newValidUntil.isAfter(existingValidUntil.toLocalDate())) {
            throw new BusinessRuleException(
                    "New valid until date must be after the existing NDA expiry date"
            );
        }

        LocalDate existingExpiryDate = existingValidUntil.toLocalDate();

        DocumentType extensionDocumentType;

        if (newValidUntil.isBefore(existingExpiryDate.plusYears(1))) {
            extensionDocumentType = DocumentType.NDA_SUPPORTING;

        } else if (newValidUntil.isBefore(existingExpiryDate.plusYears(5))) {
            extensionDocumentType = DocumentType.NDA_RENEWAL;

        } else {
            throw new BusinessRuleException(
                    "NDA cannot be extended for 5 years or more. "
                            + "Please upload a new NDA instead."
            );
        }

        String newNdaMetadataId =
                idGeneratorService.generateId(
                        "DOCUMENT_METADATA",
                        "DMD"
                );

        try {
            Path filePath = documentStorageService.store(
                    supportingDocument,
                    ndaUploadDir,
                    visitorId,
                    newNdaMetadataId
            );

            Document document = existingNda.getDocument();

            DocumentMetadata newNdaMetadata = createNdaMetadata(
                    newNdaMetadataId,
                    document,
                    filePath,
                    existingNda.getCreatedAt(),
                    newValidUntil,
                    extensionDocumentType
            );

            existingNda.setOverwrittenBy(newNdaMetadata.getId());
            existingNda.setStatus(NdaStatus.SUPERSEDED);

            documentMetadataRepository.save(existingNda);

            DocumentMetadata savedNda =
                    documentMetadataRepository.save(newNdaMetadata);

            log.info(
                    "NDA_VALIDITY_EXTENDED previousMetadataId={} newMetadataId={} visitorId={} documentType={} validUntil={}",
                    existingNda.getId(),
                    savedNda.getId(),
                    visitorId,
                    savedNda.getDocumentType(),
                    savedNda.getValidUntil()
            );

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
                        )
                );

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

        visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        ));

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

        log.debug(
                "NDA_HISTORY_RETRIEVED visitorId={} recordCount={}",
                visitorId,
                ndaHistory.size()
        );

        return ndaHistory.stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    private boolean isPdf(MultipartFile file) {
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();

        boolean validContentType =
                "application/pdf".equalsIgnoreCase(contentType);

        boolean validExtension =
                fileName != null && fileName.toLowerCase().endsWith(".pdf");

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

        if (latestNda.isEmpty()) {
            return true;
        }

        LocalDateTime validUntil = latestNda.get().getValidUntil();

        if (validUntil == null) {
            return true;
        }

        return !validUntil.isAfter(LocalDateTime.now());
    }

    public Resource getNdaFile(String visitorId) {

        DocumentMetadata metadata = getLatestNda(visitorId);

        Path filePath = Paths.get(metadata.getDocumentPath());

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("NDA file not found");
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

        if (!expiredNdas.isEmpty()) {
            log.info(
                    "EXPIRED_NDA_STATUSES_UPDATED count={}",
                    expiredNdas.size()
            );
        }
    }
}