package com.ojt.p01.service;

import com.ojt.p01.dto.request.CreateCategoryRequest;
import com.ojt.p01.dto.request.UpdateCategoryRequest;
import com.ojt.p01.dto.response.CategoryDetailResponse;
import com.ojt.p01.dto.response.CategoryResponse;
import com.ojt.p01.entity.TicketCategory;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketCategoryRepository;
import com.ojt.p01.repository.TicketRepository;
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
    private final TicketRepository ticketRepository;

    public CategoryService(TicketCategoryRepository categoryRepository, TicketRepository ticketRepository) {
        this.categoryRepository = categoryRepository;
        this.ticketRepository = ticketRepository;
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
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ID_NOT_FOUND));
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
        // UC-10 3a: kiểm tra trùng cả tên (không phân biệt hoa thường)
        if (categoryRepository.existsByCategoryNameIgnoreCase(request.categoryName().trim())) {
            throw new AppException(ErrorCode.CATEGORY_NAME_DUPLICATE);
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
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ID_NOT_FOUND));

        if (request.categoryName() != null && !request.categoryName().isBlank()) {
            // Giữ tên duy nhất khi đổi tên (HD02 "tên duy nhất"); bỏ qua chính danh mục đang sửa
            if (categoryRepository.existsByCategoryNameIgnoreCaseAndIdNot(request.categoryName().trim(), id)) {
                throw new AppException(ErrorCode.CATEGORY_NAME_DUPLICATE);
            }
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
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ID_NOT_FOUND));
        cat.setIsActive(active);
        return toDetailResponse(categoryRepository.save(cat));
    }

    /** UC-10 3c: Xóa danh mục vĩnh viễn (Hard Delete). Chỉ xóa được khi chưa có ticket tham chiếu. */
    @Transactional
    public void deleteCategory(Integer id) {
        TicketCategory cat = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_ID_NOT_FOUND));

        if (ticketRepository.existsByCategory_Id(id)) {
            throw new AppException(ErrorCode.CATEGORY_HAS_TICKETS);
        }

        categoryRepository.delete(cat);
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
