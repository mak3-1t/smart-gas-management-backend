package com.gasmanagement.controller;

import com.gasmanagement.dto.request.BrandRequest;
import com.gasmanagement.dto.response.ApiResponse;
import com.gasmanagement.dto.response.BrandResponse;
import com.gasmanagement.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    /**
     * [MANAGER] Tạo thương hiệu mới
     * POST /api/v1/brands
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo thương hiệu thành công", response));
    }

    /**
     * [ALL] Lấy danh sách tất cả thương hiệu
     * GET /api/v1/brands
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success(brandService.getAllBrands()));
    }

    /**
     * [ALL] Lấy thương hiệu theo ID
     * GET /api/v1/brands/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(brandService.getBrandById(id)));
    }

    /**
     * [MANAGER] Cập nhật thương hiệu
     * PUT /api/v1/brands/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable String id,
            @Valid @RequestBody BrandRequest request) {
        BrandResponse response = brandService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thương hiệu thành công", response));
    }

    /**
     * [MANAGER] Xoá thương hiệu
     * DELETE /api/v1/brands/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(@PathVariable String id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.success("Đã xoá thương hiệu", null));
    }
}
