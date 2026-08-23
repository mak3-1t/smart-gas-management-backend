package com.gasmanagement.service;

import com.gasmanagement.dto.request.BrandRequest;
import com.gasmanagement.dto.response.BrandResponse;

import java.util.List;

public interface BrandService {
    BrandResponse createBrand(BrandRequest request);
    BrandResponse getBrandById(String id);
    List<BrandResponse> getAllBrands();
    BrandResponse updateBrand(String id, BrandRequest request);
    void deleteBrand(String id);
}
