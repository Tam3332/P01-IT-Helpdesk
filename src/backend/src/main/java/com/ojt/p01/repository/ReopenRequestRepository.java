package com.ojt.p01.repository;

import com.ojt.p01.entity.ReopenRequest;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReopenRequestRepository extends JpaRepository<ReopenRequest, Long> {

 /** Kiểm tra ticket đã có yêu cầu đang chờ duyệt chưa. */
    boolean existsByTicket_IdAndStatus(Long ticketId, String status);

 /** Danh sách cho IT Manager; nạp sẵn ticket, requester, reviewedBy để tránh N+1 khi map DTO. */
    @EntityGraph(attributePaths = {"ticket", "requester", "reviewedBy"})
    List<ReopenRequest> findByStatusOrderByCreatedAtAsc(String status);
}
