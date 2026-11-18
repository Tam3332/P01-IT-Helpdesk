package com.ojt.p01.service;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketAttachment;
import com.ojt.p01.entity.User;
import com.ojt.p01.exception.AppException;
import com.ojt.p01.exception.ErrorCode;
import com.ojt.p01.repository.TicketAttachmentRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit test kiểm tra tệp tải lên (HD04, UC-03, IA-HD03; task T5-13):
 * đổi đuôi, sai magic bytes, quá dung lượng, tệp thứ 6, tên chứa ../ và ghi audit FILE_UPLOAD_REJECTED kèm reason.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttachmentServiceTest - Kiểm tra tệp tải lên và audit lý do từ chối")
class AttachmentServiceTest {

    private static final long TICKET_ID = 990001L;
    private static final String TICKET_NO = "HD-TEST-0001";
    // 16 byte đầu hợp lệ của tệp PNG (chữ ký 89 50 4E 47 0D 0A 1A 0A + IHDR)
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13, 0x49, 0x48, 0x44, 0x52};

    @Mock private TicketAttachmentRepository attachmentRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private TicketHistoryRepository historyRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuditLogger auditLogger;
    @Mock private TicketCommentRepository commentRepository;

    private AttachmentService service;
    private Ticket ticket;
    private User employee;

    @BeforeEach
    void setUp() {
        service = new AttachmentService(attachmentRepository, ticketRepository, historyRepository,
                userRepository, auditLogger, commentRepository);

        Role role = new Role();
        role.setId(1);
        role.setRoleName("EMPLOYEE");
        employee = new User();
        employee.setId(1L);
        employee.setUsername("employee");
        employee.setRole(role);
        employee.setIsActive(true);

        ticket = new Ticket();
        ticket.setId(TICKET_ID);
        ticket.setTicketNumber(TICKET_NO);
        ticket.setStatus("IN_PROGRESS");
        ticket.setRequester(employee);

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "employee", null, List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE"))));

        // lenient: không phải test nào cũng đi tới các bước dùng stub này
        lenient().when(ticketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));
        lenient().when(userRepository.findByUsername("employee")).thenReturn(Optional.of(employee));
        lenient().when(attachmentRepository.countByTicket_Id(TICKET_ID)).thenReturn(0L);
        lenient().when(attachmentRepository.save(any(TicketAttachment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /** Gọi upload và trả mã lỗi (null nếu thành công). */
    private ErrorCode upload(MockMultipartFile file) {
        try {
            service.uploadAttachment(String.valueOf(TICKET_ID), file, null, "127.0.0.1");
            return null;
        } catch (AppException e) {
            return e.getErrorCode();
        }
    }

    /** Kiểm tra đã ghi đúng 1 dòng FILE_UPLOAD_REJECTED với reason và http_status cho trước, không lưu tệp. */
    private void assertRejectedAudit(int httpStatus, String reason) {
        verify(auditLogger, times(1)).logWithReason(eq("FILE_UPLOAD_REJECTED"), eq(1L), eq("EMPLOYEE"),
                eq("TICKET"), eq(TICKET_NO), eq("UPLOAD_ATTACHMENT"), eq("FAILURE"), eq(httpStatus), eq(reason));
        verify(attachmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Đổi đuôi: a.exe (đuôi không cho phép) → 400 FILE_TYPE_NOT_ALLOWED")
    void disallowedExtension_rejected() {
        MockMultipartFile f = new MockMultipartFile("file", "a.exe", "application/octet-stream", "MZ".getBytes());
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, upload(f));
        assertRejectedAudit(400, "FILE_TYPE_NOT_ALLOWED");
    }

    @Test
    @DisplayName("Sai magic bytes: fake.png chứa mã PHP → 400 FILE_TYPE_NOT_ALLOWED")
    void wrongMagicBytes_rejected() {
        MockMultipartFile f = new MockMultipartFile("file", "fake.png", "image/png",
                "<?php echo 1; ?>".getBytes(StandardCharsets.UTF_8));
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, upload(f));
        assertRejectedAudit(400, "FILE_TYPE_NOT_ALLOWED");
    }

    @Test
    @DisplayName("MIME khai báo lệch đuôi: x.png gửi kèm application/pdf → 400 FILE_TYPE_NOT_ALLOWED")
    void declaredMimeMismatch_rejected() {
        MockMultipartFile f = new MockMultipartFile("file", "x.png", "application/pdf", PNG);
        assertEquals(ErrorCode.FILE_TYPE_NOT_ALLOWED, upload(f));
        assertRejectedAudit(400, "FILE_TYPE_NOT_ALLOWED");
    }

    @Test
    @DisplayName("Quá dung lượng: 10 MB + 1 byte → 400 FILE_TOO_LARGE")
    void tooLarge_rejected() {
        byte[] big = new byte[10 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        MockMultipartFile f = new MockMultipartFile("file", "big.png", "image/png", big);
        assertEquals(ErrorCode.FILE_TOO_LARGE, upload(f));
        assertRejectedAudit(400, "FILE_TOO_LARGE");
    }

    @Test
    @DisplayName("Tệp thứ 6: ticket đã có 5 tệp → 400 ATTACHMENT_LIMIT_EXCEEDED")
    void sixthFile_rejected() {
        when(attachmentRepository.countByTicket_Id(TICKET_ID)).thenReturn(5L);
        MockMultipartFile f = new MockMultipartFile("file", "ok.png", "image/png", PNG);
        assertEquals(ErrorCode.ATTACHMENT_LIMIT_EXCEEDED, upload(f));
        assertRejectedAudit(400, "ATTACHMENT_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("Tệp rỗng → 400 VALIDATION_FAILED")
    void emptyFile_rejected() {
        MockMultipartFile f = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);
        assertEquals(ErrorCode.VALIDATION_FAILED, upload(f));
        assertRejectedAudit(400, "VALIDATION_FAILED");
    }

    @Test
    @DisplayName("Ticket Closed → 422 TICKET_CLOSED (BR-HD-04)")
    void closedTicket_rejected() {
        ticket.setStatus("CLOSED");
        MockMultipartFile f = new MockMultipartFile("file", "ok.png", "image/png", PNG);
        assertEquals(ErrorCode.TICKET_CLOSED, upload(f));
        assertRejectedAudit(422, "TICKET_CLOSED");
    }

    @Test
    @DisplayName("Employee khác upload vào ticket không phải của mình → 404, ghi AUTHZ_DENIED, không ghi FILE_UPLOAD_REJECTED")
    void otherEmployee_notFound() {
        User other = new User();
        other.setId(2L);
        ticket.setRequester(other);
        MockMultipartFile f = new MockMultipartFile("file", "ok.png", "image/png", PNG);
        assertEquals(ErrorCode.TICKET_NOT_FOUND, upload(f));
        verify(auditLogger).ticketDenied(eq(employee), eq(TICKET_NO), eq("ACCESS_ATTACHMENT"), eq(404));
        verify(auditLogger, never()).logWithReason(eq("FILE_UPLOAD_REJECTED"), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Tên chứa ../: lưu thành công, tên gốc đã làm sạch, tệp nằm trong thư mục uploads với tên ngẫu nhiên")
    void pathTraversalName_sanitized() throws Exception {
        MockMultipartFile f = new MockMultipartFile("file", "../../evil.png", "image/png", PNG);
        assertNull(upload(f));

        ArgumentCaptor<TicketAttachment> captor = ArgumentCaptor.forClass(TicketAttachment.class);
        verify(attachmentRepository).save(captor.capture());
        TicketAttachment saved = captor.getValue();
        Path stored = Paths.get(saved.getStoragePath()).toAbsolutePath().normalize();
        Path root = Paths.get("uploads").toAbsolutePath().normalize();
        try {
            assertEquals("evil.png", saved.getFileName());
            assertTrue(stored.startsWith(root.resolve(String.valueOf(TICKET_ID))), "tệp phải nằm trong uploads/<ticketId>");
            assertFalse(stored.getFileName().toString().contains("evil"), "tên trên đĩa phải là tên ngẫu nhiên");
            assertTrue(Files.exists(stored));
        } finally {
            // Dọn tệp và thư mục test vừa tạo
            Files.deleteIfExists(stored);
            Files.deleteIfExists(stored.getParent());
        }
    }
}
