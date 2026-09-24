package com.adminvisitor.repository;

import com.adminvisitor.entity.BlacklistProof;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.ProofType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistProofRepository
        extends JpaRepository<BlacklistProof, String> {

    Optional<BlacklistProof> findByProofTypeAndProofBlindIndexAndBlacklistStatus(
            ProofType proofType,
            String proofBlindIndex,
            BlacklistStatus status
    );
}