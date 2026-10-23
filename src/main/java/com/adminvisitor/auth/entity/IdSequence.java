
package com.adminvisitor.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entity managing persistent identifier sequences for unique ID generation.
 * <p>
 * Mapped to the database table {@code vms_id_sequence}, storing monotonically incrementing
 * sequence counters per entity prefix (e.g., {@code USR}, {@code EMP}) under pessimistic locking.
 * </p>
 *
 * @author Logeshwaran
 */
@Entity
@Table(name = "vms_id_sequence")
@Getter
@Setter
@NoArgsConstructor
public class IdSequence {

    @Id
    @Column(name = "sequence_name", nullable = false, length = 30)
    private String sequenceName;

    @Column(name = "next_value", nullable = false)
    private long nextValue;
}
