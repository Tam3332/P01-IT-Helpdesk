package com.ojt.p01.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Gán correlation_id cho mỗi request để nối các dòng audit của cùng 1 request với nhau.
 *
 * Ưu tiên header X-Correlation-Id; nếu không có thì dùng X-Request-Id do Nginx gửi (khớp "rid" trong access log Nginx)
 * để nối dòng log Nginx với dòng audit của cùng request. Không có/không hợp lệ thì tự sinh UUID.
 * Chạy trước Spring Security (HIGHEST_PRECEDENCE) để cả AccessDeniedHandler cũng có id.
 * Trả lại id trong header phản hồi để đối chiếu khi debug.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String NGINX_HEADER = "X-Request-Id";
    public static final String ATTRIBUTE = "audit.correlationId";

    // Chỉ chấp nhận chữ, số, gạch nối, tối đa 64 ký tự: tránh chèn ký tự lạ vào file log
    private static final Pattern VALID_ID = Pattern.compile("[A-Za-z0-9-]{1,64}");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String id = request.getHeader(HEADER);
        if (id == null || !VALID_ID.matcher(id).matches()) {
            id = request.getHeader(NGINX_HEADER);
        }
        if (id == null || !VALID_ID.matcher(id).matches()) {
            id = UUID.randomUUID().toString();
        }
        request.setAttribute(ATTRIBUTE, id);
        response.setHeader(HEADER, id);
        filterChain.doFilter(request, response);
    }
}
