package com.adminvisitor.entity;

import com.adminvisitor.enums.Nationality;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    @Column(name = "nationality", nullable = false, length = 20)
    private Nationality nationality;

    @Column(name = "aadhar_number", length = 100)
    private String aadharNumber;

    @Column(name = "aadhar_document", length = 500)
    private String aadharDocument;

    @Column(name = "pan_number", length = 100)
    private String panNumber;

    @Column(name = "pan_document", length = 500)
    private String panDocument;

    @Column(name = "passport_number", length = 100)
    private String passportNumber;

    @Column(name = "passport_document", length = 500)
    private String passportDocument;

    @Column(name = "nda_document", length = 500)
    private String ndaDocument;
}