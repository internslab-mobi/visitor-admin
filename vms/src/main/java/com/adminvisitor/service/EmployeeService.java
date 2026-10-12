package com.adminvisitor.service;

import com.adminvisitor.dto.requestdto.EmployeeCreateRequest;
import com.adminvisitor.dto.responsedto.EmployeeResponse;
import java.util.List;

public interface EmployeeService {

    EmployeeResponse createEmployee(EmployeeCreateRequest request);

    List<EmployeeResponse> getEmployees();
}