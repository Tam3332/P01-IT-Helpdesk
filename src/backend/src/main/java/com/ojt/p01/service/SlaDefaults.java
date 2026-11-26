package com.ojt.p01.service;

import com.ojt.p01.entity.SLAConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * Cấu hình SLA mặc định an toàn khi bảng sla_configs thiếu dòng cho một mức ưu tiên
 * (SRS §3 P2 – Exceptions, SRS §7 SLAConfig): dùng mức Low 2h phản hồi / 48h xử lý và ghi System Log.
 *
 * Chỉ dùng cho 3 chỗ tính hạn ticket: tạo ticket, phân công lần đầu, duyệt mở lại
 * (để hệ thống không chặn người báo sự cố khi thiếu cấu hình).
 * API Admin GET/PUT /admin/sla-configs/{priority} vẫn trả 404 SLA_CONFIG_NOT_FOUND (TC-HD-02.16).
 * Đối tượng trả về chỉ dùng để đọc giờ, không lưu vào CSDL.
 */
final class SlaDefaults {

    static final BigDecimal RESPONSE_HOURS = new BigDecimal("2.00");
    static final BigDecimal RESOLUTION_HOURS = new BigDecimal("48.00");

    private static final Logger log = LoggerFactory.getLogger(SlaDefaults.class);

    private SlaDefaults() {}

    /**
     * Trả cấu hình mặc định và ghi cảnh báo để vận hành biết bảng sla_configs đang thiếu dữ liệu.
     * @param priority mức ưu tiên đang thiếu cấu hình
     * @param action   thao tác đang tính hạn (CREATE_TICKET, ASSIGN_TICKET, APPROVE_REOPEN)
     */
    static SLAConfig fallback(String priority, String action) {
        log.warn("[SLA] Thiếu SLAConfig cho mức {} khi {}: dùng mặc định Low {}h/{}h (SRS §3 P2). Kiểm tra bảng sla_configs.",
                priority, action, RESPONSE_HOURS, RESOLUTION_HOURS);
        SLAConfig c = new SLAConfig();
        c.setPriorityLevel(priority);
        c.setResponseTimeHours(RESPONSE_HOURS);
        c.setResolutionTimeHours(RESOLUTION_HOURS);
        c.setWarningThresholdPct(80);
        c.setBreachThresholdPct(100);
        return c;
    }
}
