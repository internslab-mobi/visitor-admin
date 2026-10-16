package com.adminvisitor.repository;

import com.adminvisitor.entity.MetaTimeTracking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetaTimeTrackingRepository
        extends JpaRepository<MetaTimeTracking, Long> {

    Optional<MetaTimeTracking>
    findTopByVisitIdAndOutTimeIsNullOrderByInTimeDesc(
            String visitId
    );

    List<MetaTimeTracking> findByVisitIdOrderByInTimeAsc(
            String visitId
    );
}
