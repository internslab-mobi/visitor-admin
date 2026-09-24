package com.adminvisitor.service;

import com.adminvisitor.dto.responsedto.IdentityProofResponse;
// import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final VisitorRepository visitorRepository;
    private final IdGeneratorService idGeneratorService;

    /*
     * Active service for generating HMAC-SHA-256 blind indexes.
     */
    private final HmacBlindIndexService hmacBlindIndexService;

    /*
     * ============================================================
     * OLD NDA / FILE HANDLING
     * ============================================================
     *
     * NDA handling belongs to another module/owner for now.
     * The old implementation is intentionally kept commented
     * so it can be restored/reworked later.
     */

    /*
    @Value("${vms.document.nda-upload-dir}")
    private String ndaUploadDir;

    private final VisitRepository visitRepository;

    public Document uploadSignedNda(
            String visitorId,
            MultipartFile file
    ) {

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

        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
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

        if (!isNdaRequired(visitor)) {
            throw new BadgeAlreadyExistsException(
                    "Valid NDA already exists for visitor: " + visitorId
            );
        }

        String documentId =
                idGeneratorService.generateId(
                        "DOCUMENT",
                        "doc"
                );

        try {

            Path visitorDirectory = Paths.get(
                    ndaUploadDir,
                    visitorId
            );

            Files.createDirectories(visitorDirectory);

            String fileName = documentId + ".pdf";

            Path filePath = visitorDirectory.resolve(fileName);

            Files.write(
                    filePath,
                    file.getBytes()
            );

            Document document = new Document();

            document.setId(documentId);
            document.setVisitor(visitor);

            // Old NDA path storage
            // document.setNdaDocument(filePath.toString());

            Document savedDocument =
                    documentRepository.save(document);

            visitor.setValidity(
                    LocalDateTime.now().plusMonths(6)
            );

            visitorRepository.save(visitor);

            return savedDocument;

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save signed NDA file",
                    exception
            );
        }
    }

    public boolean isNdaRequired(Visitor visitor) {

        LocalDateTime validity =
                visitor.getValidity();

        if (validity == null) {
            return true;
        }

        if (validity.isBefore(LocalDateTime.now())) {
            return true;
        }

        return documentRepository
                .findTopByVisitorIdAndNdaDocumentIsNotNullOrderByCreatedAtDesc(
                        visitor.getId()
                )
                .isEmpty();
    }

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

        LocalDateTime validity =
                visitor.getValidity();

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

        Path filePath = Paths.get(
                document.getNdaDocument()
        );

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "NDA file not found"
            );
        }

        return new FileSystemResource(filePath);
    }
    */


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


    /*
     * ============================================================
     * OLD DOCUMENT/NDA LISTING
     * ============================================================
     *
     * Currently disabled because document paths are now handled
     * separately through vms_document_metadata.
     */

    /*
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

        return documentRepository.findByVisitorId(visitorId)
                .stream()
                .map(document -> new DocumentResponse(
                        document.getNdaDocument(),
                        document.getCreatedAt().toString(),
                        document.getUpdatedAt().toString()
                ))
                .toList();
    }
    */
}