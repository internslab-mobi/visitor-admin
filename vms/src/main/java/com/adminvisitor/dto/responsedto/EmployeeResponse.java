package com.adminvisitor.dto.responsedto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmployeeResponse {

    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String departmentId;
    private String departmentName;
    private String designation;
    private String status;
}