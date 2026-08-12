package com.gasmanagement.repository;

import com.gasmanagement.model.StaffProfile;
import com.gasmanagement.model.enums.StaffStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface StaffProfileRepository extends MongoRepository<StaffProfile, String> {
    Optional<StaffProfile> findByUserId(String userId);
    List<StaffProfile> findByStaffStatus(StaffStatus status);
}
