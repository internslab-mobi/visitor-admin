package com.adminvisitor.auth.mapper;

import com.adminvisitor.auth.dto.responsedto.UserResponse;
import com.adminvisitor.auth.entity.Employee;
import com.adminvisitor.auth.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for transforming {@link User} entities into {@link UserResponse} data transfer objects.
 * <p>
 * Flattens nested employee attributes into top-level response properties and computes
 * the employee's concatenated full name.
 * </p>
 *
 * @author Logeshwaran
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts a {@link User} persistence entity into a client-facing {@link UserResponse} DTO.
     *
     * @param user the user entity to transform
     * @return the populated {@link UserResponse} instance
     */
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

    /**
     * Derives a full name string by concatenating and trimming the first and last name of the given employee.
     *
     * @param employee the employee entity, or {@code null}
     * @return trimmed full name string, or {@code null} if the employee is null
     */
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
