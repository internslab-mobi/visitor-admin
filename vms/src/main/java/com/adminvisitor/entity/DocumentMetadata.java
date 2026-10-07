//package com.adminvisitor.entity;
//
//import com.adminvisitor.enums.DocumentType;
//import com.fasterxml.jackson.annotation.JsonIgnore;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//@Entity
//@Table(name = "vms_document_metadata")
//@Getter
//@Setter
//@NoArgsConstructor
//public class DocumentMetadata extends BaseEntity {
//
//    @Id
//    @Column(name = "id", nullable = false, length = 20)
//    private String id;
//
//    @ManyToOne(fetch = FetchType.LAZY, optional = false)
//    @JoinColumn(name = "document_id", nullable = false)
//    @JsonIgnore
//    private Document document;
//
//    @Enumerated(EnumType.STRING)
//    @Column(name = "document_type", length = 30)
//    private DocumentType documentType;
//
//    @Column(name = "document_path", nullable = false, length = 500)
//    private String documentPath;
//}

package com.adminvisitor.entity;

import com.adminvisitor.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_document_metadata")
@Getter
@Setter
@NoArgsConstructor
public class DocumentMetadata extends BaseEntity {

    @Id
    @Column(name = "id", nullable = false, length = 20)
    private String id;

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

}