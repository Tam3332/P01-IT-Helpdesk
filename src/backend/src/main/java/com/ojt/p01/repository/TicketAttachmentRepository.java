package com.ojt.p01.repository;

import com.ojt.p01.entity.TicketAttachment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    /** Toàn bộ tệp đính kèm của ticket. */
    @EntityGraph(attributePaths = {"uploader"})
    List<TicketAttachment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

    /** Đếm số tệp đính kèm hiện tại của ticket để kiểm tra giới hạn <= 5 tệp (IA-HD03). */
    long countByTicket_Id(Long ticketId);
}
