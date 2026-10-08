package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.EmployeeCreateRequest;
import com.adminvisitor.dto.responsedto.EmployeeResponse;
import com.adminvisitor.entity.Employee;
import com.adminvisitor.repository.EmployeeRepository;
import com.adminvisitor.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final EmployeeService employeeService;

    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getEmployees() {
        List<EmployeeResponse> employees =
                employeeService.getEmployees();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(employees);
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request
    ) {

        EmployeeResponse response =
                employeeService.createEmployee(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}