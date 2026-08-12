package com.gasmanagement.repository;

import com.gasmanagement.model.Product;
import com.gasmanagement.model.enums.ProductStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface ProductRepository extends MongoRepository<Product, String> {
    List<Product> findByStatus(ProductStatus status);
    List<Product> findByBrandIdAndStatus(String brandId, ProductStatus status);
    List<Product> findByCategoryIdAndStatus(String categoryId, ProductStatus status);

    @Query("{ 'name': { $regex: ?0, $options: 'i' }, 'status': 'ACTIVE' }")
    List<Product> searchByName(String keyword);
}
