package com.adminvisitor.entity;

import com.adminvisitor.enums.BlacklistStatus;
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
    @JoinColumn(name = "visitorid", nullable = false)
    private Visitor visitor;

    @Column(name = "id_type", nullable = false, length = 50)
    private String idType;

    @Column(name = "id_number", nullable = false, length = 100)
    private String idNumber;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlacklistStatus status;
}