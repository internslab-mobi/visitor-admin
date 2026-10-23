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

/**
 * Startup configuration runner that initializes default administrator credentials upon application startup.
 * <p>
 * Ensures that an initial employee ({@code EMP-001}) and system administrator user ({@code USR-001})
 * exist with credentials {@code admin@adminvisitor.com} / {@code Admin@123} if not previously seeded.
 * </p>
 *
 * @author Logeshwaran
 */
@Configuration
public class TestUserSeeder {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs the test user seeder with required repository and encoder dependencies.
     *
     * @param userRepository the user repository for checking and persisting the admin user
     * @param employeeRepository the employee repository for persisting the admin employee profile
     * @param passwordEncoder the password encoder for hashing the default admin password
     */
    public TestUserSeeder(
            UserRepository userRepository,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a {@link CommandLineRunner} bean executed on startup to seed the default administrator.
     *
     * @return the command line runner instance
     */
    @Bean
    CommandLineRunner createTestUser() {
        return args -> {

            User user = userRepository.findById("USR-001")
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

            user.setId("USR-001");
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