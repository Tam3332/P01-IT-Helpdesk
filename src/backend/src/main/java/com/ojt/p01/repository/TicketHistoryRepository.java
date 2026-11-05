package com.ojt.p01.repository;

import com.ojt.p01.entity.TicketHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    /** Lấy toàn bộ lịch sử xử lý của ticket theo thực tế thời gian tăng dần (HD09, Timeline FE). */
    List<TicketHistory> findByTicket_IdOrderByTimestampAsc(Long ticketId);

    /**
     * 1. Thông báo cho STAFF: Chỉ hiện ticket khi Manager phân công cho Staff đó.
     */
    @Query("SELECT h FROM TicketHistory h " +
           "JOIN FETCH h.ticket t " +
           "JOIN FETCH h.actor a " +
           "WHERE h.fieldChanged = 'current_assignee_id' " +
           "  AND (h.action = 'ASSIGN' OR h.action = 'REASSIGN') " +
           "  AND h.newValue = :userIdStr " +
           "ORDER BY h.timestamp DESC")
    List<TicketHistory> findNotificationsForStaff(@Param("userIdStr") String userIdStr, Pageable pageable);

    /**
     * 2. Thông báo cho EMPLOYEE: Chỉ hiện ticket khi chuyển sang trạng thái RESOLVED (Staff nhấn Submit giải quyết).
     */
    @Query("SELECT h FROM TicketHistory h " +
           "JOIN FETCH h.ticket t " +
           "JOIN FETCH h.actor a " +
           "WHERE t.requester.id = :userId " +
           "  AND h.fieldChanged = 'status' " +
           "  AND h.newValue = 'RESOLVED' " +
           "  AND a.id != :userId " +
           "ORDER BY h.timestamp DESC")
    List<TicketHistory> findNotificationsForEmployee(@Param("userId") Long userId, Pageable pageable);

    /**
     * 3. Thông báo cho MANAGER: Chỉ hiện khi có ticket mới tạo (CREATE) hoặc mở lại (REOPEN).
     */
    @Query("SELECT h FROM TicketHistory h " +
           "JOIN FETCH h.ticket t " +
           "JOIN FETCH h.actor a " +
           "WHERE h.action = 'CREATE' " +
           "   OR h.action = 'REOPEN' " +
           "   OR (h.fieldChanged = 'status' AND (h.newValue = 'REOPENED' OR h.newValue = 'REOPEN_PENDING')) " +
           "ORDER BY h.timestamp DESC")
    List<TicketHistory> findNotificationsForManager(Pageable pageable);
}
