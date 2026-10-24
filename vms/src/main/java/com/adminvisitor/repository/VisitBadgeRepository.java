//package com.adminvisitor.repository;
//
//import com.adminvisitor.entity.VisitBadge;
//import jakarta.persistence.LockModeType;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Lock;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.Optional;
//
//@Repository
//public interface VisitBadgeRepository
//        extends JpaRepository<VisitBadge, String> {
//
//    Optional<VisitBadge> findByVisit_Id(String visitId);
//    Optional<VisitBadge> findByQrContainingToken(String qrContainingToken);
//
//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("""
//       SELECT b
//       FROM VisitBadge b
//       WHERE b.qrContainingToken = :token
//       """)
//    Optional<VisitBadge> findByQrContainingTokenForUpdate(
//            @Param("token") String token
//    );
//}