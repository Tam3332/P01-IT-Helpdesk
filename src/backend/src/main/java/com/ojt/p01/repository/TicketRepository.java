package com.ojt.p01.repository;

import com.ojt.p01.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    @Override
    @EntityGraph(attributePaths = {"category", "requester", "currentAssignee"})
    Optional<Ticket> findById(Long id);

    @EntityGraph(attributePaths = {"category", "requester", "currentAssignee"})
    Optional<Ticket> findByTicketNumber(String ticketNumber);

    @Override
    @EntityGraph(attributePaths = {"category", "requester", "currentAssignee"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    boolean existsByTicketNumber(String ticketNumber);

 /** Kiểm tra danh mục có ticket nào đang tham chiếu không */
    boolean existsByCategory_Id(Integer categoryId);

 /** Đếm số ticket trong năm để sinh mã HD-YYYY-XXXX tiếp theo */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.ticketNumber LIKE :yearPrefix%")
    long countByYearPrefix(@Param("yearPrefix") String yearPrefix);

 /** Ticket của Employee (màn hình "Ticket của tôi") */
    @EntityGraph(attributePaths = {"category", "requester", "currentAssignee"})
    Page<Ticket> findByRequester_IdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

 /** Ticket được giao cho IT Staff */
    @EntityGraph(attributePaths = {"category", "requester", "currentAssignee"})
    Page<Ticket> findByCurrentAssignee_IdOrderByCreatedAtDesc(Long assigneeId, Pageable pageable);

 /** Lấy danh sách ticket cho Manager: mới tạo (NEW), mở lại (REOPEN_PENDING/REOPENED), vi phạm SLA (isSlaBreached), sự cố khẩn cấp (CRITICAL/HIGH) */
    @Query("SELECT t FROM Ticket t " +
           "JOIN FETCH t.requester " +
           "WHERE t.status = 'NEW' " +
           "   OR t.status = 'REOPEN_PENDING' " +
           "   OR t.status = 'REOPENED' " +
           "   OR (t.isSlaBreached = true AND t.status NOT IN ('RESOLVED', 'CLOSED')) " +
           "   OR (t.priority IN ('CRITICAL', 'HIGH') AND t.status NOT IN ('RESOLVED', 'CLOSED')) " +
           "ORDER BY t.updatedAt DESC NULLS LAST, t.createdAt DESC")
    List<Ticket> findNotificationsForManager(Pageable pageable);

 /** Thông báo giám sát toàn cục cho Admin: vé vi phạm SLA, vé sự cố khẩn cấp (CRITICAL / HIGH) */
    @Query("SELECT t FROM Ticket t " +
           "JOIN FETCH t.requester " +
           "WHERE t.isSlaBreached = true " +
           "   OR t.priority = 'CRITICAL' " +
           "   OR t.priority = 'HIGH' " +
           "ORDER BY t.updatedAt DESC NULLS LAST, t.createdAt DESC")
    List<Ticket> findNotificationsForAdmin(Pageable pageable);

 /** Lấy danh sách ticket mở phục vụ SLA Background Worker quét định kỳ */
    @EntityGraph(attributePaths = {"category", "requester", "currentAssignee"})
    List<Ticket> findByStatusIn(List<String> statuses);
}
