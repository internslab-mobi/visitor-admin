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
}