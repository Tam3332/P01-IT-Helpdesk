package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test phạm vi tìm kiếm theo role (HD13, task T5-06 "Unit test: search theo role").
 * Cách kiểm: bắt lại Specification mà getTickets gửi xuống repository, chạy nó trên Root/CriteriaBuilder giả
 * rồi xem điều kiện WHERE lọc theo cột nào, giá trị nào. Không cần DB.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // mỗi ca chỉ dùng một phần các đường dẫn cột đã giả lập
@DisplayName("TicketSearchScopeTest - Phạm vi danh sách ticket theo role (HD13)")
@SuppressWarnings({"unchecked", "rawtypes"})
class TicketSearchScopeTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private TicketCategoryRepository categoryRepository;
    @Mock private SLAConfigRepository slaConfigRepository;
    @Mock private UserRepository userRepository;
    @Mock private TicketAssignmentRepository assignmentRepository;
    @Mock private TicketHistoryRepository historyRepository;
    @Spy private TicketStatusTransitionValidator transitionValidator = new TicketStatusTransitionValidator();
    @Mock private AuditLogger auditLogger;

    @InjectMocks private TicketService ticketService;

    // Criteria API giả: root.get("requester").get("id") và root.get("currentAssignee").get("id")
    @Mock private Root<Ticket> root;
    @Mock private CriteriaQuery<?> query;
    @Mock private CriteriaBuilder cb;
    @Mock private Path requesterPath;
    @Mock private Path requesterIdPath;
    @Mock private Path assigneePath;
    @Mock private Path assigneeIdPath;

    @BeforeEach
    void setUp() {
        when(root.get("requester")).thenReturn(requesterPath);
        when(requesterPath.get("id")).thenReturn(requesterIdPath);
        when(root.get("currentAssignee")).thenReturn(assigneePath);
        when(assigneePath.get("id")).thenReturn(assigneeIdPath);
        when(cb.equal(any(), any(Object.class))).thenReturn(mock(Predicate.class));
        when(cb.and(any(Predicate[].class))).thenReturn(mock(Predicate.class));
        when(ticketRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Tạo user với role cho trước (roleName null = tài khoản không có role) và đăng nhập bằng user đó. */
    private void loginAs(long id, String roleName) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setIsActive(true);
        if (roleName != null) {
            Role role = new Role();
            role.setRoleName(roleName);
            user.setRole(role);
        }
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user.getUsername(), "pass", List.of(new SimpleGrantedAuthority("ROLE_" + roleName)));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    /** Gọi getTickets, bắt Specification gửi xuống repository và chạy nó trên Criteria API giả. */
    private void searchWith(Long assigneeFilter) {
        ticketService.getTickets(0, 20, null, null, null, assigneeFilter, null, null, null);
        ArgumentCaptor<Specification> spec = ArgumentCaptor.forClass(Specification.class);
        verify(ticketRepository).findAll(spec.capture(), any(Pageable.class));
        spec.getValue().toPredicate(root, query, cb);
    }

    @Test
    @DisplayName("EMPLOYEE: chỉ lọc requester.id = chính mình, không lọc theo người được giao")
    void employee_onlyOwnTickets() {
        loginAs(1L, "EMPLOYEE");
        searchWith(null);
        verify(cb).equal(requesterIdPath, 1L);
        verify(cb, never()).equal(eq(assigneeIdPath), any(Object.class));
    }

    @Test
    @DisplayName("IT_STAFF: chỉ lọc currentAssignee.id = chính mình, không lọc theo người tạo")
    void staff_onlyAssignedTickets() {
        loginAs(2L, "IT_STAFF");
        searchWith(null);
        verify(cb).equal(assigneeIdPath, 2L);
        verify(cb, never()).equal(eq(requesterIdPath), any(Object.class));
    }

    @Test
    @DisplayName("IT_MANAGER: không có điều kiện phạm vi, thấy toàn hệ thống")
    void manager_noScope() {
        loginAs(3L, "IT_MANAGER");
        searchWith(null);
        verify(root, never()).get("requester");
        verify(root, never()).get("currentAssignee");
    }

    @Test
    @DisplayName("ADMIN: không có điều kiện phạm vi (xem toàn bộ, chỉ đọc)")
    void admin_noScope() {
        loginAs(4L, "ADMIN");
        searchWith(null);
        verify(root, never()).get("requester");
        verify(root, never()).get("currentAssignee");
    }

    @Test
    @DisplayName("EMPLOYEE gửi thêm assigneeId: vẫn giữ requester.id = chính mình, bộ lọc chỉ thu hẹp")
    void employee_filterCannotWidenScope() {
        loginAs(1L, "EMPLOYEE");
        searchWith(7L);
        verify(cb).equal(requesterIdPath, 1L);
        verify(cb).equal(assigneeIdPath, 7L);
    }

    @Test
    @DisplayName("Tài khoản không có role: xử lý như EMPLOYEE, chỉ thấy vé của mình")
    void noRole_treatedAsEmployee() {
        loginAs(5L, null);
        searchWith(null);
        verify(cb).equal(requesterIdPath, 5L);
        verify(cb, never()).equal(eq(assigneeIdPath), any(Object.class));
    }
}
