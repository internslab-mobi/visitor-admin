package com.adminvisitor.repository;

import com.adminvisitor.entity.Vendor;
import com.adminvisitor.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, String> {

    Optional<Vendor> findByVisitor(Visitor visitor);

    Optional<Vendor> findByVisitorId(String visitorId);
}