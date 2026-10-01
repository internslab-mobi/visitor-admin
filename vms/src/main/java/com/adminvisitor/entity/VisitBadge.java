package com.adminvisitor.entity;

import com.adminvisitor.enums.BadgeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_visitbadge")
@Getter
@Setter
@NoArgsConstructor
public class VisitBadge extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "visit_id",
            nullable = false,
            unique = true
    )
    private Visit visit;

    @Column(
            name = "qr_containing_token",
            nullable = false,
            unique = true,
            length = 255
    )
    private String qrContainingToken;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "valid_until", nullable = false)
    private LocalDateTime validUntil;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private BadgeStatus status;
}