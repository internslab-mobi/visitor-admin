package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.dto.responsedto.NdaStatusResponse;
import com.adminvisitor.dto.responsedto.IdentityProofResponse;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.exception.BadgeAlreadyExistsException;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
@Service
@RequiredArgsConstructor
public class DocumentService {

    @Value("${vms.document.nda-upload-dir}")
    private String ndaUploadDir;

    private final DocumentRepository documentRepository;
    private final VisitorRepository visitorRepository;
    private final IdGeneratorService idGeneratorService;
    private final VisitRepository visitRepository;
    private final AesEncryptionService aesEncryptionService;

    /**
     * Upload and save a newly signed NDA for a visitor.
     */
    public Document uploadSignedNda(
            String visitorId,
            MultipartFile file
    ) {

        // 1. Validate file
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Signed NDA file is required"
            );
        }

        if (!isPdf(file)) {
            throw new IllegalArgumentException(
                    "Only PDF files are allowed for NDA"
            );
        }

        // 2. Find visitor
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Visitor not found: " + visitorId
                        )
                );

        // 3. NDA is allowed only for vendors
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


        // 3. Check NDA validity
        if (!isNdaRequired(visitor)) {
            throw new BadgeAlreadyExistsException(
                    "Valid NDA already exists for visitor: " + visitorId
            );
        }

        // 3. Generate document ID
        String documentId =
                idGeneratorService.generateId(
                        "DOCUMENT",
                        "doc"
                );

        try {

            // 4. Create visitor-specific directory
            Path visitorDirectory = Paths.get(
                    ndaUploadDir,
                    visitorId
            );

            Files.createDirectories(visitorDirectory);

            // 5. Create unique file name
            String fileName = documentId + ".pdf";

            Path filePath = visitorDirectory.resolve(fileName);

            // 6. Save PDF to local filesystem
            Files.write(
                    filePath,
                    file.getBytes()
            );

            // 7. Create document record
            Document document = new Document();

            document.setId(documentId);
            document.setVisitor(visitor);
            document.setNdaDocument(
                    filePath.toString()
            );

            // 8. Save document record in database
            Document savedDocument =
                    documentRepository.save(document);

            // 9. NDA is valid for 6 months
            visitor.setValidity(
                    LocalDateTime.now().plusMonths(6)
            );

            visitorRepository.save(visitor);

            // 10. Return saved document
            return savedDocument;

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save signed NDA file",
                    exception
            );
        }
    }

    /**
     * Check whether an NDA file is required for the visitor.
     */
    public boolean isNdaRequired(Visitor visitor) {

        LocalDateTime validity =
                visitor.getValidity();

        // No valid cooldown
        if (validity == null) {
            return true;
        }

        // Cooldown expired
        if (validity.isBefore(LocalDateTime.now())) {
            return true;
        }

        // Cooldown is valid, but does an NDA actually exist?
        return documentRepository
                .findTopByVisitorIdAndNdaDocumentIsNotNullOrderByCreatedAtDesc(
                        visitor.getId()
                )
                .isEmpty();
    }
    /**
     * Retrieve the latest signed NDA for a visitor.
     */
    public Document getLatestNda(String visitorId) {

        return documentRepository
                .findTopByVisitorIdAndNdaDocumentIsNotNullOrderByCreatedAtDesc(
                        visitorId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No NDA found for visitor: "
                                        + visitorId
                        )
                );
    }

    /**
     * Validate that the uploaded file is a PDF.
     */
    private boolean isPdf(MultipartFile file) {

        String contentType = file.getContentType();

        String fileName = file.getOriginalFilename();

        boolean validContentType =
                "application/pdf".equalsIgnoreCase(contentType);

        boolean validExtension =
                fileName != null
                        && fileName.toLowerCase()
                        .endsWith(".pdf");

        return validContentType && validExtension;
    }

    public Document getValidNda(Visitor visitor) {

        LocalDateTime validity = visitor.getValidity();

        if (validity == null) {
            return null;
        }

        if (validity.isBefore(LocalDateTime.now())) {
            return null;
        }

        return documentRepository
                .findTopByVisitorIdAndNdaDocumentIsNotNullOrderByCreatedAtDesc(
                        visitor.getId()
                )
                .orElse(null);
    }

    public Resource getNdaFile(String visitorId) {

        Document document = getLatestNda(visitorId);

        Path filePath = Paths.get(document.getNdaDocument());

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("NDA file not found");
        }

        return new FileSystemResource(filePath);
    }

    @Transactional
    public Document saveIdentityProofs(
            Visitor visitor,
            Nationality nationality,
            String aadharNumber,
            String panNumber,
            String passportNumber
    ) {

        String documentId =
                idGeneratorService.generateId(
                        "DOCUMENT",
                        "doc"
                );

        Document document = new Document();

        document.setId(documentId);
        document.setVisitor(visitor);

        document.setNationality(nationality);

        if (aadharNumber != null && !aadharNumber.isBlank()) {
            document.setAadharNumber(
                    aesEncryptionService.encrypt(aadharNumber)
            );
        }

        if (panNumber != null && !panNumber.isBlank()) {
            document.setPanNumber(
                    aesEncryptionService.encrypt(panNumber)
            );
        }

        if (passportNumber != null && !passportNumber.isBlank()) {
            document.setPassportNumber(
                    aesEncryptionService.encrypt(passportNumber)
            );
        }

        return documentRepository.save(document);
    }

    public IdentityProofResponse getIdentityProofs(String visitorId) {

        Document document =
                documentRepository
                        .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Identity document not found for visitor: "
                                                + visitorId
                                )
                        );

        String aadharNumber = null;
        String panNumber = null;
        String passportNumber = null;

        if (document.getAadharNumber() != null) {
            aadharNumber =
                    maskAadhar(
                            aesEncryptionService.decrypt(
                                    document.getAadharNumber()
                            )
                    );
        }

        if (document.getPanNumber() != null) {
            panNumber =
                    maskPan(
                            aesEncryptionService.decrypt(
                                    document.getPanNumber()
                            )
                    );
        }

        if (document.getPassportNumber() != null) {
            passportNumber =
                    maskPassport(
                            aesEncryptionService.decrypt(
                                    document.getPassportNumber()
                            )
                    );
        }

        return new IdentityProofResponse(
                document.getId(),
                document.getVisitor().getId(),
                document.getNationality().name(),
                aadharNumber,
                panNumber,
                passportNumber
        );
    }

    private String maskAadhar(String aadharNumber) {

        return "XXXX XXXX "
                + aadharNumber.substring(
                aadharNumber.length() - 4
        );
    }

    private String maskPan(String panNumber) {

        return "XXXXXX"
                + panNumber.substring(
                panNumber.length() - 4
        );
    }

    private String maskPassport(String passportNumber) {

        if (passportNumber.length() <= 4) {
            return "XXXX";
        }

        return "XXXX"
                + passportNumber.substring(
                passportNumber.length() - 4
        );
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments(String visitorId) {

        visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );

        return documentRepository.findByVisitorId(visitorId)
                .stream()
                .map(document -> new DocumentResponse(
                        document.getNdaDocument(),
                        document.getCreatedAt().toString(),
                        document.getUpdatedAt().toString()
                ))
                .toList();
    }
}