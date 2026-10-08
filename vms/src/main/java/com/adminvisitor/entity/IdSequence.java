package com.adminvisitor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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