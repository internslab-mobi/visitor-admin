package com.adminvisitor.service.impl;

import com.adminvisitor.dto.requestdto.DepartmentCreateRequest;
import com.adminvisitor.dto.responsedto.DepartmentResponse;
import com.adminvisitor.entity.Department;
import com.adminvisitor.exception.ResourceAlreadyExistsException;
import com.adminvisitor.repository.DepartmentRepository;
import com.adminvisitor.service.DepartmentService;
import com.adminvisitor.service.IdGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implements department creation and retrieval operations.
 *
 * @author Nethra
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final IdGeneratorService idGeneratorService;

    /**
     * Creates a department after checking that its code and name are unique.
     * Generates a department ID, sets its initial status to ACTIVE, and saves it.
     *
     * @param request the department details to create
     * @return the response containing the saved department details
     * @throws ResourceAlreadyExistsException if the department code or name already exists
     */
    @Transactional
    public DepartmentResponse createDepartment(
            DepartmentCreateRequest request
    ) {

        if (departmentRepository.existsByDepartmentCode(
                request.departmentCode().trim()
        )) {
            throw new ResourceAlreadyExistsException(
                    "Department code already exists"
            );
        }

        if (departmentRepository.existsByDepartmentName(
                request.departmentName().trim()
        )) {
            throw new ResourceAlreadyExistsException(
                    "Department name already exists"
            );
        }

        Department department = new Department();

        department.setId(
                idGeneratorService.generateId("DEPARTMENT", "DT")
        );

        department.setDepartmentCode(
                request.departmentCode().trim()
        );

        department.setDepartmentName(
                request.departmentName().trim()
        );

        department.setStatus("ACTIVE");

        Department savedDepartment =
                departmentRepository.save(department);

        log.info(
                "DEPARTMENT_CREATED departmentId={} departmentCode={}",
                savedDepartment.getId(),
                savedDepartment.getDepartmentCode()
        );

        return new DepartmentResponse(
                savedDepartment.getId(),
                savedDepartment.getDepartmentCode(),
                savedDepartment.getDepartmentName(),
                savedDepartment.getStatus()
        );
    }

    /**
     * Retrieves all departments and converts them into response objects.
     *
     * @return a list of department responses, or an empty list if no departments exist
     */
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getDepartments() {

        List<DepartmentResponse> departments =
                departmentRepository.findAll()
                        .stream()
                        .map(department -> new DepartmentResponse(
                                department.getId(),
                                department.getDepartmentCode(),
                                department.getDepartmentName(),
                                department.getStatus()
                        ))
                        .toList();

        log.debug(
                "DEPARTMENTS_RETRIEVED count={}",
                departments.size()
        );

        return departments;
    }
}