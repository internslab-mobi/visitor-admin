package com.adminvisitor.repository;

import com.adminvisitor.entity.Visit;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.adminvisitor.enums.VisitStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitRepository extends JpaRepository<Visit, String>, JpaSpecificationExecutor<Visit> {

        @Query("""
        SELECT v
        FROM Visit v
        WHERE v.visitor.id = :visitorId
          AND v.status IN :statuses
          AND v.expectedArrivalAt < :newDepartureAt
          AND v.expectedDepartureAt > :newArrivalAt
        """)
        List<Visit> findOverlappingVisits(
                @Param("visitorId") String visitorId,
                @Param("newArrivalAt") LocalDateTime newArrivalAt,
                @Param("newDepartureAt") LocalDateTime newDepartureAt,
                @Param("statuses") Collection<VisitStatus> statuses
        );

        Optional<Visit> findTopByVisitorIdOrderByCreatedAtDesc(String visitorId);

        List<Visit> findByStatusAndExpectedArrivalAtBefore(
                VisitStatus status,
                LocalDateTime time
        );

        List<Visit> findByVisitorIdOrderByExpectedArrivalAtDesc(
                String visitorId
        );


        @Query("""
    SELECT v
    FROM Visit v
    WHERE v.visitor.id IN :visitorIds
      AND NOT EXISTS (
          SELECT newer.id
          FROM Visit newer
          WHERE newer.visitor.id = v.visitor.id
            AND (
                newer.createdAt > v.createdAt
                OR (
                    newer.createdAt = v.createdAt
                    AND newer.id > v.id
                )
            )
      )
    """)
        List<Visit> findLatestVisitsForVisitors(
                @Param("visitorIds") Collection<String> visitorIds
        );
}
