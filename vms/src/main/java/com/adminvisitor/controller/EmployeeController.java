package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.EmployeeCreateRequest;
import com.adminvisitor.dto.responsedto.EmployeeResponse;
import com.adminvisitor.repository.EmployeeRepository;
import com.adminvisitor.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing employee records.
 *
 * <p>Provides endpoints to retrieve employee records and create
 * new employees.</p>
 *
 * @author Nethra
 */
@Tag(
        name = "Employee Management",
        description = "APIs for retrieving and creating employee records"
)
@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    /**
     * Retrieves all employee records.
     *
     * @return a list of employee records with HTTP status 200
     */
    @Operation(
            summary = "Get all employees",
            description = "Retrieves the employee records available in the system."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<EmployeeResponse>> getEmployees() {

        List<EmployeeResponse> employees =
                employeeService.getEmployees();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(employees);
    }

    /**
     * Creates a new employee using the supplied employee details.
     *
     * @param request validated employee creation details
     * @return the created employee record with HTTP status 201
     */
    @Operation(
            summary = "Create an employee",
            description = "Creates a new employee using the details " +
                    "provided in the JSON request body."
    )
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<EmployeeResponse> createEmployee(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Employee details required to create a new employee",
                    required = true
            )
            @Valid
            @RequestBody
            EmployeeCreateRequest request
    ) {

        EmployeeResponse response =
                employeeService.createEmployee(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}