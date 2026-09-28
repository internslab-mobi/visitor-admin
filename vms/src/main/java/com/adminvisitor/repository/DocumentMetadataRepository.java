package com.adminvisitor.repository;

import com.adminvisitor.entity.DocumentMetadata;
import com.adminvisitor.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentMetadataRepository
        extends JpaRepository<DocumentMetadata, String> {

    List<DocumentMetadata> findByDocumentId(String documentId);

    Optional<DocumentMetadata> findTopByDocumentIdOrderByCreatedAtDesc(
            String documentId
    );

    Optional<DocumentMetadata>
    findTopByDocument_Visitor_IdAndDocumentTypeOrderByCreatedAtDesc(
            String visitorId,
            DocumentType documentType
    );
}