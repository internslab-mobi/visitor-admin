package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for persisting and querying {@link User} entities in the {@code vms_user} table.
 * <p>
 * Provides methods for email-based lookup, status existence checks, and row-level pessimistic locking for updates.
 * </p>
 *
 * @author Logeshwaran
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * Finds a user account matching the email address of the associated employee.
     *
     * @param email the employee email address
     * @return an {@link Optional} containing the user entity if found, or empty otherwise
     */
    Optional<User> findByEmployeeEmail(String email);

    /**
     * Checks if a user account exists matching both the specified identifier and account status.
     *
     * @param id the user identifier
     * @param status the expected account status (e.g., "ACTIVE")
     * @return {@code true} if an active matching user exists, {@code false} otherwise
     */
    boolean existsByIdAndStatus(String id, String status);

    /**
     * Retrieves a user entity by its identifier while applying a pessimistic write lock ({@code PESSIMISTIC_WRITE}).
     * <p>
     * Ensures exclusive database access during critical updates such as credential modifications.
     * </p>
     *
     * @param userId the user identifier
     * @return an {@link Optional} containing the locked user entity, or empty if not found
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") String userId);
}