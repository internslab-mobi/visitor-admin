package com.adminvisitor.auth.repository;

import com.adminvisitor.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmployeeEmail(String email);

    boolean existsByIdAndStatus(String id, String status);

    Optional<User> findByEmployee_Id(String employeeId);
}