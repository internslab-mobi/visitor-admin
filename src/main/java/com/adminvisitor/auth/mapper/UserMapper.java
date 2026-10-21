package com.adminvisitor.auth.mapper;

import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.entity.Employee;
import com.adminvisitor.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "employeeId", source = "user.employee.id")
    @Mapping(target = "email", source = "user.employee.email")
    @Mapping(
            target = "fullName",
            expression = "java(employeeName(user.getEmployee()))"
    )
    @Mapping(target = "role", source = "user.role")
    @Mapping(target = "status", source = "user.status")
    @Mapping(
            target = "mustChangePassword",
            source = "user.mustChangePassword"
    )
    UserResponse toUserResponse(User user);

    default String employeeName(Employee employee) {
        if (employee == null) {
            return null;
        }

        String firstName = employee.getFirstName() == null
                ? "" : employee.getFirstName();

        String lastName = employee.getLastName() == null
                ? "" : employee.getLastName();

        return (firstName + " " + lastName).trim();
    }
}
