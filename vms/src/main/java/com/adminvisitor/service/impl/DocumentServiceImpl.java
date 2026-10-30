package com.adminvisitor.service.impl;

import com.adminvisitor.dto.responsedto.DocumentResponse;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.mapper.DocumentMapper;
import com.adminvisitor.repository.VisitRepository;
import com.adminvisitor.service.DocumentService;
import com.adminvisitor.entity.Document;
import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.repository.DocumentMetadataRepository;
import com.adminvisitor.repository.DocumentRepository;
import com.adminvisitor.repository.VisitorRepository;
import com.adminvisitor.service.IdGeneratorService;
import com.adminvisitor.service.ProofSecurityService;
import com.adminvisitor.service.DocumentStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
/**
 * Service implementation for managing visitor documents and identity proofs.
 *
 * <p>Handles visitor photo uploads, identity proof storage and verification,
 * proof document uploads, document retrieval, and document deletion.</p>
 *
 * <p>Coordinates document persistence, metadata management, file storage,
 * identifier generation, and secure identity proof indexing.</p>
 *
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final VisitorRepository visitorRepository;
    private final IdGeneratorService idGeneratorService;
    private final VisitRepository visitRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final DocumentMapper documentMapper;
    private final DocumentStorageService documentStorageService;
    private final ProofSecurityService proofSecurityService;

    /**
     * Maximum permitted size for an uploaded document, set to 5 MB.
     */
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    /**
     * Configured directory for storing visitor proof documents.
     */
    @Value("${vms.document.upload-dir}")
    private String documentUploadDir;


    /**
     * Configured directory for storing visitor photos.
     */
    @Value("${vms.document.photo-upload-dir}")
    private String photoUploadDir;


    /**
     * Uploads and saves a photo associated with a visitor and a specific visit.
     *
     * <p>Validates the visitor, visit ownership, file size, and image content
     * type before storing the file and saving its metadata.</p>
     *
     * @param visitorId the unique identifier of the visitor
     * @param visitId the unique identifier of the visit
     * @param file the visitor photo to upload
     * @return the saved visitor photo metadata
     * @throws IllegalArgumentException if the file is missing, is not an image,
     *                                  or the visit does not belong to the visitor
     * @throws ResourceNotFoundException if the visitor, visit, or associated
     *                                   identity document cannot be found
     * @throws RuntimeException if an I/O error occurs while saving the photo
     */
    @Transactional
    @Override
    public DocumentMetadata uploadVisitorPhoto(
            String visitorId,
            String visitId,
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Visitor photo is required"
            );
        }

        if (!visitorRepository.existsById(visitorId)) {
            throw new ResourceNotFoundException(
                    "Visitor not found: " + visitorId
            );
        }

        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visit not found: " + visitId
                        )
                );

        if (!visit.getVisitor().getId().equals(visitorId)) {
            throw new IllegalArgumentException(
                    "The visit does not belong to the specified visitor"
            );
        }

        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found for visitor: " + visitorId
                        )
                );




        validateFile(file, "Visitor photo");

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
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

            Path filePath = documentStorageService.store(
                    file,
                    photoUploadDir,
                    visitorId,
                    metadataId
            );


            DocumentMetadata metadata =
                    documentMapper.toVisitorPhotoMetadata(
                            metadataId,
                            document,
                            visit,
                            filePath.toString()
                    );

            DocumentMetadata savedMetadata =
                    documentMetadataRepository.save(metadata);

            log.info(
                    "VISITOR_PHOTO_UPLOADED metadataId={} visitorId={} visitId={} fileSizeBytes={}",
                    savedMetadata.getId(),
                    visitorId,
                    visitId,
                    file.getSize()
            );

            return savedMetadata;

        } catch (IOException exception) {

            throw new RuntimeException(
                    "Failed to save visitor photo",
                    exception
            );
        }
    }


    /**
     * Retrieves the latest photo metadata associated with a visitor.
     *
     * @param visitorId the unique identifier of the visitor
     * @return the latest visitor photo metadata
     * @throws ResourceNotFoundException if no visitor photo is found
     */

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

    /**
     * Saves a visitor's identity proof details.
     *
     * <p>Generates a document identifier, creates blind indexes for the
     * supplied Aadhaar, PAN, and passport numbers when present, maps the
     * details to a document entity, and persists the entity.</p>
     *
     * @param visitor the visitor associated with the identity proofs
     * @param nationality the visitor's nationality category
     * @param aadharNumber the Aadhaar number, if provided
     * @param panNumber the PAN number, if provided
     * @param passportNumber the passport number, if provided
     * @return the saved identity proof document
     */
    @Transactional
    public Document saveIdentityProofs(
            Visitor visitor,
            Nationality nationality,
            String aadharNumber,
            String panNumber,
            String passportNumber
    ) {

        String documentId = idGeneratorService.generateId(
                "DOCUMENT",
                "doc"
        );

        String aadhaarIndex = hasValue(aadharNumber)
                ? proofSecurityService.generateBlindIndex(
                ProofType.AADHAAR, aadharNumber)
                : null;

        String panIndex = hasValue(panNumber)
                ? proofSecurityService.generateBlindIndex(
                ProofType.PAN, panNumber)
                : null;

        String passportIndex = hasValue(passportNumber)
                ? proofSecurityService.generateBlindIndex(
                ProofType.PASSPORT, passportNumber)
                : null;

        Document document = documentMapper.toIdentityProofDocument(
                documentId,
                visitor,
                nationality,
                aadhaarIndex,
                panIndex,
                passportIndex
        );

        Document savedDocument = documentRepository.save(document);

        log.info(
                "IDENTITY_PROOFS_SAVED documentId={} visitorId={} nationality={}",
                savedDocument.getId(),
                visitor.getId(),
                nationality
        );

        return savedDocument;
    }
    /**
     * Checks whether a string contains a non-null, non-blank value.
     *
     * @param value the string to check
     * @return {@code true} if the value is not null or blank; otherwise,
     *         {@code false}
     */
    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
    }


    /**
     * Retrieves the most recently created identity document for a visitor.
     *
     * @param visitorId the unique identifier of the visitor
     * @return the latest identity document
     * @throws ResourceNotFoundException if no identity document is found
     */
    public Document getLatestIdentityDocument(String visitorId) {

        return documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Identity document not found for visitor: " + visitorId
                        )
                );
    }


    /**
     * Verifies an identity proof by comparing its generated blind index
     * with the stored index for the specified visitor and proof type.
     *
     * @param visitor the visitor whose identity proof is being verified
     * @param proofType the type of identity proof to verify
     * @param proofNumber the identity proof number supplied for verification
     * @return {@code true} if a matching identity proof exists; otherwise,
     *         {@code false}
     * @throws IllegalArgumentException if the visitor, proof type, or proof
     *                                  number is null or invalid
     */
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

        String blindIndex = proofSecurityService.generateBlindIndex(
                proofType,
                proofNumber
        );

        return switch (proofType) {

            case AADHAAR -> documentRepository.existsByVisitorIdAndAadharNumber(
                    visitor.getId(), blindIndex
            );

            case PAN -> documentRepository.existsByVisitorIdAndPanNumber(
                    visitor.getId(), blindIndex
            );

            case PASSPORT -> documentRepository.existsByVisitorIdAndPassportNumber(
                    visitor.getId(), blindIndex
            );
        };
    }

    /**
     * Retrieves the latest visitor photo metadata associated with a visit.
     *
     * @param visitId the unique identifier of the visit
     * @return the latest photo metadata for the visit
     * @throws ResourceNotFoundException if no photo is found for the visit
     */
    @Override
    public DocumentMetadata getVisitPhoto(String visitId) {

        return documentMetadataRepository
                .findTopByVisit_IdAndDocumentTypeOrderByCreatedAtDesc(
                        visitId,
                        DocumentType.VISITOR_PHOTO
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor photo not found for visit: " + visitId
                        )
                );
    }

    /**
     * Retrieves the document details associated with a visitor.
     *
     * <p>Validates that the visitor exists, retrieves document metadata
     * records without a specific document type, and maps them to response DTOs.</p>
     *
     * @param visitorId the unique identifier of the visitor
     * @return the list of document response details for the visitor
     * @throws ResourceNotFoundException if the visitor does not exist
     */
    public List<DocumentResponse> getAllDocuments(
            String visitorId
    ) {
        visitorRepository.findById(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Visitor not found: " + visitorId
                        )
                );
        List<DocumentResponse> documents =
                documentMetadataRepository
                        .findByDocument_Visitor_IdAndDocumentTypeIsNull(visitorId)
                        .stream()
                        .map(documentMapper::toResponse)
                        .toList();

        log.debug(
                "VISITOR_DOCUMENTS_RETRIEVED visitorId={} documentCount={}",
                visitorId,
                documents.size()
        );

        return documents;
    }

    /**
     * Uploads one or more proof documents for a visitor.
     *
     * <p>Validates the visitor identifier, uploaded files, and visitor
     * existence before storing each file and saving its metadata.</p>
     *
     * @param visitorId the unique identifier of the visitor
     * @param files the proof document files to upload
     * @return the list of saved proof document metadata records
     * @throws IllegalArgumentException if the visitor identifier is missing
     *                                  or no files are provided
     * @throws ResourceNotFoundException if the visitor or associated
     *                                   identity document cannot be found
     * @throws RuntimeException if an I/O error occurs while saving the files
     */
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

        if (!visitorRepository.existsById(visitorId)) {
            throw new ResourceNotFoundException(
                    "Visitor not found: " + visitorId
            );
        }

        Document document = documentRepository
                .findTopByVisitorIdOrderByCreatedAtDesc(visitorId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Document not found for visitor: " + visitorId
                        )
                );


        files.forEach(file -> validateFile(file, "Proof document"));

        List<DocumentMetadata> uploadedDocuments =
                new ArrayList<>(files.size());

        try {
            for (MultipartFile file : files) {
                String metadataId = idGeneratorService.generateId(
                        "DOCUMENT_METADATA",
                        "DMD"
                );

                Path filePath = documentStorageService.store(
                        file,
                        documentUploadDir,
                        visitorId,
                        metadataId
                );

                DocumentMetadata metadata =
                        documentMapper.toProofDocumentMetadata(
                                metadataId,
                                document,
                                filePath.toString()
                        );

                uploadedDocuments.add(
                        documentMetadataRepository.save(metadata)
                );
            }

            log.info(
                    "PROOF_DOCUMENTS_UPLOADED visitorId={} documentId={} documentCount={}",
                    visitorId,
                    document.getId(),
                    uploadedDocuments.size()
            );

            return uploadedDocuments;

        } catch (IOException exception) {
            throw new RuntimeException(
                    "Failed to save proof documents",
                    exception
            );
        }
    }

    /**
     * Validates an uploaded file for presence and maximum permitted size.
     *
     * @param file the uploaded file to validate
     * @param fileDescription the description used in validation error messages
     * @throws IllegalArgumentException if the file is null, empty, or larger
     * than the maximum permitted size
     */
    private void validateFile(MultipartFile file, String fileDescription) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    fileDescription + " is required"
            );
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    fileDescription + " cannot exceed 5 MB"
            );
        }
    }

    /**
     * Retrieves document metadata by its unique identifier.
     *
     * @param documentId the unique identifier of the document metadata record
     * @return the requested document metadata
     * @throws ResourceNotFoundException if no document metadata is found
     */
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
    /**
     * Deletes a document and its associated file, if the file exists.
     *
     * <p>Retrieves the document metadata, deletes the file from storage
     * when a path is available, and then deletes the metadata record.</p>
     *
     * @param metadataId the unique identifier of the document metadata record
     * @throws ResourceNotFoundException if the document metadata is not found
     * @throws RuntimeException if an I/O error occurs while deleting the file
     */
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

        log.info(
                "DOCUMENT_DELETED metadataId={} documentId={}",
                metadata.getId(),
                metadata.getDocument().getId()
        );
    }

}


