package com.adminvisitor.repository;

import com.adminvisitor.entity.Document;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository
        extends JpaRepository<Document, String> {

    @EntityGraph(attributePaths = {"visitor"})
    Optional<Document> findTopByVisitorIdOrderByCreatedAtDesc(
            String visitorId
    );

    boolean existsByVisitorIdAndAadharNumber(
            String visitorId,
            String aadharNumber
    );

    boolean existsByVisitorIdAndPanNumber(
            String visitorId,
            String panNumber
    );

    boolean existsByVisitorIdAndPassportNumber(
            String visitorId,
            String passportNumber
    );

    @EntityGraph(attributePaths = {"visitor"})
    List<Document> findByVisitorId(String visitorId);

    boolean existsByVisitor_IdAndNationalityIsNotNull(String visitorId);
}