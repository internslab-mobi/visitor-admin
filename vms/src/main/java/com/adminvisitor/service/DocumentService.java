package com.adminvisitor.service;

import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
@Service
@RequiredArgsConstructor
public class DocumentService {

    @Value("${vms.document.nda-upload-dir}")
    private String ndaUploadDir;

    private final DocumentRepository documentRepository;
    private final VisitorRepository visitorRepository;
    private final IdGeneratorService idGeneratorService;

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
            visitor.setCooldownUntil(
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

        LocalDateTime cooldownUntil =
                visitor.getCooldownUntil();

        // No previous valid NDA
        if (cooldownUntil == null) {
            return true;
        }

        // NDA expired
        return cooldownUntil.isBefore(
                LocalDateTime.now()
        );
    }

    /**
     * Retrieve the latest signed NDA for a visitor.
     */
    public Document getLatestNda(String visitorId) {

        return documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(
                        visitorId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
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
}