package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.DepartmentCreateRequest;
import com.adminvisitor.dto.responsedto.DepartmentResponse;

import java.util.List;

public interface DepartmentService {

    DepartmentResponse createDepartment(
            DepartmentCreateRequest request);

    List<DepartmentResponse> getDepartments();
}