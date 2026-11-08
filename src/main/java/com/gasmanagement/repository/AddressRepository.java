package com.gasmanagement.repository;

import com.gasmanagement.model.Address;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends MongoRepository<Address, String> {
    List<Address> findByUserId(String userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(String userId);
    Optional<Address> findByIdAndUserId(String id, String userId);
}
