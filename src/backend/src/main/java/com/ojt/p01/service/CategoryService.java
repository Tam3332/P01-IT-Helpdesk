package com.ojt.p01.service;

import com.ojt.p01.dto.response.CategoryResponse;
import com.ojt.p01.repository.TicketCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý nghiệp vụ danh mục sự cố.
 */
@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final TicketCategoryRepository categoryRepository;

    public CategoryService(TicketCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Lấy danh sách danh mục đang hoạt động (is_active = true), sắp xếp A-Z.
     * Dùng cho dropdown tạo ticket trên Frontend.
     */
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByCategoryNameAsc()
                .stream()
                .map(cat -> new CategoryResponse(
                        cat.getId(),
                        cat.getCategoryCode(),
                        cat.getCategoryName(),
                        cat.getDefaultPriority()
                ))
                .toList();
    }
}
