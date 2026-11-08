package com.gasmanagement.service;

import com.gasmanagement.dto.request.ProductRequest;
import com.gasmanagement.dto.response.ProductResponse;
import com.gasmanagement.model.Brand;
import com.gasmanagement.model.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    // --- Product ---
    ProductResponse createProduct(ProductRequest request);
    ProductResponse updateProduct(String id, ProductRequest request);
    ProductResponse changeProductStatus(String id, boolean isActive);
    void deleteProduct(String id);
    Page<ProductResponse> getAllProducts(Boolean isActive, Pageable pageable);
    ProductResponse getProductById(String id);

    /**
     * Tìm kiếm và lọc sản phẩm cho Khách hàng
     */
    Page<ProductResponse> searchProducts(String keyword, String brandId, String categoryId,
                                         String gasType, Double minWeight, Double maxWeight,
                                         Double minPrice, Double maxPrice, Boolean inStockOnly,
                                         Pageable pageable);

    // --- Category ---
    Category createCategory(Category category);
    List<Category> getAllCategories();
    Category updateCategory(String id, Category category);

    // --- Brand ---
    Brand createBrand(Brand brand);
    List<Brand> getAllBrands();
    Brand updateBrand(String id, Brand brand);
}
