package com.qrservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vms_meta_time_tracking",
        indexes = {
                @Index(name = "idx_tracking_visit", columnList = "visit_id"),
                @Index(name = "idx_tracking_visitor", columnList = "visitor_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class MetaTimeTracking extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "visitor_id", nullable = false, length = 50)
    private String visitorId;

    @Column(name = "visit_id", nullable = false, length = 50)
    private String visitId;

    @Column(name = "badge_id", nullable = false, length = 50)
    private String badgeId;

    @Column(name = "in_time", nullable = false)
    private LocalDateTime inTime;

    @Column(name = "out_time")
    private LocalDateTime outTime;
}