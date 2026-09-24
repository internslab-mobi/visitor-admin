package com.adminvisitor.entity;

import com.adminvisitor.enums.Nationality;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vms_document")
@Getter
@Setter
@NoArgsConstructor
public class Document extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @Enumerated(EnumType.STRING)
    @Column(name = "nationality", length = 100)
    private Nationality nationality;

    /*
     * These columns contain HMAC-SHA-256 blind indexes.
     * They do NOT contain the original Aadhaar/PAN/Passport numbers.
     */
    @Column(name = "aadhar_number", length = 64)
    private String aadharNumber;

    @Column(name = "pan_number", length = 64)
    private String panNumber;

    @Column(name = "passport_number", length = 64)
    private String passportNumber;

    @OneToMany(
            mappedBy = "document",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<DocumentMetadata> metadata = new ArrayList<>();
}