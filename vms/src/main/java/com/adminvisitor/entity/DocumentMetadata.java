package com.adminvisitor.entity;

import com.adminvisitor.enums.DocumentType;
import com.adminvisitor.entity.Visit;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import com.adminvisitor.enums.NdaStatus;

@Entity
@Table(name = "vms_document_metadata")
@Getter
@Setter
@NoArgsConstructor
public class DocumentMetadata extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id")
    @JsonIgnore
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    @JsonIgnore
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", length = 30)
    private DocumentType documentType;

    @Column(name = "document_path", nullable = false, length = 500)
    private String documentPath;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "overwritten_by", length = 20)
    private String overwrittenBy;

    @Column (name="effective_validity")
    private LocalDateTime effective_validity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private NdaStatus status;
}