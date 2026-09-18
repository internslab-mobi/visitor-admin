package com.adminvisitor.entity;

import com.adminvisitor.enums.BadgeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vms_visitbadge",
        indexes = {
                @Index(
                        name = "idx_vms_visitbadge_visitor_id",
                        columnList = "visitor_id"
                ),
                @Index(
                        name = "idx_vms_visitbadge_visit_id",
                        columnList = "visit_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class VisitBadge extends BaseEntity {

    @Id
    @Column(name = "id", length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "visitor_id",
            nullable = false
    )
    private Visitor visitor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "visit_id",
            nullable = false
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

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

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