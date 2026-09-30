package com.adminvisitor.repository;

import com.adminvisitor.entity.Department;
import com.adminvisitor.entity.Employee;
import com.adminvisitor.entity.Visit;
import com.adminvisitor.entity.VisitBadge;
import com.adminvisitor.entity.Visitor;
import com.adminvisitor.enums.BadgeStatus;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class VisitBadgeRepositoryTest {

    @Autowired
    private VisitBadgeRepository visitBadgeRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VisitorRepository visitorRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    private Visit visit;
    private VisitBadge visitBadge;

    @BeforeEach
    void setUp() {

        LocalDateTime now = LocalDateTime.now();

      
        // Department
      

        Department department = new Department();

        department.setId("DEP-001");
        department.setDepartmentCode("IT");
        department.setDepartmentName("Information Technology");
        department.setStatus("ACTIVE");

        departmentRepository.save(department);

      
        // Employee
      

        Employee employee = new Employee();

        employee.setId("EMP-001");
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setEmail("john.doe@test.com");
        employee.setMobileNumber("9876543210");
        employee.setDepartment(department);
        employee.setDesignation("Manager");
        employee.setStatus("ACTIVE");

        employeeRepository.save(employee);

      
        // Visitor
      

        Visitor visitor = new Visitor();

        visitor.setId("VTR-001");
        visitor.setFirstName("Anisha");
        visitor.setLastName("Test");
        visitor.setEmail("anisha.test@test.com");
        visitor.setMobileNumber("9876500000");
        visitor.setCompanyName("Test Company");

        // BaseEntity audit fields
        visitor.setCreatedAt(now);
        visitor.setUpdatedAt(now);
        visitor.setCreatedBy("TEST");
        visitor.setUpdatedBy("TEST");

        visitorRepository.save(visitor);

      
        // Visit
      

        visit = new Visit();

        visit.setId("VIS-001");
        visit.setVisitReference("VIS-REF-001");
        visit.setVisitor(visitor);
        visit.setVisitorType(VisitorType.VISITOR);
        visit.setRegistrationType(
                RegistrationType.PRE_REGISTRATION
        );
        visit.setPurpose("Meeting");
        visit.setHost(employee);

        visit.setExpectedArrivalAt(
                LocalDateTime.of(2026, 9, 30, 10, 0)
        );

        visit.setExpectedDepartureAt(
                LocalDateTime.of(2026, 9, 30, 12, 0)
        );

        visit.setStatus(VisitStatus.REGISTERED);

        // BaseEntity audit fields
        visit.setCreatedAt(now);
        visit.setUpdatedAt(now);
        visit.setCreatedBy("TEST");
        visit.setUpdatedBy("TEST");

        visitRepository.save(visit);

      
        // Visit Badge
      

        visitBadge = new VisitBadge();

        visitBadge.setId("VB-001");
        visitBadge.setVisit(visit);
        visitBadge.setQrContainingToken("test-qr-token-001");

        visitBadge.setIssuedAt(
                LocalDateTime.of(2026, 9, 30, 9, 30)
        );

        visitBadge.setValidUntil(
                LocalDateTime.of(2026, 9, 30, 23, 59)
        );

        visitBadge.setStatus(BadgeStatus.ACTIVE);

        // BaseEntity audit fields
        visitBadge.setCreatedAt(now);
        visitBadge.setUpdatedAt(now);
        visitBadge.setCreatedBy("TEST");
        visitBadge.setUpdatedBy("TEST");

        visitBadgeRepository.save(visitBadge);
    }

  
    // findByVisit_Id()
  

    @Test
    void findByVisit_Id_shouldReturnBadge() {

        Optional<VisitBadge> result =
                visitBadgeRepository.findByVisit_Id("VIS-001");

        assertTrue(result.isPresent());

        assertEquals(
                "VB-001",
                result.get().getId()
        );

        assertEquals(
                "VIS-001",
                result.get().getVisit().getId()
        );
    }

    @Test
    void findByVisit_Id_shouldReturnEmptyWhenVisitDoesNotExist() {

        Optional<VisitBadge> result =
                visitBadgeRepository.findByVisit_Id("VIS-999");

        assertTrue(result.isEmpty());
    }

  
    // findByQrContainingToken()
  

    @Test
    void findByQrContainingToken_shouldReturnBadge() {

        Optional<VisitBadge> result =
                visitBadgeRepository.findByQrContainingToken(
                        "test-qr-token-001"
                );

        assertTrue(result.isPresent());

        assertEquals(
                "VB-001",
                result.get().getId()
        );

        assertEquals(
                "test-qr-token-001",
                result.get().getQrContainingToken()
        );
    }

    @Test
    void findByQrContainingToken_shouldReturnEmptyWhenTokenDoesNotExist() {

        Optional<VisitBadge> result =
                visitBadgeRepository.findByQrContainingToken(
                        "non-existing-token"
                );

        assertTrue(result.isEmpty());
    }
}