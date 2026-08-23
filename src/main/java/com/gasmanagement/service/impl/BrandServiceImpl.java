package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.BrandRequest;
import com.gasmanagement.dto.response.BrandResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Brand;
import com.gasmanagement.repository.BrandRepository;
import com.gasmanagement.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;

    @Override
    public BrandResponse createBrand(BrandRequest request) {
        if (brandRepository.existsByName(request.getName())) {
            throw new IllegalArgumentException("Thương hiệu '" + request.getName() + "' đã tồn tại");
        }
        Brand brand = Brand.builder()
                .name(request.getName())
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .build();
        return mapToResponse(brandRepository.save(brand));
    }

    @Override
    public BrandResponse getBrandById(String id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + id));
        return mapToResponse(brand);
    }

    @Override
    public List<BrandResponse> getAllBrands() {
        return brandRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BrandResponse updateBrand(String id, BrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + id));

        // Kiểm tra trùng tên (ngoại trừ chính nó)
        brandRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new IllegalArgumentException("Thương hiệu '" + request.getName() + "' đã tồn tại");
            }
        });

        brand.setName(request.getName());
        brand.setLogoUrl(request.getLogoUrl());
        brand.setDescription(request.getDescription());
        return mapToResponse(brandRepository.save(brand));
    }

    @Override
    public void deleteBrand(String id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy thương hiệu với id: " + id);
        }
        brandRepository.deleteById(id);
    }

    private BrandResponse mapToResponse(Brand brand) {
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .logoUrl(brand.getLogoUrl())
                .description(brand.getDescription())
                .build();
    }
}
