package com.adminvisitor.repository;

import com.adminvisitor.entity.Vendor;
import com.adminvisitor.entity.Visitor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, String> {

    //Optional<Vendor> findByVisitor(Visitor visitor);

    @Override
    @EntityGraph(attributePaths = "visitor")
    List<Vendor> findAll();

    @Override
    @EntityGraph(attributePaths = "visitor")
    Optional<Vendor> findById(String id);
    Optional<Vendor> findByVisitorId(String visitorId);
}