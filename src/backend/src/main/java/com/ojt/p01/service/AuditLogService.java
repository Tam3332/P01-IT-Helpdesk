package com.ojt.p01.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.p01.dto.response.AuditLogResponse;
import com.ojt.p01.dto.response.PageResponse;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service đọc và tra cứu nhật ký kiểm toán bất biến (Audit Logs) từ audit_events.jsonl.
 */
@Slf4j
@Service
public class AuditLogService {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final String configuredLogFile;

    public AuditLogService(UserRepository userRepository,
                           ObjectMapper objectMapper,
                           @Value("${audit.log-file:logs/audit_events.jsonl}") String configuredLogFile) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.configuredLogFile = configuredLogFile;
    }

    /**
     * Lấy danh sách nhật ký hệ thống có phân trang và bộ lọc (Admin).
     */
    public PageResponse<AuditLogResponse> getAuditLogs(int page, int size, String search, String result) {
        Path logPath = resolveLogFile();
        if (!Files.exists(logPath)) {
            return new PageResponse<>(Collections.emptyList(), page, size, 0, 0, true);
        }

        // 1. Tải danh sách người dùng để map actor_id sang fullName
        Map<Long, String> userNames = new HashMap<>();
        try {
            for (User u : userRepository.findAll()) {
                userNames.put(u.getId(), u.getFullName());
            }
        } catch (Exception e) {
            log.warn("Không tải được danh sách user cho audit log: {}", e.getMessage());
        }

        // 2. Đọc tất cả các dòng từ file JSON Lines
        List<String> rawLines = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(logPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    rawLines.add(line);
                }
            }
        } catch (IOException e) {
            log.error("Lỗi khi đọc file audit log {}: {}", logPath, e.getMessage());
            return new PageResponse<>(Collections.emptyList(), page, size, 0, 0, true);
        }

        // 3. Đảo ngược thứ tự: sự kiện mới nhất lên trước
        Collections.reverse(rawLines);

        // 4. Parse và convert sang AuditLogResponse
        List<AuditLogResponse> allLogs = new ArrayList<>();
        long virtualId = rawLines.size();

        String needle = (search != null) ? search.trim().toLowerCase() : "";
        String resFilter = (result != null) ? result.trim().toLowerCase() : "";

        TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};

        for (String raw : rawLines) {
            try {
                Map<String, Object> map = objectMapper.readValue(raw, typeRef);

                String timestamp = (String) map.get("timestamp");
                Long actorId = map.get("actor_id") != null ? Long.valueOf(map.get("actor_id").toString()) : null;
                String role = (String) map.get("role");

                String actorName;
                if (actorId != null && userNames.containsKey(actorId)) {
                    actorName = userNames.get(actorId);
                } else if (role != null) {
                    actorName = role;
                } else {
                    actorName = "Hệ thống";
                }

                String outcome = (String) map.getOrDefault("outcome", "");
                String targetType = (String) map.getOrDefault("target_type", "");
                String targetId = map.get("target_id") != null ? map.get("target_id").toString() : "";
                String action = formatAction(map);
                String target = formatTarget(targetType, targetId);

                // Kiểm tra bộ lọc kết quả
                if ("ok".equals(resFilter) && !"SUCCESS".equalsIgnoreCase(outcome)) {
                    virtualId--;
                    continue;
                }
                if ("fail".equals(resFilter) && "SUCCESS".equalsIgnoreCase(outcome)) {
                    virtualId--;
                    continue;
                }

                // Kiểm tra từ khóa tìm kiếm (họ tên, hành động, đối tượng)
                if (!needle.isEmpty()) {
                    String fullSearchText = (actorName + " " + action + " " + target + " " + targetType + " " + targetId).toLowerCase();
                    if (!fullSearchText.contains(needle)) {
                        virtualId--;
                        continue;
                    }
                }

                String srcIp = (String) map.get("src_ip");
                String correlationId = (String) map.get("correlation_id");
                Integer httpStatus = map.get("http_status") != null ? Integer.valueOf(map.get("http_status").toString()) : null;
                Object before = map.get("before");
                Object after = map.get("after");

                allLogs.add(new AuditLogResponse(
                        virtualId--,
                        timestamp,
                        timestamp, // createdAt
                        actorId,
                        actorName,
                        actorName,
                        role,
                        action,
                        target,
                        targetType,
                        targetId,
                        outcome, // result
                        outcome,
                        srcIp,
                        correlationId,
                        httpStatus,
                        before,
                        after
                ));
            } catch (Exception e) {
                // Bỏ qua dòng bị lỗi định dạng
                virtualId--;
            }
        }

        // 5. Phân trang
        int totalElements = allLogs.size();
        int pageSize = Math.max(size, 1);
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int currentPage = Math.max(page, 0);

        int fromIndex = Math.min(currentPage * pageSize, totalElements);
        int toIndex = Math.min(fromIndex + pageSize, totalElements);

        List<AuditLogResponse> content = allLogs.subList(fromIndex, toIndex);
        boolean isLast = (currentPage >= totalPages - 1) || (totalElements == 0);

        return new PageResponse<>(content, currentPage, pageSize, totalElements, totalPages, isLast);
    }

    private Path resolveLogFile() {
        Path path = Path.of(configuredLogFile);
        if (Files.exists(path)) return path;

        Path sub = Path.of("src/backend", configuredLogFile);
        if (Files.exists(sub)) return sub;

        Path parent = Path.of("..", configuredLogFile);
        if (Files.exists(parent)) return parent;

        return path;
    }

    private String formatAction(Map<String, Object> map) {
        String eventType = (String) map.get("event_type");
        String action = (String) map.get("action");
        String outcome = (String) map.get("outcome");

        if ("AUTHZ_DENIED".equalsIgnoreCase(eventType)) {
            return "Từ chối quyền truy cập (" + action + ")";
        }
        if ("LOGIN".equalsIgnoreCase(action)) {
            return "SUCCESS".equalsIgnoreCase(outcome) ? "Đăng nhập thành công" : "Đăng nhập thất bại";
        }
        if ("CREATE".equalsIgnoreCase(action) || "TICKET_CREATED".equalsIgnoreCase(eventType)) {
            return "Tạo phiếu yêu cầu";
        }
        if ("CHANGE_PASSWORD".equalsIgnoreCase(action) || "PASSWORD_CHANGED".equalsIgnoreCase(eventType)) {
            return "Đổi mật khẩu";
        }
        if ("ADMIN_RESET_PASSWORD".equalsIgnoreCase(eventType) || "RESET_PASSWORD".equalsIgnoreCase(action)) {
            return "Đặt lại mật khẩu (Admin)";
        }
        if ("REOPEN_APPROVE".equalsIgnoreCase(action) || "REOPEN_APPROVED".equalsIgnoreCase(eventType)) {
            return "Duyệt mở lại ticket";
        }
        if ("REOPEN_REQUEST".equalsIgnoreCase(action) || "REOPEN_REQUESTED".equalsIgnoreCase(eventType)) {
            return "Gửi yêu cầu mở lại ticket";
        }
        if ("CLOSE_TICKET".equalsIgnoreCase(action)) {
            return "Đóng phiếu hoàn tất";
        }
        if ("ADD_COMMENT".equalsIgnoreCase(action)) {
            return "Thêm bình luận";
        }
        if ("VIEW_TICKET".equalsIgnoreCase(action)) {
            return "Xem chi tiết ticket";
        }
        if ("VIEW_COMMENTS".equalsIgnoreCase(action)) {
            return "Xem bình luận ticket";
        }
        if ("VIEW_HISTORY".equalsIgnoreCase(action)) {
            return "Xem lịch sử xử lý";
        }
        if ("SLA_MONITOR".equalsIgnoreCase(action) || "SLA_BREACHED".equalsIgnoreCase(eventType)) {
            return "Cảnh báo vi phạm SLA";
        }
        if (action != null && !action.isBlank()) {
            return action;
        }
        return eventType != null ? eventType : "Thao tác hệ thống";
    }

    private String formatTarget(String targetType, String targetId) {
        if ("TICKET".equalsIgnoreCase(targetType)) {
            return "Ticket " + targetId;
        }
        if ("USER".equalsIgnoreCase(targetType)) {
            return "Tài khoản #" + targetId;
        }
        if ("ENDPOINT".equalsIgnoreCase(targetType)) {
            return targetId;
        }
        if (targetType != null && !targetType.isBlank()) {
            return targetType + (targetId != null && !targetId.isBlank() ? " " + targetId : "");
        }
        return targetId != null ? targetId : "—";
    }
}
