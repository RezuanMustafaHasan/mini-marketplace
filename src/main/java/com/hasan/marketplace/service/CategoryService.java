package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.CategoryRequest;
import com.hasan.marketplace.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse getCategoryById(Long categoryId);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long categoryId, CategoryRequest request);

    void deleteCategory(Long categoryId);
}
