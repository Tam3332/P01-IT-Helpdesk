package com.ojt.p01.worker;

import com.ojt.p01.audit.AuditLogger;
import com.ojt.p01.entity.Role;
import com.ojt.p01.entity.Ticket;
import com.ojt.p01.entity.TicketHistory;
import com.ojt.p01.entity.User;
import com.ojt.p01.repository.TicketHistoryRepository;
import com.ojt.p01.repository.TicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaBackgroundWorkerTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TicketHistoryRepository historyRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private SlaBackgroundWorker slaBackgroundWorker;

    @Test
    @DisplayName("SLA Worker phát hiện vé quá hạn: Bật cờ isSlaBreached=true, ghi TicketHistory và Security Audit Log SLA_BREACHED")
    void scanAndCheckSla_detectsBreach_updatesTicketAndLogs() {
        OffsetDateTime now = OffsetDateTime.now();

        Role staffRole = new Role();
        staffRole.setId(2);
        staffRole.setRoleName("IT_STAFF");

        User staff = new User();
        staff.setId(10L);
        staff.setFullName("Nguyễn Kỹ Thuật");
        staff.setRole(staffRole);

        // Vé 1: Đang mở và đã quá hạn xử lý (deadline trước thời điểm hiện tại 1 giờ)
        Ticket overdueTicket = new Ticket();
        overdueTicket.setId(100L);
        overdueTicket.setTicketNumber("HD-2026-0100");
        overdueTicket.setStatus("IN_PROGRESS");
        overdueTicket.setCurrentAssignee(staff);
        overdueTicket.setCreatedAt(now.minusHours(5));
        overdueTicket.setResolutionDeadline(now.minusHours(1));
        overdueTicket.setIsSlaBreached(false);

        // Vé 2: Đang mở và còn hạn xử lý
        Ticket normalTicket = new Ticket();
        normalTicket.setId(101L);
        normalTicket.setTicketNumber("HD-2026-0101");
        normalTicket.setStatus("IN_PROGRESS");
        normalTicket.setCurrentAssignee(staff);
        normalTicket.setCreatedAt(now.minusHours(1));
        normalTicket.setResolutionDeadline(now.plusHours(3));
        normalTicket.setIsSlaBreached(false);

        when(ticketRepository.findByStatusIn(anyList())).thenReturn(List.of(overdueTicket, normalTicket));

        // Thực thi worker
        slaBackgroundWorker.scanAndCheckSla();

        // 1. Kiểm tra cập nhật vé
        assertTrue(overdueTicket.getIsSlaBreached());
        assertFalse(normalTicket.getIsSlaBreached());
        verify(ticketRepository, times(1)).save(overdueTicket);
        verify(ticketRepository, never()).save(normalTicket);

        // 2. Kiểm tra lưu TicketHistory
        ArgumentCaptor<TicketHistory> historyCaptor = ArgumentCaptor.forClass(TicketHistory.class);
        verify(historyRepository, times(1)).save(historyCaptor.capture());
        TicketHistory savedHistory = historyCaptor.getValue();
        assertEquals("SLA_BREACH", savedHistory.getAction());
        assertEquals("is_sla_breached", savedHistory.getFieldChanged());
        assertEquals("true", savedHistory.getNewValue());

        // 3. Kiểm tra ghi Security Audit Log SLA_BREACHED
        verify(auditLogger, times(1)).log(
                eq("SLA_BREACHED"),
                eq(10L),
                eq("IT_STAFF"),
                eq("TICKET"),
                eq("HD-2026-0100"),
                eq("SLA_CHECK"),
                eq("BREACHED"),
                eq(200),
                anyMap(),
                anyMap()
        );
    }
}
