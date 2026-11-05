package com.ojt.p01.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Ghi Security Audit Log dạng JSON Lines (SDD §7, IA-HD04): mỗi sự kiện là 1 dòng JSON trong audit_events.jsonl.
 *
 * - Log riêng với bảng ticket_history (SDD §7: "Security Audit Log được lưu riêng với bảng ticket_history").
 * - Trường theo SDD §7: timestamp (UTC), event_type, actor_id, role, target_type, target_id, action, outcome,
 *   src_ip, correlation_id, http_status.
 * - KHÔNG truyền mật khẩu, JWT hay token vào bất kỳ trường nào (SDD §7).
 * - Ghi file trên 1 luồng riêng để không chặn request (SDD, thành phần Audit Logger: "Non-blocking async"); lỗi ghi log chỉ in ra console, không làm hỏng API.
 * - Đường dẫn file đọc từ audit.log-file (biến môi trường AUDIT_LOG_FILE) để gắn vào log volume khi chạy Docker.
 */
@Component
public class AuditLogger {

    private static final Logger log = LoggerFactory.getLogger(AuditLogger.class);

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final Path logFile;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "audit-writer");
        t.setDaemon(true);
        return t;
    });

    public AuditLogger(ObjectMapper objectMapper,
                       UserRepository userRepository,
                       @Value("${audit.log-file:logs/audit_events.jsonl}") String logFile) {
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.logFile = Path.of(logFile);
    }

    /**
     * Ghi 1 sự kiện. IP nguồn và correlation_id lấy từ request hiện tại (phải gọi trên luồng xử lý request).
     *
     * @param actorId    id người thực hiện; null nếu chưa xác định (vd đăng nhập bằng email không tồn tại)
     * @param role       role người thực hiện; null nếu chưa xác định
     * @param targetType loại đối tượng: USER, USER_EMAIL, TICKET, ENDPOINT
     * @param targetId   định danh đối tượng (id, mã ticket, email đăng nhập, method + URL)
     * @param outcome    SUCCESS / FAILURE / DENIED
     */
    public void log(String eventType, Long actorId, String role, String targetType, String targetId,
                    String action, String outcome, Integer httpStatus) {
        HttpServletRequest request = currentRequest();

        // LinkedHashMap để các trường luôn ra đúng thứ tự trong file, dễ đọc khi đối chiếu
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("timestamp", Instant.now().toString());   // ISO-8601 UTC, vd 2026-11-05T05:30:00.123Z
        event.put("event_type", eventType);
        event.put("actor_id", actorId);
        event.put("role", role);
        event.put("target_type", targetType);
        event.put("target_id", targetId);
        event.put("action", action);
        event.put("outcome", outcome);
        event.put("src_ip", request != null ? request.getRemoteAddr() : null);
        event.put("correlation_id", correlationId(request));
        event.put("http_status", httpStatus);

        String line;
        try {
            line = objectMapper.writeValueAsString(event);
        } catch (IOException e) {
            log.error("Không tạo được dòng audit {}: {}", eventType, e.getMessage());
            return;
        }
        writer.submit(() -> append(line));
    }

    /**
     * Ghi sự kiện cho người đang đăng nhập (lấy từ SecurityContext).
     * Dùng ở chỗ chỉ có token mà chưa có đối tượng User, vd AccessDeniedHandler của Spring Security.
     */
    public void logForCurrentUser(String eventType, String targetType, String targetId,
                                  String action, String outcome, Integer httpStatus) {
        Long actorId = null;
        String role = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            User user = userRepository.findByUsername(auth.getName()).orElse(null);
            if (user != null) {
                actorId = user.getId();
                role = user.getRole() != null ? user.getRole().getRoleName() : null;
            }
        }
        log(eventType, actorId, role, targetType, targetId, action, outcome, httpStatus);
    }

    private void append(String line) {
        try {
            Path parent = logFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(logFile, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.error("Không ghi được audit log vào {}: {}", logFile.toAbsolutePath(), e.getMessage());
        }
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String correlationId(HttpServletRequest request) {
        if (request != null && request.getAttribute(CorrelationIdFilter.ATTRIBUTE) instanceof String id) {
            return id;
        }
        return UUID.randomUUID().toString();
    }

    /** Khi tắt ứng dụng: ghi nốt các dòng còn trong hàng đợi. */
    @PreDestroy
    public void shutdown() {
        writer.shutdown();
    }
}
