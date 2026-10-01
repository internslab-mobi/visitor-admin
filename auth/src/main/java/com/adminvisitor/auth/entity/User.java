package com.adminvisitor.auth.entity;

import com.adminvisitor.auth.enums.UserRole;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword = true;
}