package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.ProductRequest;
import com.gasmanagement.dto.response.ProductResponse;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Product;
import com.gasmanagement.model.enums.ProductStatus;
import com.gasmanagement.repository.ProductRepository;
import com.gasmanagement.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    // ─────────────── MANAGER ───────────────

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .brandId(request.getBrandId())
                .categoryId(request.getCategoryId())
                .gasType(request.getGasType())
                .weightKg(request.getWeightKg())
                .gasPrice(request.getGasPrice())
                .cylinderFee(request.getCylinderFee())
                .description(request.getDescription())
                .status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        return mapToResponse(productRepository.save(product));
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = findByIdOrThrow(id);
        product.setName(request.getName());
        product.setBrandId(request.getBrandId());
        product.setCategoryId(request.getCategoryId());
        product.setGasType(request.getGasType());
        product.setWeightKg(request.getWeightKg());
        product.setGasPrice(request.getGasPrice());
        product.setCylinderFee(request.getCylinderFee());
        product.setDescription(request.getDescription());
        product.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(productRepository.save(product));
    }

    @Override
    public void deactivateProduct(String id) {
        Product product = findByIdOrThrow(id);
        product.setStatus(ProductStatus.INACTIVE);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
    }

    @Override
    public ProductResponse activateProduct(String id) {
        Product product = findByIdOrThrow(id);
        product.setStatus(ProductStatus.ACTIVE);
        product.setUpdatedAt(LocalDateTime.now());
        return mapToResponse(productRepository.save(product));
    }

    @Override
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────── PUBLIC ───────────────

    @Override
    public ProductResponse getProductById(String id) {
        return mapToResponse(findByIdOrThrow(id));
    }

    @Override
    public List<ProductResponse> getAllActiveProducts() {
        return productRepository.findByStatus(ProductStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.searchActiveProducts(keyword).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByBrand(String brandId) {
        return productRepository.findByBrandIdAndStatus(brandId, ProductStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByCategory(String categoryId) {
        return productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByGasType(String gasType) {
        return productRepository.findByGasTypeAndStatus(gasType, ProductStatus.ACTIVE).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProductResponse> getProductsByPriceRange(double minPrice, double maxPrice) {
        return productRepository.findByGasPriceBetweenAndStatusActive(minPrice, maxPrice).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─────────────── HELPER ───────────────

    private Product findByIdOrThrow(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .brandId(product.getBrandId())
                .categoryId(product.getCategoryId())
                .gasType(product.getGasType())
                .weightKg(product.getWeightKg())
                .gasPrice(product.getGasPrice())
                .cylinderFee(product.getCylinderFee())
                .description(product.getDescription())
                .images(product.getImages())
                .status(product.getStatus())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
