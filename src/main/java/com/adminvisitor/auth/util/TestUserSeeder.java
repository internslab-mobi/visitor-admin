package com.adminvisitor.auth.util;

import com.adminvisitor.auth.entity.Employee;
import com.adminvisitor.auth.entity.User;
import com.adminvisitor.auth.enums.UserRole;
import com.adminvisitor.auth.repository.EmployeeRepository;
import com.adminvisitor.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@Configuration
public class TestUserSeeder {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public TestUserSeeder(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    CommandLineRunner createTestUser() {
        return args -> {

            User user = userRepository.findById("USR001")
                    .orElseGet(User::new);

            Employee employee = employeeRepository.findById("EMP-001")
                    .orElseGet(() -> {
                        Employee emp = new Employee();
                        emp.setId("EMP-001");
                        emp.setFirstName("System");
                        emp.setLastName("Admin");
                        emp.setEmail("admin@adminvisitor.com");
                        emp.setMobileNumber("1234567890");
                        emp.setDesignation("Administrator");
                        emp.setStatus("ACTIVE");
                        return employeeRepository.save(emp);
                    });

            user.setId("USR001");
            user.setEmployee(employee);

            user.setPasswordHash(
                    passwordEncoder.encode("Admin@123")
            );

            user.setRole(UserRole.ADMIN);
            user.setStatus("ACTIVE");
            user.setMustChangePassword(false);

            LocalDateTime now = LocalDateTime.now();
            if (user.getCreatedAt() == null) {
                user.setCreatedAt(now);
            }
            user.setUpdatedAt(now);

            userRepository.save(user);
        };
    }
}