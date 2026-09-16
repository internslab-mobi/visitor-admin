package com.adminvisitor.entity;

import com.adminvisitor.enums.BlacklistStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "vms_blacklist")
@Getter
@Setter
@NoArgsConstructor
public class Blacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitorid", nullable = false)
    private Long visitorId;

    @Column(name = "id_type", nullable = false, length = 50)
    private String idType;

    @Column(name = "id_number", nullable = false, length = 100)
    private String idNumber;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BlacklistStatus status;

    @Column(name = "added_by", nullable = false)
    private Long addedBy;

    @Column(name = "added_at", nullable = false)
    private LocalDateTime addedAt;

    @Column(name = "removed_at")
    private LocalDateTime removedAt;

    @Column(name = "removed_by")
    private Long removedBy;
}