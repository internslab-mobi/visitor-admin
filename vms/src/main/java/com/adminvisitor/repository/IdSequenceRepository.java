package com.adminvisitor.repository;

import com.adminvisitor.entity.IdSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IdSequenceRepository extends JpaRepository<IdSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT s
            FROM IdSequence s
            WHERE s.sequenceName = :sequenceName
            """)
    Optional<IdSequence> findBySequenceNameForUpdate(
            @Param("sequenceName") String sequenceName
    );
}