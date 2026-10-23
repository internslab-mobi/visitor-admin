package com.adminvisitor.auth.entity;

import com.adminvisitor.auth.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Entity representing an authenticated user account within the Visitor Management System.
 * <p>
 * Mapped to the database table {@code vms_user}. Enforces a one-to-one relationship with {@link Employee},
 * stores BCrypt password hashes, assigns security roles ({@link UserRole}), and manages account
 * activation and initial password change requirements.
 * </p>
 *
 * @author Logeshwaran
 */
@Entity
@Table(
        name = "vms_user",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vms_user_employee",
                        columnNames = "employee_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    /**
     * Associated employee profile linked to this user account in a unique one-to-one relationship.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            unique = true
    )
    private Employee employee;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Flag indicating whether the user is required to change their initial password before accessing the system.
     */
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}