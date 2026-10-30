package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.DepartmentCreateRequest;
import com.adminvisitor.dto.responsedto.DepartmentResponse;
import com.adminvisitor.service.DepartmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing department records.
 *
 * <p>Provides endpoints to retrieve existing departments and create
 * new departments.</p>
 *
 * @author Nethra
 */
@Tag(
        name = "Department Management",
        description = "APIs for retrieving and creating department records"
)
@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Retrieves all department records.
     *
     * @return a list of department records with HTTP status 200
     */
    @Operation(
            summary = "Get all departments",
            description = "Retrieves the department records available in the system."
    )
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DepartmentResponse>> getDepartments() {

        try {
            List<DepartmentResponse> departments =
                    departmentService.getDepartments();

            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(departments);

        } catch (IllegalStateException ex) {
            log.error("Failed to retrieve department records.", ex);
            throw ex;
        }
    }

    /**
     * Creates a new department using the supplied department details.
     *
     * @param request validated department creation details
     * @return the created department record with HTTP status 201
     */
    @Operation(
            summary = "Create a department",
            description = "Creates a new department using the details " +
                    "provided in the JSON request body."
    )
    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<DepartmentResponse> createDepartment(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Department details required to create a new department",
                    required = true
            )
            @Valid
            @RequestBody
            DepartmentCreateRequest request
    ) {

        try {
            DepartmentResponse response =
                    departmentService.createDepartment(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid department creation request: {}",
                    ex.getMessage(), ex);
            throw ex;
        }
    }
}
