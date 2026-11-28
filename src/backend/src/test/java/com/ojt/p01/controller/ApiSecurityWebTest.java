package com.ojt.p01.controller;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.config.SecurityConfig;
import com.ojt.p01.dto.response.LoginResponse;
import com.ojt.p01.dto.response.UserResponse;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TokenBlacklistRepository;
import com.ojt.p01.repository.UserRepository;
import com.ojt.p01.security.JwtAuthenticationFilter;
import com.ojt.p01.security.JwtTokenProvider;
import com.ojt.p01.service.AttachmentService;
import com.ojt.p01.service.AuthService;
import com.ojt.p01.service.SLAConfigService;
import com.ojt.p01.service.TicketService;
import com.ojt.p01.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test tầng HTTP (task T6-06): chạy SecurityConfig, JwtAuthenticationFilter, JwtTokenProvider,
 * kiểm tra @Valid và GlobalExceptionHandler THẬT; service và repository là bản giả (không cần Postgres).
 * Mục tiêu: các lỗi chỉ xảy ra ở tầng HTTP mà unit test gọi thẳng service không thấy được
 * (thiếu/sai/hết hạn token → 401, sai role theo SecurityConfig → 403, body sai → 400, định dạng lỗi JSON chung).
 */
@WebMvcTest(controllers = {AuthController.class, TicketController.class, AdminUserController.class,
        SLAConfigController.class, TicketAttachmentController.class})
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class})
@DisplayName("ApiSecurityWebTest - Xác thực, phân quyền và kiểm tra dữ liệu ở tầng HTTP")
class ApiSecurityWebTest {

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Value("${jwt.secret}") private String jwtSecret;

    @MockBean private AuthService authService;
    @MockBean private TicketService ticketService;
    @MockBean private UserService userService;
    @MockBean private SLAConfigService slaConfigService;
    @MockBean private AttachmentService attachmentService;
    @MockBean private UserRepository userRepository;
    @MockBean private TokenBlacklistRepository tokenBlacklistRepository;
    @MockBean private AuditLogger auditLogger;

    private User employee, staff, manager, admin;

    private User user(long id, String username, String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setEmail(username + "@helpdesk.local");
        u.setFullName(username);
        u.setRole(role);
        u.setIsActive(true);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(u));
        return u;
    }

    @BeforeEach
    void setUp() {
        employee = user(1L, "employee", "EMPLOYEE");
        staff = user(2L, "staff", "IT_STAFF");
        manager = user(3L, "manager", "IT_MANAGER");
        admin = user(4L, "admin", "ADMIN");
        when(tokenBlacklistRepository.existsByJti(anyString())).thenReturn(false);
    }

    /** Header Authorization với token thật, ký bằng khóa của ứng dụng. */
    private String bearer(User u) {
        return "Bearer " + jwtTokenProvider.generateToken(u);
    }

    // ---------- 401: xác thực ----------

    @Test
    @DisplayName("Không gửi token → 401, body JSON chung success=false, error.code=UNAUTHORIZED")
    void noToken_401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        verify(authService, never()).getCurrentUser();
    }

    @Test
    @DisplayName("Token ký bằng khóa khác (giả mạo) → 401")
    void forgedToken_401() throws Exception {
        JwtTokenProvider other = new JwtTokenProvider("khoa-khac-hoan-toan-dung-de-thu-chu-ky-sai-32b", 900_000);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + other.generateToken(employee)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token hết hạn → 401")
    void expiredToken_401() throws Exception {
        JwtTokenProvider expired = new JwtTokenProvider(jwtSecret, -60_000); // hạn = 1 phút trước
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + expired.generateToken(employee)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token đã logout (jti trong blacklist) → 401")
    void blacklistedToken_401() throws Exception {
        when(tokenBlacklistRepository.existsByJti(anyString())).thenReturn(true);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(employee)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Tài khoản bị khóa sau khi đã có token → token cũ bị từ chối 401")
    void lockedAccount_oldToken_401() throws Exception {
        String token = bearer(employee);
        employee.setIsActive(false);
        mvc.perform(get("/api/v1/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token hợp lệ → 200, trả đúng người dùng")
    void validToken_200() throws Exception {
        when(authService.getCurrentUser()).thenReturn(UserResponse.from(employee));
        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("employee@helpdesk.local"));
    }

    @Test
    @DisplayName("POST /auth/login không cần token (API public duy nhất) → 200")
    void login_isPublic() throws Exception {
        when(authService.login(any())).thenReturn(new LoginResponse("tok", "Bearer", 900, UserResponse.from(employee)));
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"employee@helpdesk.local\",\"password\":\"Any@Pass2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("tok"));
    }

    // ---------- 403: phân quyền theo SecurityConfig ----------

    @Test
    @DisplayName("Employee gọi /admin/users → 403 FORBIDDEN, ghi AUTHZ_DENIED kèm method + URL")
    void employee_adminApi_403() throws Exception {
        mvc.perform(get("/api/v1/admin/users").header("Authorization", bearer(employee)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
        verify(auditLogger).logForCurrentUser(eq("AUTHZ_DENIED"), eq("ENDPOINT"), eq("GET /api/v1/admin/users"),
                eq("GET"), eq("DENIED"), eq(403));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("IT Staff phân công ticket (PUT /tickets/{id}/assign) → 403, service không được gọi")
    void staff_assign_403() throws Exception {
        mvc.perform(put("/api/v1/tickets/5/assign").header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ticketService);
    }

    @Test
    @DisplayName("IT Manager tạo ticket (POST /tickets) → 403, chỉ Employee được tạo")
    void manager_createTicket_403() throws Exception {
        mvc.perform(post("/api/v1/tickets").header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"description\":\"D\",\"categoryId\":1,\"impactLevel\":\"LOW\"}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(ticketService);
    }

    @Test
    @DisplayName("SLA config: IT Manager được xem (200) nhưng không được sửa (403); Admin sửa được")
    void slaConfig_managerReadOnly() throws Exception {
        when(slaConfigService.getAllSLAConfigs()).thenReturn(List.of());
        mvc.perform(get("/api/v1/admin/sla-configs").header("Authorization", bearer(manager)))
                .andExpect(status().isOk());
        String body = "{\"responseTimeHours\":1,\"resolutionTimeHours\":8,\"warningThresholdPct\":80,\"breachThresholdPct\":100}";
        mvc.perform(put("/api/v1/admin/sla-configs/HIGH").header("Authorization", bearer(manager))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        verify(slaConfigService, never()).updateSLAConfig(anyString(), any());
    }

    @Test
    @DisplayName("Admin tải tệp lên ticket → 403 (Admin chỉ đọc), service không được gọi")
    void admin_upload_403() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        mvc.perform(multipart("/api/v1/tickets/5/attachments").file(file).header("Authorization", bearer(admin)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(attachmentService);
    }

    // ---------- 400: kiểm tra dữ liệu đầu vào ----------

    @Test
    @DisplayName("Login thiếu email và mật khẩu → 400 VALIDATION_FAILED kèm details, service không được gọi")
    void login_blankFields_400() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error.details").exists());
        verify(authService, never()).login(any());
    }

    @Test
    @DisplayName("Employee tạo ticket tiêu đề trống, impactLevel sai → 400 VALIDATION_FAILED, service không được gọi")
    void createTicket_invalidBody_400() throws Exception {
        mvc.perform(post("/api/v1/tickets").header("Authorization", bearer(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"description\":\"May loi\",\"categoryId\":1,\"impactLevel\":\"URGENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
        verifyNoInteractions(ticketService);
    }

    // ---------- Định dạng lỗi nghiệp vụ ----------

    @Test
    @DisplayName("Service ném TICKET_NOT_FOUND → HTTP 404 với error.code đúng mã (GlobalExceptionHandler)")
    void appException_mappedToStatusAndCode() throws Exception {
        when(ticketService.getTicketDetail("999")).thenThrow(new AppException(ErrorCode.TICKET_NOT_FOUND));
        mvc.perform(get("/api/v1/tickets/999").header("Authorization", bearer(employee)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TICKET_NOT_FOUND"));
    }
}
