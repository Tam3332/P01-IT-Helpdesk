package com.ojt.p01.service;

import com.ojt.p01.dto.response.DashboardStatsResponse;
import com.ojt.p01.dto.response.SLAMonitorResponse;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketCategory;
import com.ojt.p01.entity.User;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User manager;
    private TicketCategory hardwareCategory;
    private User staff;

    @BeforeEach
    void setUp() {
        Role managerRole = new Role();
        managerRole.setId(3);
        managerRole.setRoleName("IT_MANAGER");

        manager = new User();
        manager.setId(2L);
        manager.setUsername("manager");
        manager.setFullName("Lê Minh Cường");
        manager.setRole(managerRole);

        Role staffRole = new Role();
        staffRole.setId(2);
        staffRole.setRoleName("IT_STAFF");

        staff = new User();
        staff.setId(3L);
        staff.setUsername("staff");
        staff.setFullName("Hoàng Văn Nam");
        staff.setRole(staffRole);

        hardwareCategory = new TicketCategory();
        hardwareCategory.setId(1);
        hardwareCategory.setCategoryName("Phần cứng");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("manager", "password")
        );
        lenient().when(userRepository.findByUsername("manager")).thenReturn(Optional.of(manager));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Thống kê Dashboard trả về đúng số lượng vé, phân loại trạng thái, danh mục và người xử lý")
    void getDashboardStats_calculatesAccurately() {
        OffsetDateTime now = OffsetDateTime.now();

        // Ticket 1: NEW, bình thường
        Ticket t1 = new Ticket();
        t1.setId(1L);
        t1.setTicketNumber("HD-2026-0001");
        t1.setStatus("NEW");
        t1.setCategory(hardwareCategory);
        t1.setCreatedAt(now.minusHours(1));
        t1.setResolutionDeadline(now.plusHours(7));
        t1.setIsSlaBreached(false);

        // Ticket 2: IN_PROGRESS, quá hạn
        Ticket t2 = new Ticket();
        t2.setId(2L);
        t2.setTicketNumber("HD-2026-0002");
        t2.setStatus("IN_PROGRESS");
        t2.setCategory(hardwareCategory);
        t2.setCurrentAssignee(staff);
        t2.setCreatedAt(now.minusHours(10));
        t2.setResolutionDeadline(now.minusHours(2));
        t2.setIsSlaBreached(true);

        // Ticket 3: RESOLVED, giải quyết đúng hạn
        Ticket t3 = new Ticket();
        t3.setId(3L);
        t3.setTicketNumber("HD-2026-0003");
        t3.setStatus("RESOLVED");
        t3.setCategory(hardwareCategory);
        t3.setCurrentAssignee(staff);
        t3.setCreatedAt(now.minusHours(5));
        t3.setResolvedAt(now.minusHours(1));
        t3.setResolutionDeadline(now.plusHours(3));
        t3.setIsSlaBreached(false);

        when(ticketRepository.findAll(any(Specification.class))).thenReturn(List.of(t1, t2, t3));

        DashboardStatsResponse stats = dashboardService.getDashboardStats();

        assertNotNull(stats);
        assertEquals(3, stats.totalTickets());
        assertEquals(1, stats.newTickets());
        assertEquals(1, stats.inProgressTickets());
        assertEquals(1, stats.resolvedTickets());
        assertEquals(1, stats.slaBreachedCount());
        assertTrue(stats.slaBreachedPct() > 0);
        assertEquals(3L, stats.byCategory().get("Phần cứng"));
        assertEquals(2L, stats.byAssignee().get("Hoàng Văn Nam"));
        assertEquals(1L, stats.byAssignee().get("Chưa phân công"));
    }

    @Test
    @DisplayName("Giám sát SLA: Tính toán % thời gian, gán nhãn BREACHED/NEAR_BREACH/NORMAL và lọc breachedOnly")
    void getSlaMonitor_evaluatesLabelsAndFilters() {
        OffsetDateTime now = OffsetDateTime.now();

        // Ticket 1: Normal (mới tạo 10% thời gian)
        Ticket t1 = new Ticket();
        t1.setId(1L);
        t1.setTicketNumber("HD-2026-0001");
        t1.setTitle("Hỗ trợ bàn phím");
        t1.setPriority("LOW");
        t1.setStatus("ASSIGNED");
        t1.setCreatedAt(now.minusHours(1));
        t1.setResolutionDeadline(now.plusHours(9));
        t1.setIsSlaBreached(false);

        // Ticket 2: Near Breach (đã trôi 90% thời gian)
        Ticket t2 = new Ticket();
        t2.setId(2L);
        t2.setTicketNumber("HD-2026-0002");
        t2.setTitle("Mạng chập chờn");
        t2.setPriority("HIGH");
        t2.setStatus("IN_PROGRESS");
        t2.setCreatedAt(now.minusHours(9));
        t2.setResolutionDeadline(now.plusHours(1));
        t2.setIsSlaBreached(false);

        // Ticket 3: Breached (đã quá hạn)
        Ticket t3 = new Ticket();
        t3.setId(3L);
        t3.setTicketNumber("HD-2026-0003");
        t3.setTitle("Máy chủ sập");
        t3.setPriority("CRITICAL");
        t3.setStatus("IN_PROGRESS");
        t3.setCreatedAt(now.minusHours(6));
        t3.setResolutionDeadline(now.minusHours(2));
        t3.setIsSlaBreached(true);

        when(ticketRepository.findAll(any(Specification.class))).thenReturn(List.of(t1, t2, t3));

        // 1. Kiểm tra lấy toàn bộ
        List<SLAMonitorResponse> allItems = dashboardService.getSlaMonitor(false);
        assertEquals(3, allItems.size());
        assertEquals("HD-2026-0003", allItems.get(0).ticketNumber()); // Ưu tiên vé quá hạn % cao nhất lên đầu
        assertEquals("BREACHED", allItems.get(0).slaLabel());
        assertEquals("NEAR_BREACH", allItems.get(1).slaLabel());
        assertEquals("NORMAL", allItems.get(2).slaLabel());

        // 2. Kiểm tra lọc chỉ lấy vé vi phạm (breachedOnly = true)
        List<SLAMonitorResponse> breachedOnly = dashboardService.getSlaMonitor(true);
        assertEquals(1, breachedOnly.size());
        assertEquals("HD-2026-0003", breachedOnly.get(0).ticketNumber());
        assertTrue(breachedOnly.get(0).isSlaBreached());
    }
}
