package com.gasmanagement.repository;

import com.gasmanagement.model.Voucher;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VoucherRepository extends MongoRepository<Voucher, String> {
    Optional<Voucher> findByCode(String code);

    @Query("{ 'status': 'ACTIVE', 'startDate': { $lte: ?0 }, 'endDate': { $gte: ?0 } }")
    List<Voucher> findActiveVouchers(LocalDateTime now);
}
