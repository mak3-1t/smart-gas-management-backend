package com.gasmanagement.repository;

import com.gasmanagement.model.StoreSetting;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreSettingRepository extends MongoRepository<StoreSetting, String> {
    Optional<StoreSetting> findBySettingKey(String settingKey);
}
