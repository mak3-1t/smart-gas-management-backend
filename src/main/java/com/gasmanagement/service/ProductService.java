package com.gasmanagement.service;

import com.gasmanagement.dto.request.ProductRequest;
import com.gasmanagement.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    // MANAGER
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(String id, ProductRequest request);
    void deactivateProduct(String id);
    ProductResponse activateProduct(String id);
    List<ProductResponse> getAllProducts(); // kể cả INACTIVE

    // PUBLIC
    ProductResponse getProductById(String id);
    List<ProductResponse> getAllActiveProducts();
    List<ProductResponse> searchProducts(String keyword);
    List<ProductResponse> getProductsByBrand(String brandId);
    List<ProductResponse> getProductsByCategory(String categoryId);
    List<ProductResponse> getProductsByGasType(String gasType);
    List<ProductResponse> getProductsByPriceRange(double minPrice, double maxPrice);
}
