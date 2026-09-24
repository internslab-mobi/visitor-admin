package com.adminvisitor.repository;

import com.adminvisitor.entity.Department;
import com.adminvisitor.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, String> {
//    Optional<Document> findTopByVisitorIdAndNdaDocumentIsNotNullOrderByCreatedAtDesc(
//            String visitorId
//    );
}