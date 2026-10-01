package com.adminvisitor.repository;

import com.adminvisitor.entity.Blacklist;
import com.adminvisitor.enums.BlacklistStatus;
import com.adminvisitor.enums.ProofType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, String> {

    Optional<Blacklist> findByVisitorIdAndStatus(
            String visitorId,
            BlacklistStatus status
    );

}