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

    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    @Value("${vms.document.upload-dir}")
    private String documentUploadDir;

    @Value("${vms.document.photo-upload-dir}")
    private String photoUploadDir;

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

//            DocumentMetadata metadata = new DocumentMetadata();
//
//            metadata.setId(metadataId);
//            metadata.setDocument(document);
//            metadata.setVisit(visit);
//            metadata.setDocumentType(DocumentType.VISITOR_PHOTO);
//            metadata.setDocumentPath(filePath.toString());

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

    private boolean hasValue(String value) {
        return value != null && !value.isBlank();
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

//        List<DocumentMetadata> existingDocuments =
//                documentMetadataRepository
//                        .findByDocumentId(document.getId());
//
//        final long MAX_FILE_SIZE = 5 * 1024 * 1024;
//
//        for (MultipartFile file : files) {
//
//            if (file == null || file.isEmpty()) {
//
//                throw new IllegalArgumentException(
//                        "Proof document cannot be empty"
//                );
//            }
//
//            if (file.getSize() > MAX_FILE_SIZE) {
//
//                throw new IllegalArgumentException(
//                        "Proof document '"
//                                + file.getOriginalFilename()
//                                + "' exceeds the maximum size of 5 MB"
//                );
//            }
//        }
//
//        Path visitorDirectory =
//                Paths.get(
//                        documentUploadDir,
//                        visitorId
//                );
//
//        try {
//
//            List<DocumentMetadata> uploadedDocuments =
//                    new java.util.ArrayList<>();
//
//            for (MultipartFile file : files) {
//
//                String metadataId = idGeneratorService.generateId(
//                        "DOCUMENT_METADATA",
//                        "DMD"
//                );
//
//                Path filePath = documentStorageService.store(
//                        file,
//                        documentUploadDir,
//                        visitorId,
//                        metadataId
//                );
//
//                DocumentMetadata metadata = new DocumentMetadata();
//
//                metadata.setId(metadataId);
//                metadata.setDocument(document);
//                metadata.setDocumentPath(filePath.toString());
//
//                DocumentMetadata savedMetadata =
//                        documentMetadataRepository.save(metadata);
//
//                uploadedDocuments.add(savedMetadata);
//            }
//
//            log.info(
//                    "PROOF_DOCUMENTS_UPLOADED visitorId={} documentId={} documentCount={}",
//                    visitorId,
//                    document.getId(),
//                    uploadedDocuments.size()
//            );
//
//            return uploadedDocuments;
//
//        } catch (IOException exception) {
//
//            throw new RuntimeException(
//                    "Failed to save proof documents",
//                    exception
//            );
//        }

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

//    private void validateProofDocument(MultipartFile file) {
//        if (file == null || file.isEmpty()) {
//            throw new IllegalArgumentException(
//                    "Proof document cannot be empty"
//            );
//        }
//
//        if (file.getSize() > 5L * 1024 * 1024) {
//            throw new IllegalArgumentException(
//                    "Proof document '" + file.getOriginalFilename()
//                            + "' exceeds the maximum size of 5 MB"
//            );
//        }

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

        log.info(
                "DOCUMENT_DELETED metadataId={} documentId={}",
                metadata.getId(),
                metadata.getDocument().getId()
        );
    }

}


