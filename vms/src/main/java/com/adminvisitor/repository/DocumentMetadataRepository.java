package com.adminvisitor.repository;

import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.enums.NdaStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DocumentMetadataRepository
        extends JpaRepository<DocumentMetadata, String> {

    @EntityGraph(attributePaths = {"document", "document.visitor"})
    List<DocumentMetadata> findByDocumentId(String documentId);

    Optional<DocumentMetadata> findTopByDocumentIdOrderByCreatedAtDesc(
            String documentId
    );

    Optional<DocumentMetadata>
    findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
            String visitorId,
            DocumentType documentType
    );

    Optional<DocumentMetadata>
    findTopByVisit_IdAndDocumentTypeOrderByCreatedAtDesc(
            String visitId,
            DocumentType documentType
    );

    // Get all files of a particular type
    List<DocumentMetadata> findByDocumentIdAndDocumentType(
            String documentId,
            DocumentType documentType
    );

    // Get latest file of a particular type
    Optional<DocumentMetadata>
    findTopByDocumentIdAndDocumentTypeOrderByCreatedAtDesc(
            String documentId,
            DocumentType documentType
    );


    List<DocumentMetadata>
    findByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
            String visitorId,
            DocumentType documentType
    );

    List<DocumentMetadata> findByDocumentIdIn(
            Collection<String> documentIds
    );

    List<DocumentMetadata>
    findByDocument_Visitor_IdAndDocumentTypeNotIn(
            String visitorId,
            List<DocumentType> documentTypes
    );


    @EntityGraph(attributePaths = "document")
    Optional<DocumentMetadata>
    findTopByDocument_Visitor_IdAndDocumentTypeAndOverwrittenByIsNullOrderByCreatedAtDesc(
            String visitorId,
            DocumentType documentType
    );

    List<DocumentMetadata>
    findByDocumentTypeAndStatusAndValidUntilLessThanEqual(
            DocumentType documentType,
            NdaStatus status,
            LocalDateTime now
    );
}