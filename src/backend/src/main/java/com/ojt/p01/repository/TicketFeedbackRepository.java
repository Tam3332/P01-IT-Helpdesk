package com.ojt.p01.repository;

import com.ojt.p01.entity.TicketFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TicketFeedbackRepository extends JpaRepository<TicketFeedback, Long> {

    Optional<TicketFeedback> findByTicket_Id(Long ticketId);

    boolean existsByTicket_Id(Long ticketId);
}
