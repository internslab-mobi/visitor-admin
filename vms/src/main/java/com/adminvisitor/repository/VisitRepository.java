package com.adminvisitor.repository;

import com.adminvisitor.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
public interface VisitRepository
        extends JpaRepository<Visit, Long>,
        JpaSpecificationExecutor<Visit> {

        }