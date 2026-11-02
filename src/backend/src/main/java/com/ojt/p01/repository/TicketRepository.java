package com.ojt.p01.repository;

import com.ojt.p01.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    boolean existsByTicketNumber(String ticketNumber);

    /** Đếm số ticket trong năm để sinh mã HD-YYYY-XXXX tiếp theo */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.ticketNumber LIKE :yearPrefix%")
    long countByYearPrefix(@Param("yearPrefix") String yearPrefix);

    /** Ticket của Employee (màn hình "Ticket của tôi") */
    Page<Ticket> findByRequester_IdOrderByCreatedAtDesc(Long requesterId, Pageable pageable);

    /** Ticket được giao cho IT Staff */
    Page<Ticket> findByCurrentAssignee_IdOrderByCreatedAtDesc(Long assigneeId, Pageable pageable);
}
