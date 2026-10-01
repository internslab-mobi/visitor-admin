package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.DepartmentCreateRequest;
import com.adminvisitor.dto.responsedto.DepartmentResponse;
import com.adminvisitor.entity.Department;
import com.adminvisitor.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final IdGeneratorService idGeneratorService;

    @Transactional
    public DepartmentResponse createDepartment(
            DepartmentCreateRequest request
    ) {

        if (departmentRepository.existsByDepartmentCode(
                request.getDepartmentCode().trim()
        )) {
            throw new IllegalArgumentException(
                    "Department code already exists"
            );
        }

        if (departmentRepository.existsByDepartmentName(
                request.getDepartmentName().trim()
        )) {
            throw new IllegalArgumentException(
                    "Department name already exists"
            );
        }

        Department department = new Department();

        department.setId(
                idGeneratorService.generateId("DEPARTMENT","DT")
        );

        department.setDepartmentCode(
                request.getDepartmentCode().trim()
        );

        department.setDepartmentName(
                request.getDepartmentName().trim()
        );

        department.setStatus("ACTIVE");

        Department savedDepartment =
                departmentRepository.save(department);

        return new DepartmentResponse(
                savedDepartment.getId(),
                savedDepartment.getDepartmentCode(),
                savedDepartment.getDepartmentName(),
                savedDepartment.getStatus()
        );
    }
}