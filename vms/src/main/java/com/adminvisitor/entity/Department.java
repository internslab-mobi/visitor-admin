package com.adminvisitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vms_department")
@Getter
@Setter
@NoArgsConstructor
public class Department {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @Column(
            name = "department_code",
            nullable = false,
            unique = true,
            length = 50
    )
    private String departmentCode;

    @Column(
            name = "department_name",
            nullable = false,
            unique = true,
            length = 100
    )
    private String departmentName;

    @Column(name = "status", nullable = false, length = 20)
    private String status;
}