package com.ojt.p01.service;

import com.ojt.p01.dto.request.UpdateSLAConfigRequest;
import com.ojt.p01.dto.response.SLAConfigResponse;
import com.ojt.p01.entity.SLAConfig;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.SLAConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý nghiệp vụ SLA Config (UC-09, BR-HD-03).
 *
 * BR-HD-03: Giờ SLA là giờ liên tục (không phải giờ làm việc).
 * Quyền sửa: ADMIN only. IT_MANAGER chỉ xem.
 */
@Service
public class SLAConfigService {

    private final SLAConfigRepository slaConfigRepository;

    public SLAConfigService(SLAConfigRepository slaConfigRepository) {
        this.slaConfigRepository = slaConfigRepository;
    }

    // -------------------------------------------------------------------------
    // Read — IT_MANAGER + ADMIN
    // -------------------------------------------------------------------------

    /** Trả về toàn bộ 4 cấu hình SLA (theo thứ tự CRITICAL → HIGH → MEDIUM → LOW). */
    @Transactional(readOnly = true)
    public List<SLAConfigResponse> getAllSLAConfigs() {
        return slaConfigRepository.findAllOrderByPriority()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Xem cấu hình SLA theo priority level. */
    @Transactional(readOnly = true)
    public SLAConfigResponse getSLAConfigByPriority(String priority) {
        SLAConfig config = slaConfigRepository.findByPriorityLevel(priority.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.SLA_CONFIG_NOT_FOUND));
        return toResponse(config);
    }

    // -------------------------------------------------------------------------
    // Write — ADMIN only
    // -------------------------------------------------------------------------

    /**
     * UC-10 / BR-HD-03: Cập nhật giờ SLA theo priority.
     * Validation: responseTimeHours và resolutionTimeHours phải > 0.
     * Giờ SLA là giờ liên tục (24/7).
     */
    @Transactional
    public SLAConfigResponse updateSLAConfig(String priority, UpdateSLAConfigRequest request) {
        SLAConfig config = slaConfigRepository.findByPriorityLevel(priority.toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.SLA_CONFIG_NOT_FOUND));

        // Double-check business rule (dù @DecimalMin đã validate từ Controller)
        if (request.responseTimeHours().doubleValue() <= 0
                || request.resolutionTimeHours().doubleValue() <= 0) {
            throw new AppException(ErrorCode.SLA_INVALID_HOURS);
        }

        config.setResponseTimeHours(request.responseTimeHours());
        config.setResolutionTimeHours(request.resolutionTimeHours());
        config.setWarningThresholdPct(request.warningThresholdPct());
        config.setBreachThresholdPct(request.breachThresholdPct());

        return toResponse(slaConfigRepository.save(config));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private SLAConfigResponse toResponse(SLAConfig c) {
        return new SLAConfigResponse(
                c.getId(),
                c.getPriorityLevel(),
                c.getResponseTimeHours(),
                c.getResolutionTimeHours(),
                c.getWarningThresholdPct(),
                c.getBreachThresholdPct(),
                c.getUpdatedAt()
        );
    }
}
