package com.adminvisitor.entity;

import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.Nationality;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BlacklistStatus status;

    @OneToMany(
            mappedBy = "blacklist",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<BlacklistProof> proofs = new ArrayList<>();

    @Builder
    public Blacklist(
            String id,
            Visitor visitor,
            Nationality nationality,
            String reason,
            BlacklistStatus status,
            List<BlacklistProof> proofs,
            String createdBy
    ) {
        this.id = id;
        this.visitor = visitor;
        this.nationality = nationality;
        this.reason = reason;
        this.status = status;
        this.proofs = proofs != null ? proofs : new ArrayList<>();
        this.setCreatedBy(createdBy);
    }
}