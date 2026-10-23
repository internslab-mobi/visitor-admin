package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.EmployeeCreateRequest;
import com.adminvisitor.dto.responsedto.EmployeeResponse;
import com.adminvisitor.entity.Department;
import com.adminvisitor.entity.Employee;
import com.adminvisitor.exception.ResourceAlreadyExistsException;
import com.adminvisitor.exception.ResourceNotFoundException;
import com.adminvisitor.repository.DepartmentRepository;
import com.adminvisitor.repository.EmployeeRepository;
import com.adminvisitor.service.EmployeeService;
import com.adminvisitor.service.IdGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
/**
 * Implements employee creation and retrieval operations.
 *
 * @author Nethra
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final IdGeneratorService idGeneratorService;

    /**
     * Creates an employee after checking email uniqueness and validating the department.
     * Generates an employee ID, assigns the department, and sets the initial status to ACTIVE.
     *
     * @param request the employee details to create
     * @return the response containing the saved employee and department details
     * @throws ResourceAlreadyExistsException if the employee email already exists
     * @throws ResourceNotFoundException if the specified department does not exist
     */
    @Transactional
    public EmployeeResponse createEmployee(
            EmployeeCreateRequest request
    ) {

        String email = request.email().trim();

        if (employeeRepository.existsByEmail(email)) {
            throw new ResourceAlreadyExistsException(
                    "Employee email already exists"
            );
        }

        Department department =
                departmentRepository.findById(
                        request.departmentId().trim()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
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
                request.firstName().trim()
        );

        employee.setLastName(
                request.lastName().trim()
        );

        employee.setEmail(email);

        employee.setMobileNumber(
                request.mobileNumber().trim()
        );

        employee.setDepartment(department);

        employee.setDesignation(
                request.designation().trim()
        );

        employee.setStatus("ACTIVE");

        Employee savedEmployee =
                employeeRepository.save(employee);

        log.info(
                "EMPLOYEE_CREATED employeeId={} departmentId={} status={}",
                savedEmployee.getId(),
                savedEmployee.getDepartment().getId(),
                savedEmployee.getStatus()
        );

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

    /**
     * Retrieves all employees and converts them into response objects,
     * including their associated department details.
     *
     * @return a list of employee responses, or an empty list if no employees exist
     */
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getEmployees() {

        List<EmployeeResponse> employees =
                employeeRepository.findAll()
                        .stream()
                        .map(employee -> new EmployeeResponse(
                                employee.getId(),
                                employee.getFirstName(),
                                employee.getLastName(),
                                employee.getEmail(),
                                employee.getMobileNumber(),
                                employee.getDepartment().getId(),
                                employee.getDepartment().getDepartmentName(),
                                employee.getDesignation(),
                                employee.getStatus()
                        ))
                        .toList();

        log.debug(
                "EMPLOYEES_RETRIEVED count={}",
                employees.size()
        );

        return employees;
    }
}