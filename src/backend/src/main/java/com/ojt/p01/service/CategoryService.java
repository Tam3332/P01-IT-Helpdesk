package com.ojt.p01.service;

import com.ojt.p01.dto.request.CreateCategoryRequest;
import com.ojt.p01.dto.request.UpdateCategoryRequest;
import com.ojt.p01.dto.response.CategoryDetailResponse;
import com.ojt.p01.dto.response.CategoryResponse;
import com.ojt.p01.entity.TicketCategory;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý nghiệp vụ Ticket Category (UC-02, UC-10).
 *
 * GET Active    — mọi role đã xác thực (dropdown tạo vé).
 * GET All       — Admin (UC-10 quản trị danh mục).
 * POST          — Admin only.
 * PUT           — Admin only.
 * PATCH /status — Admin only.
 */
@Service
public class CategoryService {

    private final TicketCategoryRepository categoryRepository;

    public CategoryService(TicketCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // -------------------------------------------------------------------------
    // Read — Public (tất cả user đã login)
    // -------------------------------------------------------------------------

    /** Trả về danh sách danh mục Active, dùng cho dropdown tạo vé. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByIsActiveTrueOrderByCategoryNameAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Read — Admin (quản trị)
    // -------------------------------------------------------------------------

    /** Trả về toàn bộ danh mục (kể cả Inactive) — Admin xem để quản trị. */
    @Transactional(readOnly = true)
    public List<CategoryDetailResponse> getAllCategories() {
        return categoryRepository.findAllByOrderByCategoryNameAsc()
                .stream()
                .map(this::toDetailResponse)
                .toList();
    }

    /** Xem chi tiết 1 danh mục theo id. */
    @Transactional(readOnly = true)
    public CategoryDetailResponse getCategoryById(Integer id) {
        TicketCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        return toDetailResponse(cat);
    }

    // -------------------------------------------------------------------------
    // Write — Admin only
    // -------------------------------------------------------------------------

    /** UC-10: Tạo danh mục mới. Mã category_code phải duy nhất. */
    @Transactional
    public CategoryDetailResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByCategoryCode(request.categoryCode())) {
            throw new AppException(ErrorCode.CATEGORY_CODE_DUPLICATE);
        }

        TicketCategory cat = new TicketCategory();
        cat.setCategoryCode(request.categoryCode().toUpperCase());
        cat.setCategoryName(request.categoryName());
        cat.setDescription(request.description());
        cat.setDefaultPriority(request.defaultPriority().toUpperCase());
        cat.setIsActive(true); // mặc định active khi tạo mới

        return toDetailResponse(categoryRepository.save(cat));
    }

    /** UC-10: Cập nhật thông tin danh mục (partial update — chỉ field != null). */
    @Transactional
    public CategoryDetailResponse updateCategory(Integer id, UpdateCategoryRequest request) {
        TicketCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        if (request.categoryName() != null && !request.categoryName().isBlank()) {
            cat.setCategoryName(request.categoryName());
        }
        if (request.description() != null) {
            cat.setDescription(request.description());
        }
        if (request.defaultPriority() != null && !request.defaultPriority().isBlank()) {
            cat.setDefaultPriority(request.defaultPriority().toUpperCase());
        }

        return toDetailResponse(categoryRepository.save(cat));
    }

    /** UC-10: Bật / Tắt trạng thái hoạt động của danh mục. */
    @Transactional
    public CategoryDetailResponse toggleCategoryStatus(Integer id, boolean active) {
        TicketCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        cat.setIsActive(active);
        return toDetailResponse(categoryRepository.save(cat));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CategoryResponse toCategoryResponse(TicketCategory cat) {
        return new CategoryResponse(
                cat.getId(),
                cat.getCategoryCode(),
                cat.getCategoryName(),
                cat.getDescription(),
                cat.getDefaultPriority()
        );
    }

    private CategoryDetailResponse toDetailResponse(TicketCategory cat) {
        return new CategoryDetailResponse(
                cat.getId(),
                cat.getCategoryCode(),
                cat.getCategoryName(),
                cat.getDescription(),
                cat.getDefaultPriority(),
                cat.getIsActive(),
                cat.getCreatedAt(),
                cat.getUpdatedAt()
        );
    }
}
