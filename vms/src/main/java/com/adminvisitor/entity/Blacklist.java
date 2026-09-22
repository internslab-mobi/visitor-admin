package com.adminvisitor.entity;

import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.Nationality;
import com.adminvisitor.enums.ProofType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vms_blacklist")
@Getter
@Setter
@NoArgsConstructor
public class Blacklist extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @Enumerated(EnumType.STRING)
    @Column(name = "nationality", nullable = false, length = 20)
    private Nationality nationality;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 20)
    private ProofType proofType;

    @Column(name = "proof_blind_index", nullable = false, length = 64)
    private String proofBlindIndex;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BlacklistStatus status;
}