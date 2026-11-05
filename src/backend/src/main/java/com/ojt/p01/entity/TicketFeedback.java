package com.ojt.p01.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Đánh giá mức độ hài lòng khách hàng CSAT (bảng ticket_feedbacks, HD15).
 */
@Entity
@Table(name = "ticket_feedbacks")
@Getter
@Setter
@NoArgsConstructor
public class TicketFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, unique = true)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "rating_stars", nullable = false)
    private Integer ratingStars;

    @Column(name = "feedback_comment", length = 500)
    private String feedbackComment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public TicketFeedback(Ticket ticket, User requester, Integer ratingStars, String feedbackComment) {
        this.ticket = ticket;
        this.requester = requester;
        this.ratingStars = ratingStars;
        this.feedbackComment = feedbackComment;
        this.createdAt = OffsetDateTime.now();
    }
}
