package com.gasmanagement.repository;

import com.gasmanagement.model.CustomerProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerProfileRepository extends MongoRepository<CustomerProfile, String> {
    Optional<CustomerProfile> findByUserId(String userId);
    boolean existsByUserId(String userId);
    void deleteByUserId(String userId);
}
