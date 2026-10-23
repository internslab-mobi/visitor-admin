package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.IdSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * Spring Data JPA repository for managing persistent sequence counters in the {@code vms_id_sequence} table.
 * <p>
 * Provides pessimistic write locking methods to guarantee atomic, collision-free identifier increments.
 * </p>
 *
 * @author Logeshwaran
 */
public interface IdSequenceRepository
        extends JpaRepository<IdSequence, String> {

    /**
     * Retrieves an {@link IdSequence} entity by its sequence name while acquiring a pessimistic write lock ({@code PESSIMISTIC_WRITE}).
     * <p>
     * Ensures exclusive database row access during ID generation transactions to avoid duplicate sequence values.
     * </p>
     *
     * @param sequenceName the name of the sequence to lock and increment (e.g., "USR", "EMP")
     * @return an {@link Optional} containing the locked sequence, or empty if not yet initialized
     */
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
