package com.gasmanagement.controller;

import com.gasmanagement.dto.request.ProductRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.ProductResponse;
import com.gasmanagement.model.Brand;
import com.gasmanagement.model.Category;
import com.gasmanagement.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('MANAGER')")
public class ProductController {

    private final ProductService productService;

    // --- PRODUCT ENDPOINTS ---

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productService.createProduct(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productService.updateProduct(id, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ProductResponse>> changeProductStatus(
            @PathVariable String id,
            @RequestParam boolean isActive) {
        return ResponseEntity.ok(ApiResponse.ok(productService.changeProductStatus(id, isActive)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllProducts(isActive, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getProductById(id)));
    }

    // --- CATEGORY ENDPOINTS ---

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.ok(productService.createCategory(category)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllCategories()));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Category>> updateCategory(@PathVariable String id, @RequestBody Category category) {
        return ResponseEntity.ok(ApiResponse.ok(productService.updateCategory(id, category)));
    }

    // --- BRAND ENDPOINTS ---

    @PostMapping("/brands")
    public ResponseEntity<ApiResponse<Brand>> createBrand(@RequestBody Brand brand) {
        return ResponseEntity.ok(ApiResponse.ok(productService.createBrand(brand)));
    }

    @GetMapping("/brands")
    public ResponseEntity<ApiResponse<List<Brand>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllBrands()));
    }

    @PutMapping("/brands/{id}")
    public ResponseEntity<ApiResponse<Brand>> updateBrand(@PathVariable String id, @RequestBody Brand brand) {
        return ResponseEntity.ok(ApiResponse.ok(productService.updateBrand(id, brand)));
    }
}
