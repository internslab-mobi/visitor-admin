package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.dto.responsedto.IdentityProofResponse;
// import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.VisitorType;
import com.adminvisitor.exception.BadgeAlreadyExistsException;
import com.adminvisitor.exception.BusinessRuleException;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.repository.DocumentMetadataRepository;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final VisitorRepository visitorRepository;
    private final IdGeneratorService idGeneratorService;
    private final DocumentMetadataRepository documentMetadataRepository;

     //Active service for generating HMAC-SHA-256 blind indexes.

    private final HmacBlindIndexService hmacBlindIndexService;


    @Value("${vms.document.nda-upload-dir}")
    private String ndaUploadDir;

    @Value("${vms.document.upload-dir}")
    private String documentUploadDir;
    private final VisitRepository visitRepository;

    @Value("${vms.document.photo-upload-dir}")
    private String photoUploadDir;


    @Transactional
    public DocumentMetadata uploadVisitorPhoto(
            String visitorId,
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Visitor photo is required"
            );
        }

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );

        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found for visitor: " + visitorId
                        )
                );

        /*
         * Maximum visitor photo size: 5 MB.
         */
        final long MAX_FILE_SIZE = 5 * 1024 * 1024;

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Visitor photo cannot exceed 5 MB"
            );
        }

        /*
         * Only image files are allowed.
         */
        String contentType = file.getContentType();

        if (contentType == null
                || !contentType.startsWith("image/")) {

            throw new IllegalArgumentException(
                    "Only image files are allowed for visitor photo"
            );
        }

        String metadataId =
                idGeneratorService.generateId(
                        "DOCUMENT_METADATA",
                        "DMD"
                );

        try {

            /*
             * Store photos in a visitor-specific directory.
             *
             * Example:
             * visitor-photo/VTR-022/DMD001.jpg
             */
            Path visitorDirectory =
                    Paths.get(
                            photoUploadDir,
                            visitorId
                    );

            Files.createDirectories(visitorDirectory);

            String originalFileName =
                    file.getOriginalFilename();

            String extension = "";

            if (originalFileName != null
                    && originalFileName.contains(".")) {

                extension =
                        originalFileName.substring(
                                originalFileName.lastIndexOf(".")
                        );
            }

            String storedFileName =
                    metadataId + extension;

            Path filePath =
                    visitorDirectory.resolve(
                            storedFileName
                    );

            Files.write(
                    filePath,
                    file.getBytes()
            );

            DocumentMetadata metadata =
                    new DocumentMetadata();

            metadata.setId(metadataId);
            metadata.setDocument(document);
            metadata.setDocumentType(
                    DocumentType.VISITOR_PHOTO
            );
            metadata.setDocumentPath(
                    filePath.toString()
            );

            return documentMetadataRepository.save(metadata);

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save visitor photo",
                    exception
            );
        }
    }


    @Transactional
    public Document uploadSignedNda(
            String visitorId,
            MultipartFile file,
            LocalDate validUntil

    ) {

        // 1. File validation
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Signed NDA file is required"
            );
        }

        // 2. PDF validation
        if (!isPdf(file)) {
            throw new IllegalArgumentException(
                    "Only PDF files are allowed for NDA"
            );
        }
      //  NDA validity date validation
        if (validUntil == null) {
            throw new IllegalArgumentException(
                    "NDA valid until date is required"
            );
        }

        if (validUntil.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "NDA valid until date cannot be in the past"
            );
        }

        // 3. Find visitor
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );

        // 4. Find latest visit
        Visit visit = visitRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No visit found for visitor: " + visitorId
                        )
                );

        // 5. NDA only for vendors
        if (visit.getVisitorType() != VisitorType.VENDOR) {
            throw new BusinessRuleException(
                    "NDA can only be uploaded for vendor visitors"
            );
        }

        // 6. Check NDA validity
        if (!isNdaRequired(visitor)) {
            throw new BusinessRuleException(
                    "Valid NDA already exists for visitor: " + visitorId
            );
        }

        // 7. Find existing Document
        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found for visitor: " + visitorId
                        )
                );

        // 8. Generate metadata ID
        String metadataId =
                idGeneratorService.generateId(
                        "DOCUMENT_METADATA",
                        "DMD"
                );

        try {

            // 9. Create visitor-specific NDA directory
            Path visitorDirectory = Paths.get(
                    ndaUploadDir,
                    visitorId
            );

            Files.createDirectories(visitorDirectory);

            // 10. Create unique NDA filename
            String fileName = metadataId + ".pdf";

            Path filePath = visitorDirectory.resolve(fileName);

            // 11. Save physical file
            Files.write(
                    filePath,
                    file.getBytes()
            );

            // 12. Create metadata record
            DocumentMetadata metadata = new DocumentMetadata();

            metadata.setId(metadataId);
            metadata.setDocument(document);

            // IMPORTANT
            metadata.setDocumentType(DocumentType.NDA);
            metadata.setDocumentPath(filePath.toString());

            metadata.setValidUntil(
                    validUntil.atTime(LocalTime.MAX)
            );


            documentMetadataRepository.save(metadata);



            return document;

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save signed NDA file",
                    exception
            );
        }
    }





//       public boolean isNdaRequired(Visitor visitor) {
//
//        LocalDateTime validity = visitor.getValidity();
//
//        if (validity == null) {
//            return true;
//        }
//
//        return validity.isBefore(LocalDateTime.now());
//    }

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
        return validUntil.isBefore(LocalDateTime.now());
    }
    @Transactional(readOnly = true)
    public DocumentMetadata getLatestNda(String visitorId) {

        // 1. Verify visitor exists
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );

        // 2. Check whether NDA is currently valid
        if (isNdaRequired(visitor)) {
            throw new ResourceNotFoundException(
                    "No valid NDA found for visitor: " + visitorId
            );
        }

        // 3. Find latest NDA only
        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No document found for visitor: " + visitorId
                        )
                );

        return documentMetadataRepository
                .findTopByDocumentIdAndDocumentTypeOrderByCreatedAtDesc(
                        document.getId(),
                        DocumentType.NDA
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No valid NDA found for visitor: " + visitorId
                        )
                );
    }

    @Transactional(readOnly = true)
    public DocumentMetadata getLatestVisitorPhoto(String visitorId) {

        return documentMetadataRepository
                .findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
                        visitorId,
                        DocumentType.VISITOR_PHOTO
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor photo not found for visitor: " + visitorId
                        )
                );
    }

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

    public DocumentMetadata getValidNda(Visitor visitor) {

        if (visitor == null || isNdaRequired(visitor)) {
            return null;
        }

        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitor.getId())
                .orElse(null);

        if (document == null) {
            return null;
        }

        return documentMetadataRepository
                .findTopByDocumentIdAndDocumentTypeOrderByCreatedAtDesc(
                        document.getId(),
                        DocumentType.NDA
                )
                .orElse(null);
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



    /*
     * ============================================================
     * IDENTITY PROOF
     * ============================================================
     *
     * Current flow:
     *
     * Raw Aadhaar/PAN/Passport
     *          ↓
     * ProofValidationService
     *          ↓
     * HmacBlindIndexService
     *          ↓
     * HMAC-SHA-256 blind index
     *          ↓
     * vms_document
     *
     * The raw proof number is NOT stored in the database.
     */

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

        /*
         * Aadhaar
         */
        if (aadharNumber != null && !aadharNumber.isBlank()) {

            document.setAadharNumber(
                    hmacBlindIndexService.generateBlindIndex(
                            ProofType.AADHAAR,
                            aadharNumber
                    )
            );
        }

        /*
         * PAN
         */
        if (panNumber != null && !panNumber.isBlank()) {

            document.setPanNumber(
                    hmacBlindIndexService.generateBlindIndex(
                            ProofType.PAN,
                            panNumber
                    )
            );
        }

        /*
         * Passport
         */
        if (passportNumber != null && !passportNumber.isBlank()) {

            document.setPassportNumber(
                    hmacBlindIndexService.generateBlindIndex(
                            ProofType.PASSPORT,
                            passportNumber
                    )
            );
        }

        return documentRepository.save(document);
    }

    public Document getLatestIdentityDocument(String visitorId) {

        return documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Identity document not found for visitor: " + visitorId
                        )
                );
    }

    public boolean verifyIdentityProof(
            Visitor visitor,
            ProofType proofType,
            String proofNumber
    ) {

        if (visitor == null) {
            throw new IllegalArgumentException(
                    "Visitor is required"
            );
        }

        if (proofType == null) {
            throw new IllegalArgumentException(
                    "Proof type is required"
            );
        }

        if (proofNumber == null || proofNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Proof number is required"
            );
        }

        String blindIndex =
                hmacBlindIndexService.generateBlindIndex(
                        proofType,
                        proofNumber
                );

        return switch (proofType) {

            case AADHAAR ->
                    documentRepository
                            .existsByVisitorIdAndAadharNumber(
                                    visitor.getId(),
                                    blindIndex
                            );

            case PAN ->
                    documentRepository
                            .existsByVisitorIdAndPanNumber(
                                    visitor.getId(),
                                    blindIndex
                            );

            case PASSPORT ->
                    documentRepository
                            .existsByVisitorIdAndPassportNumber(
                                    visitor.getId(),
                                    blindIndex
                            );
        };
    }


    /*
     * ============================================================
     * OLD AES ENCRYPTION FLOW
     * ============================================================
     *
     * Kept for future reference.
     *
     * IMPORTANT:
     * Do NOT use this flow for the current vms_document
     * identity-proof columns.
     *
     * Current columns contain HMAC blind indexes.
     */

    /*
    private final AesEncryptionService aesEncryptionService;

    @Transactional
    public Document saveIdentityProofsUsingAes(
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

    public IdentityProofResponse getIdentityProofsUsingAes(
            String visitorId
    ) {

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
    */




    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments(
            String visitorId
    ) {

        visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );

        return documentRepository
                .findByVisitorId(visitorId)
                .stream()
                .flatMap(document ->
                        documentMetadataRepository
                                .findByDocumentId(document.getId())
                                .stream()
                )
                .filter(metadata ->
                        metadata.getDocumentType() != DocumentType.NDA
                                && metadata.getDocumentType() != DocumentType.VISITOR_PHOTO
                )
                .map(metadata ->
                        new DocumentResponse(
                                metadata.getId(),
                                metadata.getDocumentPath(),

                                // Created At
                                metadata.getCreatedAt() != null
                                        ? metadata.getCreatedAt().toString()
                                        : null,

                                // Valid From - not applicable
                                null,

                                // Valid Until - not applicable
                                null,

                                // Overwritten By - not applicable
                                null
                        )
                )
                .toList();
    }


    @Transactional
    public List<DocumentMetadata> uploadProofDocuments(
            String visitorId,
            List<MultipartFile> files
    ) {


        if (visitorId == null || visitorId.isBlank()) {
            throw new IllegalArgumentException(
                    "Visitor ID is required"
            );
        }



        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one proof document is required"
            );
        }


        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );



        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found for visitor: "
                                        + visitorId
                        )
                );



        List<DocumentMetadata> existingDocuments =
                documentMetadataRepository
                        .findByDocumentId(document.getId());

//        if (!existingDocuments.isEmpty()) {
//            return existingDocuments;
//        }


        final long MAX_FILE_SIZE =
                5 * 1024 * 1024; // 5 MB

        for (MultipartFile file : files) {

            if (file == null || file.isEmpty()) {

                throw new IllegalArgumentException(
                        "Proof document cannot be empty"
                );
            }

            if (file.getSize() > MAX_FILE_SIZE) {

                throw new IllegalArgumentException(
                        "Proof document '"
                                + file.getOriginalFilename()
                                + "' exceeds the maximum size of 5 MB"
                );
            }
        }


        Path visitorDirectory =
                Paths.get(
                        documentUploadDir,
                        visitorId
                );


        try {

            Files.createDirectories(visitorDirectory);


            List<DocumentMetadata> uploadedDocuments =
                    new java.util.ArrayList<>();


            for (MultipartFile file : files) {

                String metadataId =
                        idGeneratorService.generateId(
                                "DOCUMENT_METADATA",
                                "DMD"
                        );



                String originalFileName =
                        file.getOriginalFilename();

                String extension = "";

                if (originalFileName != null
                        && originalFileName.contains(".")) {

                    extension =
                            originalFileName.substring(
                                    originalFileName.lastIndexOf(".")
                            );
                }


                String storedFileName =
                        metadataId + extension;


                Path filePath =
                        visitorDirectory.resolve(
                                storedFileName
                        );


                Files.write(
                        filePath,
                        file.getBytes()
                );


                DocumentMetadata metadata =
                        new DocumentMetadata();

                metadata.setId(metadataId);

                metadata.setDocument(
                        document
                );

                metadata.setDocumentPath(
                        filePath.toString()
                );


                DocumentMetadata savedMetadata =
                        documentMetadataRepository.save(
                                metadata
                        );


                uploadedDocuments.add(
                        savedMetadata
                );
            }


            return uploadedDocuments;


        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save proof documents",
                    exception
            );
        }
    }



//    @Transactional(readOnly = true)
//    public List<DocumentResponse> getAllNdas(String visitorId) {
//
//        Visitor visitor = visitorRepository.findById(visitorId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "Visitor not found: " + visitorId
//                        )
//                );
//
//        Document document = documentRepository
//                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
//                .orElseThrow(() ->
//                        new ResourceNotFoundException(
//                                "No document found for visitor: " + visitorId
//                        )
//                );
//
//        return documentMetadataRepository
//                .findByDocumentIdAndDocumentType(
//                        document.getId(),
//                        DocumentType.NDA
//                )
//                .stream()
//                .map(metadata ->
//                        new DocumentResponse(
//                                metadata.getId(),
//                                metadata.getDocumentPath(),
//                                metadata.getCreatedAt() != null
//                                        ? metadata.getCreatedAt().toString()
//                                        : null,
//                                metadata.getValidUntil() != null
//                                        ? metadata.getValidUntil().toString()
//                                        : null
//                        )
//                )
//                .toList();
//    }


    @Transactional(readOnly = true)
    public DocumentMetadata getDocumentMetadata(String documentId) {

        return documentMetadataRepository
                .findById(documentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found: " + documentId
                        )
                );
    }



    @Transactional
    public void deleteDocument(String metadataId) {

        DocumentMetadata metadata =
                documentMetadataRepository.findById(metadataId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Document not found: " + metadataId
                                )
                        );

        String documentPath = metadata.getDocumentPath();
        if (documentPath != null && !documentPath.isBlank()) {

            try {
                Path filePath = Paths.get(documentPath);

                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }

            } catch (IOException exception) {
                throw new RuntimeException(
                        "Failed to delete document file: " + metadataId,
                        exception
                );
            }
        }
        documentMetadataRepository.delete(metadata);
    }




//    @Transactional
//    public DocumentMetadata updateNdaValidity(
//            String metadataId,
//            LocalDate validUntil
//    ) {
//
//        if (validUntil == null) {
//            throw new IllegalArgumentException(
//                    "NDA valid until date is required"
//            );
//        }
//
//        if (validUntil.isBefore(LocalDate.now())) {
//            throw new IllegalArgumentException(
//                    "NDA valid until date cannot be in the past"
//            );
//        }
//
//        DocumentMetadata metadata =
//                documentMetadataRepository.findById(metadataId)
//                        .orElseThrow(() ->
//                                new ResourceNotFoundException(
//                                        "Document not found: " + metadataId
//                                )
//                        );
//
//        if (metadata.getDocumentType() != DocumentType.NDA) {
//            throw new BusinessRuleException(
//                    "Only NDA documents can have their validity updated"
//            );
//        }
//
//        metadata.setValidUntil(
//                validUntil.atTime(LocalTime.MAX)
//        );
//
//        return documentMetadataRepository.save(metadata);
//    }
    }

