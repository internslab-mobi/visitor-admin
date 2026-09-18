package com.adminvisitor.repository;

import com.adminvisitor.entity.VisitBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VisitBadgeRepository
        extends JpaRepository<VisitBadge, String> {

    Optional<VisitBadge> findByVisit_Id(String visitId);
    Optional<VisitBadge> findByQrContainingToken(String qrContainingToken);
}