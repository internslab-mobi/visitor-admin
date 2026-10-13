package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, String> {

    Optional<Employee> findByEmail(String email);
}
