package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.DepartmentCreateRequest;
import com.adminvisitor.dto.responsedto.DepartmentResponse;
import com.adminvisitor.entity.Department;
import com.adminvisitor.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(
            DepartmentCreateRequest request);

    List<DepartmentResponse> getDepartments();
}