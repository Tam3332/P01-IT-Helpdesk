package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.CreateCommentRequest;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketComment;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketCommentRepository;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import com.ojt.p01.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test phân quyền theo đối tượng cho bình luận (HD06, SecRS IA-HD02 / PC-03; task T6-06 phần RBAC).
 * Ticket HD-2026-0005: người tạo = employee, người được giao = staff, đang IN_PROGRESS.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // không phải ca nào cũng dùng hết các stub chung
@DisplayName("CommentServiceTest - Phân quyền xem và gửi bình luận")
class CommentServiceTest {

    private static final String TICKET_NO = "HD-2026-0005";

    @Mock private TicketCommentRepository commentRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketHistoryRepository historyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogger auditLogger;

    @InjectMocks
    private CommentService commentService;

    private User employee, employee2, staff, staff2, manager, admin;
    private Ticket ticket;

    private User user(long id, String username, String roleName) {
        Role role = new Role();
        role.setRoleName(roleName);
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        u.setFullName(username);
        u.setRole(role);
        u.setIsActive(true);
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(u));
        return u;
    }

    @BeforeEach
    void setUp() {
        employee = user(1L, "employee", "EMPLOYEE");
        employee2 = user(10L, "employee2", "EMPLOYEE");
        staff = user(2L, "staff", "IT_STAFF");
        staff2 = user(7L, "staff2", "IT_STAFF");
        manager = user(3L, "manager", "IT_MANAGER");
        admin = user(4L, "admin", "ADMIN");

        ticket = new Ticket();
        ticket.setId(5L);
        ticket.setTicketNumber(TICKET_NO);
        ticket.setStatus("IN_PROGRESS");
        ticket.setRequester(employee);
        ticket.setCurrentAssignee(staff);
        when(ticketRepository.findById(5L)).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(TicketComment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Đăng nhập giả: SecurityContext chứa username, service tự đọc lại User từ repository. */
    private void loginAs(User u) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                u.getUsername(), null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().getRoleName()))));
    }

    private ErrorCode addError(String text, Boolean internal) {
        AppException e = assertThrows(AppException.class,
                () -> commentService.addComment("5", new CreateCommentRequest(text, internal), "10.0.0.1"));
        return e.getErrorCode();
    }

    @Test
    @DisplayName("Employee khác xem bình luận → 404 TICKET_NOT_FOUND, ghi AUTHZ_DENIED VIEW_COMMENTS")
    void otherEmployee_view_404() {
        loginAs(employee2);
        AppException e = assertThrows(AppException.class, () -> commentService.getComments("5"));
        assertEquals(ErrorCode.TICKET_NOT_FOUND, e.getErrorCode());
        verify(auditLogger).ticketDenied(employee2, TICKET_NO, "VIEW_COMMENTS", 404);
        verifyNoInteractions(commentRepository);
    }

    @Test
    @DisplayName("Employee khác gửi bình luận → 404, ghi AUTHZ_DENIED ADD_COMMENT, không lưu")
    void otherEmployee_add_404() {
        loginAs(employee2);
        assertEquals(ErrorCode.TICKET_NOT_FOUND, addError("Bình luận thử", false));
        verify(auditLogger).ticketDenied(employee2, TICKET_NO, "ADD_COMMENT", 404);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("IT Staff không được giao: xem và gửi đều 404 (Anti-IDOR), không lưu")
    void unassignedStaff_404() {
        loginAs(staff2);
        AppException e = assertThrows(AppException.class, () -> commentService.getComments("5"));
        assertEquals(ErrorCode.TICKET_NOT_FOUND, e.getErrorCode());
        assertEquals(ErrorCode.TICKET_NOT_FOUND, addError("Bình luận thử", true));
        verify(auditLogger).ticketDenied(staff2, TICKET_NO, "VIEW_COMMENTS", 404);
        verify(auditLogger).ticketDenied(staff2, TICKET_NO, "ADD_COMMENT", 404);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Admin gửi bình luận → 403 FORBIDDEN (chỉ đọc), ghi AUTHZ_DENIED 403, không lưu")
    void admin_add_403() {
        loginAs(admin);
        assertEquals(ErrorCode.FORBIDDEN, addError("Admin thử bình luận", false));
        assertEquals(403, ErrorCode.FORBIDDEN.getHttpStatus().value());
        verify(auditLogger).ticketDenied(admin, TICKET_NO, "ADD_COMMENT", 403);
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Chủ ticket bình luận khi ticket CLOSED → 422 TICKET_CLOSED (BR-HD-04), không lưu")
    void owner_closedTicket_422() {
        ticket.setStatus("CLOSED");
        loginAs(employee);
        assertEquals(ErrorCode.TICKET_CLOSED, addError("Bình luận sau khi đóng", false));
        assertEquals(422, ErrorCode.TICKET_CLOSED.getHttpStatus().value());
        verify(commentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Employee khác bình luận vào ticket CLOSED → vẫn 404, không phải 422 (kiểm quyền trước trạng thái)")
    void otherEmployee_closedTicket_stillNotFound() {
        ticket.setStatus("CLOSED");
        loginAs(employee2);
        assertEquals(ErrorCode.TICKET_NOT_FOUND, addError("Thử dò ticket", false));
    }

    @Test
    @DisplayName("Employee gửi isInternal = true → lưu thành bình luận công khai; IT Staff được giao giữ ghi chú nội bộ")
    void internalFlag_onlyForItRoles() {
        ArgumentCaptor<TicketComment> saved = ArgumentCaptor.forClass(TicketComment.class);

        loginAs(employee);
        commentService.addComment("5", new CreateCommentRequest("  Máy vẫn lỗi  ", true), "10.0.0.1");
        loginAs(staff);
        commentService.addComment("5", new CreateCommentRequest("Đã kiểm tra ổ cứng", true), "10.0.0.2");

        verify(commentRepository, times(2)).save(saved.capture());
        assertFalse(saved.getAllValues().get(0).getIsInternal());
        assertEquals("Máy vẫn lỗi", saved.getAllValues().get(0).getCommentText());
        assertTrue(saved.getAllValues().get(1).getIsInternal());
        verify(auditLogger, times(2)).logAfterCommit(eq("COMMENT_ADDED"), anyLong(), anyString(), eq("TICKET"),
                eq(TICKET_NO), eq("ADD_COMMENT"), eq("SUCCESS"), eq(201), isNull(), any());
    }

    @Test
    @DisplayName("Employee chỉ đọc bình luận công khai; IT Manager đọc cả ghi chú nội bộ")
    void visibility_employeePublicOnly_managerAll() {
        loginAs(employee);
        commentService.getComments("5");
        verify(commentRepository).findByTicket_IdAndIsInternalFalseOrderByCreatedAtAsc(5L);
        verify(commentRepository, never()).findByTicket_IdOrderByCreatedAtAsc(anyLong());

        loginAs(manager);
        commentService.getComments("5");
        verify(commentRepository).findByTicket_IdOrderByCreatedAtAsc(5L);
    }
}
