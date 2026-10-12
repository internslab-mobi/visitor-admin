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

        @EntityGraph(attributePaths = {"host", "host.department"})
        List<Visit> findByVisitorIdOrderByExpectedArrivalAtDesc(
                String visitorId
        );


        @EntityGraph(attributePaths = {"visitor", "host"})
        @Override
        List<Visit> findAll(
                org.springframework.data.jpa.domain.Specification<Visit> specification,
                org.springframework.data.domain.Sort sort
        );

        @EntityGraph(attributePaths = {"visitor", "host"})
        @Override
        org.springframework.data.domain.Page<Visit> findAll(
                org.springframework.data.jpa.domain.Specification<Visit> specification,
                org.springframework.data.domain.Pageable pageable
        );

        @Query("SELECT v FROM Visit v WHERE v.id = :visitId")
        @EntityGraph(attributePaths = {"visitor", "host", "host.department"})
        Optional<Visit> findDetailsById(@Param("visitId") String visitId);


        @Query("""
    SELECT v.visitor.id, v.visitorType, v.expectedArrivalAt
    FROM Visit v
    WHERE v.visitor.id IN :visitorIds
      AND v.createdAt = (
          SELECT MAX(v2.createdAt)
          FROM Visit v2
          WHERE v2.visitor.id = v.visitor.id
      )
    """)
        List<Object[]> findLatestVisitsByVisitorIds(
                @Param("visitorIds") Collection<String> visitorIds);

}