package com.adminvisitor.entity;

import com.adminvisitor.enums.ProofType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "vms_blacklist_proof",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_blacklist_proof_type",
                        columnNames = {"blacklist_id", "proof_type"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
public class BlacklistProof extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blacklist_id", nullable = false)
    private Blacklist blacklist;

    @Enumerated(EnumType.STRING)
    @Column(name = "proof_type", nullable = false, length = 20)
    private ProofType proofType;

    @Column(name = "proof_blind_index", nullable = false, length = 64)
    private String proofBlindIndex;
}