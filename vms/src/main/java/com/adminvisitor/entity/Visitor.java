package com.adminvisitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vms_visitor",
        indexes = {
                @Index(
                        name = "idx_vms_visitor_email",
                        columnList = "email"
                ),
                @Index(
                        name = "idx_vms_visitor_mobile_number",
                        columnList = "mobile_number"
                ),
                @Index(
                        name = "idx_vms_visitor_first_name",
                        columnList = "first_name"
                ),
                @Index(
                        name = "idx_vms_visitor_last_name",
                        columnList = "last_name"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Visitor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "company_name", nullable = false, length = 150)
    private String companyName;

    @Column(name = "cooldown_until")
    private LocalDateTime cooldownUntil;
}