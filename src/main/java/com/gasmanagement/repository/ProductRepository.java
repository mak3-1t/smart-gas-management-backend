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

    List<Product> findByGasTypeAndStatus(String gasType, ProductStatus status);
    List<Product> findByBrandIdAndCategoryIdAndStatus(String brandId, String categoryId, ProductStatus status);

    @Query("{ 'gasPrice': { $gte: ?0, $lte: ?1 }, 'status': 'ACTIVE' }")
    List<Product> findByGasPriceBetweenAndStatusActive(double minPrice, double maxPrice);

    @Query("{ 'status': 'ACTIVE', $or: [ { 'name': { $regex: ?0, $options: 'i' } }, { 'gasType': { $regex: ?0, $options: 'i' } } ] }")
    List<Product> searchActiveProducts(String keyword);
}
