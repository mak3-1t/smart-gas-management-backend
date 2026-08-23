package com.gasmanagement.controller;

import com.gasmanagement.dto.request.ProductRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.ProductResponse;
import com.gasmanagement.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // ─────────────────────────────────────────────────────
    //  MANAGER – Quản lý sản phẩm
    // ─────────────────────────────────────────────────────

    /** [MANAGER] Tạo sản phẩm mới */
    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công", response));
    }

    /** [MANAGER] Cập nhật sản phẩm */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", response));
    }

    /** [MANAGER] Vô hiệu hóa sản phẩm (soft delete) */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivateProduct(@PathVariable String id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Đã vô hiệu hóa sản phẩm", null));
    }

    /** [MANAGER] Kích hoạt lại sản phẩm */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<ProductResponse>> activateProduct(@PathVariable String id) {
        ProductResponse response = productService.activateProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Đã kích hoạt sản phẩm", response));
    }

    // ─────────────────────────────────────────────────────
    //  PUBLIC / CUSTOMER – Duyệt sản phẩm
    // ─────────────────────────────────────────────────────

    /** [ALL] Lấy sản phẩm theo ID */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductById(id)));
    }

    /** [ALL] Danh sách sản phẩm đang ACTIVE */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllActiveProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllActiveProducts()));
    }

    /** [ALL] Tìm kiếm sản phẩm theo keyword (name, gasType) */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(productService.searchProducts(keyword)));
    }

    /** [ALL] Filter sản phẩm theo brand */
    @GetMapping("/brand/{brandId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getByBrand(
            @PathVariable String brandId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByBrand(brandId)));
    }

    /** [ALL] Filter sản phẩm theo category */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getByCategory(
            @PathVariable String categoryId) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByCategory(categoryId)));
    }

    /** [ALL] Filter sản phẩm theo gasType */
    @GetMapping("/gas-type/{gasType}")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getByGasType(
            @PathVariable String gasType) {
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsByGasType(gasType)));
    }

    /** [ALL] Filter sản phẩm theo khoảng giá */
    @GetMapping("/price-range")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getByPriceRange(
            @RequestParam double minPrice,
            @RequestParam double maxPrice) {
        return ResponseEntity.ok(ApiResponse.success(
                productService.getProductsByPriceRange(minPrice, maxPrice)));
    }

    /** [MANAGER] Lấy tất cả sản phẩm (kể cả INACTIVE) */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts()));
    }
}
