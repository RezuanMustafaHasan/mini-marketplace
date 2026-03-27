package com.hasan.marketplace.service;

import com.hasan.marketplace.dto.CategoryRequest;
import com.hasan.marketplace.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories();

    CategoryResponse createCategory(CategoryRequest request);

    void deleteCategory(Long categoryId);
}
