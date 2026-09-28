package com.adminvisitor.repository;

import com.adminvisitor.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository
        extends JpaRepository<Document, String> {

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

    List<Document> findByVisitorId(String visitorId);
}