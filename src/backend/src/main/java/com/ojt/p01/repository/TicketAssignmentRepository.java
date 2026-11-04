package com.ojt.p01.repository;

import com.ojt.p01.entity.TicketAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketAssignmentRepository extends JpaRepository<TicketAssignment, Long> {

    /** Lần phân công đang hiệu lực của ticket (tối đa 1 dòng, BR-HD-05). */
    Optional<TicketAssignment> findByTicket_IdAndIsCurrentTrue(Long ticketId);
}
