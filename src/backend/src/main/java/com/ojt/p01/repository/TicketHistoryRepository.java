package com.ojt.p01.repository;

import com.ojt.p01.entity.TicketHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketHistoryRepository extends JpaRepository<TicketHistory, Long> {

    /** Lấy toàn bộ lịch sử xử lý của ticket theo thứ tự thời gian tăng dần (HD09, Timeline FE). */
    List<TicketHistory> findByTicket_IdOrderByTimestampAsc(Long ticketId);
}
