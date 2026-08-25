package com.gasmanagement.repository;

import com.gasmanagement.model.User;
import com.gasmanagement.model.enums.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    long countByRole(UserRole role);
}
