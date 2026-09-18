package com.adminvisitor.entity;

import com.adminvisitor.enums.ProofType;
import com.adminvisitor.enums.RegistrationType;
import com.adminvisitor.enums.VisitStatus;
import com.adminvisitor.enums.VisitorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_visit")
@Getter
@Setter
@NoArgsConstructor
public class Visit extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @Column(
            name = "visit_reference",
            nullable = false,
            unique = true,
            length = 50
    )
    private String visitReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "visitor_id",
            nullable = false
    )
    private Visitor visitor;

    @Enumerated(EnumType.STRING)
    @Column(name = "visitor_type", nullable = false, length = 30)
    private VisitorType visitorType;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", nullable = false, length = 30)
    private RegistrationType registrationType;

    @Column(name = "purpose", nullable = false, length = 500)
    private String purpose;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "host_id", nullable = false)
    private Employee host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "expected_arrival_at", nullable = false)
    private LocalDateTime expectedArrivalAt;

    @Column(name = "expected_departure_at")
    private LocalDateTime expectedDepartureAt;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", length = 30)
    private ProofType proofType;

    @Column(name = "proof_number", length = 100)
    private String proofNumber;

    @Column(name = "proof_image_path", length = 500)
    private String proofImagePath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private VisitStatus status;
}