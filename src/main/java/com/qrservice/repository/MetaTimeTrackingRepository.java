package com.qrservice.repository;

import com.qrservice.entity.MetaTimeTracking;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<MetaTimeTracking>
    findTopByVisitIdOrderByInTimeDesc(String visitId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT t
        FROM MetaTimeTracking t
        WHERE t.visitId = :visitId
          AND t.outTime IS NULL
        ORDER BY t.inTime DESC
        """)
    List<MetaTimeTracking> findOpenSessionsForUpdate(
            @Param("visitId") String visitId
    );
}