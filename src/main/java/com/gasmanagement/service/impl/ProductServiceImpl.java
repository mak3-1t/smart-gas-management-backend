package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.ProductRequest;
import com.gasmanagement.dto.response.ProductResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Brand;
import com.gasmanagement.model.Category;
import com.gasmanagement.model.Product;
import com.gasmanagement.model.enums.ProductStatus;
import com.gasmanagement.repository.BrandRepository;
import com.gasmanagement.repository.CategoryRepository;
import com.gasmanagement.repository.ProductRepository;
import com.gasmanagement.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        validateReferences(request);

        Product product = Product.builder()
                .name(request.getName())
                .brandId(request.getBrandId())
                .categoryId(request.getCategoryId())
                .gasType(request.getGasType())
                .weightKg(request.getWeightKg())
                .gasPrice(request.getGasPrice())
                .cylinderFee(request.getCylinderFee())
                .description(request.getDescription())
                .images(request.getImages())
                .status(ProductStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        validateReferences(request);

        product.setName(request.getName());
        product.setBrandId(request.getBrandId());
        product.setCategoryId(request.getCategoryId());
        product.setGasType(request.getGasType());
        product.setWeightKg(request.getWeightKg());
        product.setGasPrice(request.getGasPrice());
        product.setCylinderFee(request.getCylinderFee());
        product.setDescription(request.getDescription());
        product.setImages(request.getImages());
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);
        return mapToResponse(savedProduct);
    }

    @Override
    public ProductResponse changeProductStatus(String id, boolean isActive) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        
        product.setStatus(isActive ? ProductStatus.ACTIVE : ProductStatus.INACTIVE);
        product.setUpdatedAt(LocalDateTime.now());
        
        return mapToResponse(productRepository.save(product));
    }

    @Override
    public Page<ProductResponse> getAllProducts(Boolean isActive, Pageable pageable) {
        Page<Product> products;
        if (isActive != null) {
            ProductStatus status = isActive ? ProductStatus.ACTIVE : ProductStatus.INACTIVE;
            products = productRepository.findByStatus(status, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(this::mapToResponse);
    }

    @Override
    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToResponse(product);
    }

    // --- CATEGORY ---

    @Override
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category updateCategory(String id, Category categoryDetails) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
        category.setName(categoryDetails.getName());
        category.setDescription(categoryDetails.getDescription());
        category.setActive(categoryDetails.isActive());
        return categoryRepository.save(category);
    }

    // --- BRAND ---

    @Override
    public Brand createBrand(Brand brand) {
        return brandRepository.save(brand);
    }

    @Override
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Override
    public Brand updateBrand(String id, Brand brandDetails) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));
        brand.setName(brandDetails.getName());
        brand.setDescription(brandDetails.getDescription());
        brand.setActive(brandDetails.isActive());
        return brandRepository.save(brand);
    }

    // --- HELPERS ---

    private void validateReferences(ProductRequest request) {
        if (request.getCategoryId() != null && !categoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException("Category ID does not exist");
        }
        if (request.getBrandId() != null && !brandRepository.existsById(request.getBrandId())) {
            throw new BusinessException("Brand ID does not exist");
        }
    }

    private ProductResponse mapToResponse(Product product) {
        String categoryName = null;
        if (product.getCategoryId() != null) {
            categoryName = categoryRepository.findById(product.getCategoryId())
                    .map(Category::getName).orElse(null);
        }

        String brandName = null;
        if (product.getBrandId() != null) {
            brandName = brandRepository.findById(product.getBrandId())
                    .map(Brand::getName).orElse(null);
        }

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .brandId(product.getBrandId())
                .brandName(brandName)
                .categoryId(product.getCategoryId())
                .categoryName(categoryName)
                .gasType(product.getGasType())
                .weightKg(product.getWeightKg())
                .gasPrice(product.getGasPrice())
                .cylinderFee(product.getCylinderFee())
                .description(product.getDescription())
                .images(product.getImages())
                .status(product.getStatus())
                .build();
    }
}
