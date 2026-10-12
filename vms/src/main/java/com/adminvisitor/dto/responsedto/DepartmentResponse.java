package com.adminvisitor.dto.responsedto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DepartmentResponse {

    private String id;
    private String departmentCode;
    private String departmentName;
    private String status;
}