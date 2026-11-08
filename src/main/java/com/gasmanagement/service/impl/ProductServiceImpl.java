package com.gasmanagement.service.impl;

import com.gasmanagement.dto.request.ProductRequest;
import com.gasmanagement.dto.response.ProductResponse;
import com.gasmanagement.exception.BusinessException;
import com.gasmanagement.exception.ResourceNotFoundException;
import com.gasmanagement.model.Brand;
import com.gasmanagement.model.Category;
import com.gasmanagement.model.Inventory;
import com.gasmanagement.model.Product;
import com.gasmanagement.model.enums.ProductStatus;
import com.gasmanagement.repository.BrandRepository;
import com.gasmanagement.repository.CategoryRepository;
import com.gasmanagement.repository.InventoryRepository;
import com.gasmanagement.repository.ProductRepository;
import com.gasmanagement.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final InventoryRepository inventoryRepository;
    private final MongoTemplate mongoTemplate;

    // --- PRODUCT ---

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
                .updatedAt(LocalDateTime.now())
                .build();

        Product savedProduct = productRepository.save(product);

        int initialStock = request.getStockCount() != null ? Math.max(0, request.getStockCount()) : 0;
        Inventory inv = inventoryRepository.findByProductId(savedProduct.getId())
                .orElseGet(() -> Inventory.builder()
                        .productId(savedProduct.getId())
                        .fullCylinder(initialStock)
                        .emptyCylinder(0)
                        .damagedCylinder(0)
                        .reservedCylinder(0)
                        .build());
        if (request.getStockCount() != null) {
            inv.setFullCylinder(initialStock);
        }
        inventoryRepository.save(inv);

        return mapToResponse(savedProduct);
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        validateReferences(request);

        if (request.getName() != null && !request.getName().isBlank()) {
            product.setName(request.getName().trim());
        }
        if (request.getBrandId() != null && !request.getBrandId().isBlank()) {
            product.setBrandId(request.getBrandId().trim());
        }
        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()) {
            product.setCategoryId(request.getCategoryId().trim());
        }
        if (request.getGasType() != null && !request.getGasType().isBlank()) {
            product.setGasType(request.getGasType().trim());
        }
        if (request.getWeightKg() != null && request.getWeightKg() > 0) {
            product.setWeightKg(request.getWeightKg());
        }
        if (request.getGasPrice() != null) {
            product.setGasPrice(request.getGasPrice());
        }
        if (request.getCylinderFee() != null) {
            product.setCylinderFee(request.getCylinderFee());
        }
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            product.setDescription(request.getDescription().trim());
        }
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            product.setImages(request.getImages());
        }
        product.setUpdatedAt(LocalDateTime.now());

        Product savedProduct = productRepository.save(product);

        if (request.getStockCount() != null) {
            int newFull = Math.max(0, request.getStockCount());
            Inventory inv = inventoryRepository.findByProductId(savedProduct.getId())
                    .orElseGet(() -> Inventory.builder()
                            .productId(savedProduct.getId())
                            .fullCylinder(newFull)
                            .emptyCylinder(0)
                            .damagedCylinder(0)
                            .reservedCylinder(0)
                            .build());
            inv.setFullCylinder(newFull + Math.max(0, inv.getReservedCylinder()));
            inventoryRepository.save(inv);
        }

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
    public void deleteProduct(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm: " + id));
        inventoryRepository.findByProductId(product.getId())
                .ifPresent(inventoryRepository::delete);
        productRepository.delete(product);
    }

    @Override
    public Page<ProductResponse> getAllProducts(Boolean isActive, Pageable pageable) {
        ProductStatus status = (isActive == null || isActive) ? ProductStatus.ACTIVE : ProductStatus.INACTIVE;
        Page<Product> products = productRepository.findByStatus(status, pageable);
        return products.map(this::mapToResponse);
    }

    @Override
    public ProductResponse getProductById(String id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return mapToResponse(product);
    }

    @Override
    public Page<ProductResponse> searchProducts(String keyword, String brandId, String categoryId,
                                                String gasType, Double minWeight, Double maxWeight,
                                                Double minPrice, Double maxPrice, Boolean inStockOnly,
                                                Pageable pageable) {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(ProductStatus.ACTIVE));

        if (keyword != null && !keyword.trim().isEmpty()) {
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(keyword.trim(), "i"),
                    Criteria.where("description").regex(keyword.trim(), "i")
            ));
        }

        if (brandId != null && !brandId.trim().isEmpty()) {
            query.addCriteria(Criteria.where("brandId").is(brandId));
        }

        if (categoryId != null && !categoryId.trim().isEmpty()) {
            query.addCriteria(Criteria.where("categoryId").is(categoryId));
        }

        if (gasType != null && !gasType.trim().isEmpty()) {
            query.addCriteria(Criteria.where("gasType").regex(gasType.trim(), "i"));
        }

        if (minWeight != null && maxWeight != null) {
            query.addCriteria(Criteria.where("weightKg").gte(minWeight).lte(maxWeight));
        } else if (minWeight != null) {
            query.addCriteria(Criteria.where("weightKg").gte(minWeight));
        } else if (maxWeight != null) {
            query.addCriteria(Criteria.where("weightKg").lte(maxWeight));
        }

        if (minPrice != null && maxPrice != null) {
            query.addCriteria(Criteria.where("gasPrice").gte(minPrice).lte(maxPrice));
        } else if (minPrice != null) {
            query.addCriteria(Criteria.where("gasPrice").gte(minPrice));
        } else if (maxPrice != null) {
            query.addCriteria(Criteria.where("gasPrice").lte(maxPrice));
        }

        long total = mongoTemplate.count(query, Product.class);
        query.with(pageable);
        List<Product> products = mongoTemplate.find(query, Product.class);

        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .filter(p -> inStockOnly == null || !inStockOnly || p.isInStock())
                .collect(Collectors.toList());

        return new PageImpl<>(responses, pageable, total);
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
        if (request.getCategoryId() != null && !request.getCategoryId().isBlank()
                && !categoryRepository.existsById(request.getCategoryId())) {
            throw new BusinessException("Category ID does not exist");
        }
        if (request.getBrandId() != null && !request.getBrandId().isBlank()
                && !brandRepository.existsById(request.getBrandId())) {
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

        Inventory inventory = inventoryRepository.findByProductId(product.getId()).orElse(null);
        int availableStock = inventory != null ? inventory.getAvailableCylinder() : 0;
        boolean inStock = availableStock > 0;

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
                .inStock(inStock)
                .availableStock(availableStock)
                .build();
    }
}
