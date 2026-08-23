package com.gasmanagement.service;

import com.gasmanagement.dto.request.CategoryRequest;
import com.gasmanagement.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse getCategoryById(String id);
    List<CategoryResponse> getAllCategories();
    CategoryResponse updateCategory(String id, CategoryRequest request);
    void deleteCategory(String id);
}
