package com.adminvisitor.repository;

import com.adminvisitor.entity.Visit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitRepository
        extends JpaRepository<Visit, Long>,
        JpaSpecificationExecutor<Visit> {

//    @Override
//    @EntityGraph(attributePaths = {"visitor"})
//    List<Visit> findAll(
//            org.springframework.data.jpa.domain.Specification<Visit> specification,
//            Sort sort
//    );
}