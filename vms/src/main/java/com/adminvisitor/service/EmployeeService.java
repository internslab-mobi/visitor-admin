package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.EmployeeCreateRequest;
import com.adminvisitor.dto.responsedto.EmployeeResponse;
import com.adminvisitor.entity.Department;
import com.adminvisitor.entity.Employee;
import com.adminvisitor.repository.DepartmentRepository;
import com.adminvisitor.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final IdGeneratorService idGeneratorService;

    @Transactional
    public EmployeeResponse createEmployee(
            EmployeeCreateRequest request
    ) {

        String email = request.getEmail().trim();

        if (employeeRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Employee email already exists"
            );
        }

        Department department =
                departmentRepository.findById(
                        request.getDepartmentId().trim()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Department not found"
                        )
                );

        Employee employee = new Employee();

        employee.setId(
                idGeneratorService.generateId(
                        "EMPLOYEE",
                        "EMP"
                )
        );

        employee.setFirstName(
                request.getFirstName().trim()
        );

        employee.setLastName(
                request.getLastName().trim()
        );

        employee.setEmail(email);

        employee.setMobileNumber(
                request.getMobileNumber().trim()
        );

        employee.setDepartment(department);

        employee.setDesignation(
                request.getDesignation().trim()
        );

        employee.setStatus("ACTIVE");

        Employee savedEmployee =
                employeeRepository.save(employee);

        return new EmployeeResponse(
                savedEmployee.getId(),
                savedEmployee.getFirstName(),
                savedEmployee.getLastName(),
                savedEmployee.getEmail(),
                savedEmployee.getMobileNumber(),
                savedEmployee.getDepartment().getId(),
                savedEmployee.getDepartment().getDepartmentName(),
                savedEmployee.getDesignation(),
                savedEmployee.getStatus()
        );
    }
}