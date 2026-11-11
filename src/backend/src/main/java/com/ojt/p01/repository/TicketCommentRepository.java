package com.ojt.p01.repository;

import com.ojt.p01.entity.TicketComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, Long> {

 /** Toàn bộ bình luận của ticket sắp xếp tăng dần theo thời gian (IT Staff, Manager, Admin). */
    @EntityGraph(attributePaths = {"author", "author.role"})
    List<TicketComment> findByTicket_IdOrderByCreatedAtAsc(Long ticketId);

 /** Chỉ bình luận công khai dành cho Employee (isInternal = false). */
    @EntityGraph(attributePaths = {"author", "author.role"})
    List<TicketComment> findByTicket_IdAndIsInternalFalseOrderByCreatedAtAsc(Long ticketId);
}
