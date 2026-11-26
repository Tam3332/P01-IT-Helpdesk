package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.AssignTicketRequest;
import com.ojt.p01.dto.request.CreateTicketRequest;
import com.ojt.p01.dto.response.TicketDetailResponse;
import com.ojt.p01.dto.response.TicketResponse;
import com.ojt.p01.entity.ReopenRequest;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.SLAConfig;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketCategory;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.ReopenRequestRepository;
import com.ojt.p01.repository.SLAConfigRepository;
import com.ojt.p01.repository.TicketAssignmentRepository;
import com.ojt.p01.repository.TicketCategoryRepository;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test SLA mặc định khi thiếu SLAConfig (SRS §3 P2 – Exceptions, §7; task T6-06, Trí duyệt 25/11):
 * 3 chỗ tính hạn ticket (tạo, phân công lần đầu, duyệt mở lại) dùng Low 2h/48h thay vì báo lỗi 404;
 * API Admin GET /admin/sla-configs/{priority} vẫn trả 404 SLA_CONFIG_NOT_FOUND (TC-HD-02.16).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // không phải ca nào cũng dùng hết các stub chung
@DisplayName("SlaFallbackTest - SLA mặc định Low 2h/48h khi thiếu cấu hình")
class SlaFallbackTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketCategoryRepository categoryRepository;
    @Mock private SLAConfigRepository slaConfigRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketAssignmentRepository assignmentRepository;
    @Mock private TicketHistoryRepository historyRepository;
    @Mock private ReopenRequestRepository reopenRequestRepository;
    @Mock private TicketStatusTransitionValidator transitionValidator;
    @Mock private AuditLogger auditLogger;

    @InjectMocks private TicketService ticketService;
    @InjectMocks private ReopenService reopenService;
    @InjectMocks private SLAConfigService slaConfigService;

    private User employee, staff, manager;
    private TicketCategory category;

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
        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        return u;
    }

    private void loginAs(User u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                u.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().getRoleName()))));
    }

    /** Ticket sẵn có để phân công / mở lại. */
    private Ticket ticket(String status, String priority) {
        Ticket t = new Ticket();
        t.setId(5L);
        t.setTicketNumber("HD-2026-0005");
        t.setTitle("Máy in không in được");
        t.setDescription("Mô tả");
        t.setCategory(category);
        t.setImpactLevel(priority);
        t.setPriority(priority);
        t.setStatus(status);
        t.setRequester(employee);
        t.setCreatedAt(OffsetDateTime.of(2026, 11, 25, 8, 0, 0, 0, ZoneOffset.UTC));
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(t));
        return t;
    }

    /** Kiểm mốc hạn nằm trong khoảng [trước + giờ, sau + giờ] (thời điểm gọi service nằm giữa trước và sau). */
    private static void assertWithin(OffsetDateTime actual, OffsetDateTime before, OffsetDateTime after, long hours) {
        Duration h = Duration.ofHours(hours);
        assertFalse(actual.isBefore(before.plus(h)), "hạn sớm hơn mong đợi: " + actual);
        assertFalse(actual.isAfter(after.plus(h)), "hạn muộn hơn mong đợi: " + actual);
    }

    @BeforeEach
    void setUp() {
        employee = user(1L, "employee", "EMPLOYEE");
        staff = user(2L, "staff", "IT_STAFF");
        manager = user(3L, "manager", "IT_MANAGER");

        category = new TicketCategory();
        category.setId(1);
        category.setCategoryCode("HARDWARE");
        category.setCategoryName("Phần cứng");
        category.setIsActive(true);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        // Không mức nào có cấu hình, trừ khi ca test tự thêm
        when(slaConfigRepository.findByPriorityLevel(anyString())).thenReturn(Optional.empty());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Tạo ticket CRITICAL khi thiếu cấu hình → vẫn tạo được, hạn = lúc tạo + 2h / + 48h, không ghi gì vào sla_configs")
    void createTicket_missingConfig_usesLowDefault() {
        loginAs(employee);
        OffsetDateTime before = OffsetDateTime.now();
        TicketResponse res = ticketService.createTicket(
                new CreateTicketRequest("Mất mạng toàn tầng", "Không vào được mạng", 1, "CRITICAL"), "10.0.0.1");
        OffsetDateTime after = OffsetDateTime.now();

        assertEquals("CRITICAL", res.priority()); // giữ đúng mức ưu tiên, chỉ mượn số giờ của Low
        assertWithin(res.responseDeadline(), before, after, 2);
        assertWithin(res.resolutionDeadline(), before, after, 48);
        verify(slaConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("Tạo ticket khi CÓ cấu hình → dùng đúng cấu hình (HIGH 0.5h / 8h), không dùng mặc định")
    void createTicket_withConfig_usesConfig() {
        SLAConfig high = new SLAConfig();
        high.setPriorityLevel("HIGH");
        high.setResponseTimeHours(new BigDecimal("0.50"));
        high.setResolutionTimeHours(new BigDecimal("8.00"));
        when(slaConfigRepository.findByPriorityLevel("HIGH")).thenReturn(Optional.of(high));
        loginAs(employee);

        OffsetDateTime before = OffsetDateTime.now();
        TicketResponse res = ticketService.createTicket(
                new CreateTicketRequest("Lỗi phần mềm kế toán", "Không mở được", 1, "HIGH"), "10.0.0.1");
        OffsetDateTime after = OffsetDateTime.now();

        assertFalse(res.responseDeadline().isBefore(before.plusMinutes(30)));
        assertFalse(res.responseDeadline().isAfter(after.plusMinutes(30)));
        assertWithin(res.resolutionDeadline(), before, after, 8);
    }

    @Test
    @DisplayName("Phân công lần đầu, mức chính thức CRITICAL thiếu cấu hình → ASSIGNED, hạn = created_at + 2h / + 48h")
    void assignFirstTime_missingConfig_usesLowDefault() {
        Ticket t = ticket("NEW", "LOW");
        loginAs(manager);

        TicketDetailResponse res = ticketService.assignTicket("5", new AssignTicketRequest(2L, "CRITICAL", null), "10.0.0.2");

        assertEquals("ASSIGNED", res.status());
        assertEquals("CRITICAL", t.getPriority());
        assertEquals(t.getCreatedAt().plusHours(2), t.getResponseDeadline());
        assertEquals(t.getCreatedAt().plusHours(48), t.getResolutionDeadline());
        verify(slaConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("Manager duyệt mở lại, mức của ticket thiếu cấu hình → REOPENED, hạn xử lý mới = lúc duyệt + 48h")
    void approveReopen_missingConfig_usesLowDefault() {
        Ticket t = ticket("REOPEN_PENDING", "CRITICAL");
        ReopenRequest rr = new ReopenRequest(t, employee, "Máy lại lỗi như cũ sau khi đã sửa xong", OffsetDateTime.now());
        rr.setId(9L);
        when(reopenRequestRepository.findById(9L)).thenReturn(Optional.of(rr));
        loginAs(manager);

        OffsetDateTime before = OffsetDateTime.now();
        reopenService.approveReopenRequest(9L, null, "10.0.0.3");
        OffsetDateTime after = OffsetDateTime.now();

        assertEquals("REOPENED", t.getStatus());
        assertEquals(ReopenRequest.STATUS_APPROVED, rr.getStatus());
        assertWithin(t.getResolutionDeadline(), before, after, 48);
    }

    @Test
    @DisplayName("API Admin GET /admin/sla-configs/URGENT (mức không tồn tại) vẫn 404 SLA_CONFIG_NOT_FOUND (TC-HD-02.16)")
    void adminGet_unknownPriority_still404() {
        AppException e = assertThrows(AppException.class, () -> slaConfigService.getSLAConfigByPriority("URGENT"));
        assertEquals(ErrorCode.SLA_CONFIG_NOT_FOUND, e.getErrorCode());
        assertEquals(404, ErrorCode.SLA_CONFIG_NOT_FOUND.getHttpStatus().value());
    }

    @Test
    @DisplayName("Giá trị mặc định: phản hồi 2h, xử lý 48h, ngưỡng 80% / 100%, giữ nguyên tên mức ưu tiên")
    void defaultValues() {
        SLAConfig c = SlaDefaults.fallback("MEDIUM", "TEST");
        assertEquals(0, new BigDecimal("2").compareTo(c.getResponseTimeHours()));
        assertEquals(0, new BigDecimal("48").compareTo(c.getResolutionTimeHours()));
        assertEquals(80, c.getWarningThresholdPct());
        assertEquals(100, c.getBreachThresholdPct());
        assertEquals("MEDIUM", c.getPriorityLevel());
        assertNull(c.getId()); // không phải bản ghi trong CSDL
    }
}
