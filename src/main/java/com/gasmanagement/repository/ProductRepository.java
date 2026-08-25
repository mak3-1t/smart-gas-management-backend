package com.gasmanagement.repository;

import com.gasmanagement.model.Product;
import com.gasmanagement.model.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    List<Product> findByCategoryIdAndStatus(String categoryId, ProductStatus status);
    List<Product> findByBrandIdAndStatus(String brandId, ProductStatus status);
}
