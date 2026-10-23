package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for managing persistent {@link Employee} entities.
 * <p>
 * Provides CRUD operations and lookup methods against the {@code vms_employee} table.
 * </p>
 *
 * @author Logeshwaran
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, String> {
}
