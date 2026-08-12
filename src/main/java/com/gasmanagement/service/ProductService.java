package com.gasmanagement.service;

import com.gasmanagement.dto.request.ProductRequest;
import com.gasmanagement.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    ProductResponse createProduct(ProductRequest request);
    ProductResponse getProductById(String id);
    List<ProductResponse> getAllActiveProducts();
    ProductResponse updateProduct(String id, ProductRequest request);
    void deactivateProduct(String id);
}
