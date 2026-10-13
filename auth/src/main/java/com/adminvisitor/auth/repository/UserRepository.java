package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmployeeEmail(String email);

    boolean existsByIdAndStatus(String id, String status);
}