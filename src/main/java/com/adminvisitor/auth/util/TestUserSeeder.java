package com.adminvisitor.auth.util;

import com.adminvisitor.auth.entity.Employee;
import com.adminvisitor.auth.entity.User;
import com.adminvisitor.auth.enums.UserRole;
import com.adminvisitor.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.persistence.EntityManager;

@Configuration
@RequiredArgsConstructor
public class TestUserSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    @Bean
    CommandLineRunner createTestUser() {
        return args -> {

            User user = userRepository.findById("USR001")
                    .orElseGet(User::new);

            Employee employee =
                    entityManager.getReference(Employee.class, "EMP-001");

            user.setId("USR001");
            user.setEmployee(employee);

            user.setPasswordHash(
                    passwordEncoder.encode("Admin@123")
            );

            user.setRole(UserRole.ADMIN);
            user.setStatus("ACTIVE");
            user.setMustChangePassword(false);

            userRepository.save(user);
        };
    }
}