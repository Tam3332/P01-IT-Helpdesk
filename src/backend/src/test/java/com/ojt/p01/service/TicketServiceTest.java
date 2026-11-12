package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.dto.request.AssignTicketRequest;
import com.ojt.p01.dto.request.UpdateTicketStatusRequest;
import com.ojt.p01.dto.response.TicketDetailResponse;
import com.ojt.p01.entity.*;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketServiceTest - Kiểm thử các Quy tắc nghiệp vụ (Business Rules)")
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketCategoryRepository categoryRepository;

    @Mock
    private SLAConfigRepository slaConfigRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TicketAssignmentRepository assignmentRepository;

    @Mock
    private TicketHistoryRepository historyRepository;

    @Spy
    private TicketStatusTransitionValidator transitionValidator = new TicketStatusTransitionValidator();

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private TicketService ticketService;

    private User employeeUser;
    private User otherEmployeeUser;
    private User itStaffUser;
    private User itStaffUser2;
    private User itManagerUser;
    private Ticket ticket;
    private TicketCategory category;

    @BeforeEach
    void setUp() {
        Role employeeRole = new Role();
        employeeRole.setId(1);
        employeeRole.setRoleName("EMPLOYEE");

        Role itStaffRole = new Role();
        itStaffRole.setId(2);
        itStaffRole.setRoleName("IT_STAFF");

        Role itManagerRole = new Role();
        itManagerRole.setId(3);
        itManagerRole.setRoleName("IT_MANAGER");

        employeeUser = new User();
        employeeUser.setId(1L);
        employeeUser.setUsername("employee1");
        employeeUser.setFullName("Nguyen Van An");
        employeeUser.setRole(employeeRole);
        employeeUser.setIsActive(true);

        otherEmployeeUser = new User();
        otherEmployeeUser.setId(99L);
        otherEmployeeUser.setUsername("employee2");
        otherEmployeeUser.setFullName("Le Thi Khac");
        otherEmployeeUser.setRole(employeeRole);
        otherEmployeeUser.setIsActive(true);

        itStaffUser = new User();
        itStaffUser.setId(2L);
        itStaffUser.setUsername("it.staff");
        itStaffUser.setFullName("Tran Thi Binh");
        itStaffUser.setRole(itStaffRole);
        itStaffUser.setIsActive(true);

        itStaffUser2 = new User();
        itStaffUser2.setId(5L);
        itStaffUser2.setUsername("it.staff2");
        itStaffUser2.setFullName("Hoang Van Nam");
        itStaffUser2.setRole(itStaffRole);
        itStaffUser2.setIsActive(true);

        itManagerUser = new User();
        itManagerUser.setId(3L);
        itManagerUser.setUsername("it.manager");
        itManagerUser.setFullName("Le Minh Cuong");
        itManagerUser.setRole(itManagerRole);
        itManagerUser.setIsActive(true);

        category = new TicketCategory();
        category.setId(1);
        category.setCategoryName("Phan mem");

        ticket = new Ticket();
        ticket.setId(10L);
        ticket.setTicketNumber("HD-2026-0001");
        ticket.setTitle("Lỗi máy in");
        ticket.setDescription("Không in được tài liệu");
        ticket.setCategory(category);
        ticket.setImpactLevel("HIGH");
        ticket.setPriority("HIGH");
        ticket.setStatus("IN_PROGRESS");
        ticket.setRequester(employeeUser);
        ticket.setCurrentAssignee(itStaffUser);
        ticket.setCreatedAt(OffsetDateTime.now().minusHours(2));
        ticket.setResponseDeadline(OffsetDateTime.now().plusHours(1));
        ticket.setResolutionDeadline(OffsetDateTime.now().plusHours(6));
        ticket.setReopenCount(0);
        ticket.setIsSlaBreached(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(User user) {
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().getRoleName())
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(user.getUsername(), "pass", authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        lenient().when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
    }

    // =========================================================================
    // BR-HD-01: Bắt buộc giải pháp khi RESOLVED phải >= 20 ký tự
    // =========================================================================
    @Nested
    @DisplayName("BR-HD-01: Bắt buộc giải pháp kỹ thuật khi hoàn tất xử lý (resolutionNote >= 20 ký tự)")
    class BrHd01Test {

        @Test
        @DisplayName("Cập nhật RESOLVED thất bại nếu resolutionNote null hoặc ngắn hơn 20 ký tự")
        void shouldThrowExceptionWhenResolutionNoteIsTooShort() {
            authenticateAs(itStaffUser);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            UpdateTicketStatusRequest requestShort = new UpdateTicketStatusRequest("RESOLVED", "Da sua xong");

            assertThatThrownBy(() -> ticketService.updateTicketStatus("10", requestShort, "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.RESOLUTION_NOTE_REQUIRED);
                    });

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cập nhật RESOLVED thất bại nếu resolutionNote rỗng hoặc chỉ có khoảng trắng")
        void shouldThrowExceptionWhenResolutionNoteIsBlank() {
            authenticateAs(itStaffUser);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            UpdateTicketStatusRequest requestBlank = new UpdateTicketStatusRequest("RESOLVED", "                     ");

            assertThatThrownBy(() -> ticketService.updateTicketStatus("10", requestBlank, "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.RESOLUTION_NOTE_REQUIRED);
                    });

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Cập nhật RESOLVED thành công khi resolutionNote >= 20 ký tự hợp lệ")
        void shouldSucceedWhenResolutionNoteIsValid() {
            authenticateAs(itStaffUser);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            String validNote = "Đã thay cáp kết nối USB và cài đặt lại driver máy in.";
            UpdateTicketStatusRequest requestValid = new UpdateTicketStatusRequest("RESOLVED", validNote);

            TicketDetailResponse response = ticketService.updateTicketStatus("10", requestValid, "127.0.0.1");

            assertThat(response).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo("RESOLVED");
            assertThat(ticket.getResolutionNote()).isEqualTo(validNote);
            assertThat(ticket.getResolvedAt()).isNotNull();

            verify(ticketRepository).save(ticket);
            verify(historyRepository).save(any(TicketHistory.class));
        }
    }

    // =========================================================================
    // BR-HD-02: Chỉ Employee chủ vé mới được đóng vé / mở lại vé
    // =========================================================================
    @Nested
    @DisplayName("BR-HD-02: Quyền đóng vé (Chỉ Employee chủ vé hoặc IT Manager)")
    class BrHd02Test {

        @Test
        @DisplayName("Employee khác không phải chủ vé cố đóng vé -> ném 404 Anti-IDOR")
        void shouldDenyWhenOtherEmployeeTriesToCloseTicket() {
            authenticateAs(otherEmployeeUser);
            ticket.setStatus("RESOLVED");
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> ticketService.closeTicket("10", "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.TICKET_NOT_FOUND);
                    });

            verify(auditLogger).ticketDenied(otherEmployeeUser, ticket.getTicketNumber(), "CLOSE_TICKET", 404);
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("IT Staff cố đóng vé -> ném 403 FORBIDDEN")
        void shouldDenyWhenItStaffTriesToCloseTicket() {
            authenticateAs(itStaffUser);
            ticket.setStatus("RESOLVED");
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> ticketService.closeTicket("10", "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
                    });

            verify(auditLogger).ticketDenied(itStaffUser, ticket.getTicketNumber(), "CLOSE_TICKET", 403);
            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Đóng vé thất bại nếu vé chưa ở trạng thái RESOLVED")
        void shouldFailWhenTicketIsNotResolved() {
            authenticateAs(employeeUser);
            ticket.setStatus("IN_PROGRESS");
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            assertThatThrownBy(() -> ticketService.closeTicket("10", "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.TICKET_NOT_RESOLVED);
                    });

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Chính Employee chủ vé đóng vé thành công khi trạng thái là RESOLVED")
        void shouldAllowOwnerToCloseResolvedTicket() {
            authenticateAs(employeeUser);
            ticket.setStatus("RESOLVED");
            ticket.setResolvedAt(OffsetDateTime.now().minusMinutes(30));
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.closeTicket("10", "127.0.0.1");

            assertThat(response).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo("CLOSED");
            assertThat(ticket.getClosedAt()).isNotNull();

            verify(ticketRepository).save(ticket);
            verify(historyRepository).save(any(TicketHistory.class));
        }

        @Test
        @DisplayName("IT Manager đóng vé thành công khi trạng thái là RESOLVED")
        void shouldAllowManagerToCloseResolvedTicket() {
            authenticateAs(itManagerUser);
            ticket.setStatus("RESOLVED");
            ticket.setResolvedAt(OffsetDateTime.now().minusMinutes(30));
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            TicketDetailResponse response = ticketService.closeTicket("10", "127.0.0.1");

            assertThat(response).isNotNull();
            assertThat(ticket.getStatus()).isEqualTo("CLOSED");
            assertThat(ticket.getClosedAt()).isNotNull();

            verify(ticketRepository).save(ticket);
        }
    }

    // =========================================================================
    // BR-HD-04: Chặn không cho sửa ticket khi đã CLOSED
    // =========================================================================
    @Nested
    @DisplayName("BR-HD-04: Chặn không cho sửa đổi ticket khi đã CLOSED")
    class BrHd04Test {

        @Test
        @DisplayName("Không thể cập nhật trạng thái khi ticket đã CLOSED")
        void shouldRejectStatusUpdateOnClosedTicket() {
            authenticateAs(itStaffUser);
            ticket.setStatus("CLOSED");
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            UpdateTicketStatusRequest request = new UpdateTicketStatusRequest("IN_PROGRESS", null);

            assertThatThrownBy(() -> ticketService.updateTicketStatus("10", request, "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
                    });

            verify(ticketRepository, never()).save(any());
        }

        @Test
        @DisplayName("Không thể phân công / điều phối lại khi ticket đã CLOSED")
        void shouldRejectAssignOnClosedTicket() {
            authenticateAs(itManagerUser);
            ticket.setStatus("CLOSED");
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));

            AssignTicketRequest request = new AssignTicketRequest(itStaffUser2.getId(), "HIGH", "Giao lai");

            assertThatThrownBy(() -> ticketService.assignTicket("10", request, "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.TICKET_NOT_ASSIGNABLE);
                    });

            verify(assignmentRepository, never()).save(any());
        }
    }

    // =========================================================================
    // BR-HD-05: Mỗi ticket chỉ có duy nhất 1 phân công có is_current = true
    // =========================================================================
    @Nested
    @DisplayName("BR-HD-05: Duy nhất 1 người phụ trách chính (is_current = true)")
    class BrHd05Test {

        @Test
        @DisplayName("Điều phối lại: phải flush đóng assignment cũ trước khi lưu assignment mới")
        void shouldEndAndFlushOldAssignmentBeforeCreatingNewOne() {
            authenticateAs(itManagerUser);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(itStaffUser2.getId())).thenReturn(Optional.of(itStaffUser2));

            TicketAssignment oldAssignment = new TicketAssignment(
                    ticket, itStaffUser, itManagerUser, OffsetDateTime.now().minusDays(1), "Phan cong cu"
            );
            when(assignmentRepository.findByTicket_IdAndIsCurrentTrue(ticket.getId()))
                    .thenReturn(Optional.of(oldAssignment));

            AssignTicketRequest request = new AssignTicketRequest(itStaffUser2.getId(), "HIGH", "Dieu phoi sang staff 2");

            ticketService.assignTicket("10", request, "127.0.0.1");

            // Kiểm tra phân công cũ bị đóng
            assertThat(oldAssignment.getIsCurrent()).isFalse();
            assertThat(oldAssignment.getUnassignedAt()).isNotNull();
            verify(assignmentRepository).saveAndFlush(oldAssignment);

            // Kiểm tra phân công mới được lưu với is_current = true
            ArgumentCaptor<TicketAssignment> captor = ArgumentCaptor.forClass(TicketAssignment.class);
            verify(assignmentRepository).save(captor.capture());
            TicketAssignment newAssignment = captor.getValue();

            assertThat(newAssignment.getAssignee().getId()).isEqualTo(itStaffUser2.getId());
            assertThat(newAssignment.getIsCurrent()).isTrue();
            assertThat(ticket.getCurrentAssignee()).isEqualTo(itStaffUser2);
        }

        @Test
        @DisplayName("Phân công lại cho cùng một IT Staff mà không đổi priority -> ném lỗi ASSIGNEE_UNCHANGED")
        void shouldRejectReassignToSameStaffWithoutPriorityChange() {
            authenticateAs(itManagerUser);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
            when(userRepository.findById(itStaffUser.getId())).thenReturn(Optional.of(itStaffUser));

            AssignTicketRequest request = new AssignTicketRequest(itStaffUser.getId(), "HIGH", "Giao lai nguoi cu");

            assertThatThrownBy(() -> ticketService.assignTicket("10", request, "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.ASSIGNEE_UNCHANGED);
                    });

            verify(assignmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("Phân công cho người không phải IT Staff -> ném lỗi INVALID_ASSIGNEE")
        void shouldRejectAssignToNonStaffUser() {
            authenticateAs(itManagerUser);
            when(ticketRepository.findById(10L)).thenReturn(Optional.of(ticket));
            // Gán cho employeeUser thay vì IT Staff
            when(userRepository.findById(employeeUser.getId())).thenReturn(Optional.of(employeeUser));

            AssignTicketRequest request = new AssignTicketRequest(employeeUser.getId(), "HIGH", "Giao nham");

            assertThatThrownBy(() -> ticketService.assignTicket("10", request, "127.0.0.1"))
                    .isInstanceOf(AppException.class)
                    .satisfies(e -> {
                        AppException appEx = (AppException) e;
                        assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.INVALID_ASSIGNEE);
                    });

            verify(assignmentRepository, never()).save(any());
        }
    }
}
