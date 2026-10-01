package com.adminvisitor.controller;

import com.adminvisitor.dto.requestdto.DepartmentCreateRequest;
import com.adminvisitor.dto.responsedto.DepartmentResponse;
import com.adminvisitor.entity.Department;
import com.adminvisitor.repository.DepartmentRepository;
import com.adminvisitor.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentRepository departmentRepository;
    private final DepartmentService departmentService;

    @GetMapping
    public List<Department> getDepartments() {
        return departmentRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> createDepartment(
            @Valid @RequestBody DepartmentCreateRequest request
    ) {

        DepartmentResponse response =
                departmentService.createDepartment(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}