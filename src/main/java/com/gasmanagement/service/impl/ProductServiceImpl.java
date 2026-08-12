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

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Override
    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
        return mapToResponse(product);
    }

    @Override
    public List<ProductResponse> getAllActiveProducts() {
        List<Product> products = productRepository.findByStatus(ProductStatus.ACTIVE);
        return products.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));

        product.setName(request.getName());
        product.setBrandId(request.getBrandId());
        product.setCategoryId(request.getCategoryId());
        product.setGasType(request.getGasType());
        product.setWeightKg(request.getWeightKg());
        product.setGasPrice(request.getGasPrice());
        product.setCylinderFee(request.getCylinderFee());
        product.setDescription(request.getDescription());
        product.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = productRepository.save(product);
        return mapToResponse(updatedProduct);
    }

    @Override
    public void deactivateProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với id: " + id));
        
        product.setStatus(ProductStatus.INACTIVE);
        product.setUpdatedAt(LocalDateTime.now());
        productRepository.save(product);
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
