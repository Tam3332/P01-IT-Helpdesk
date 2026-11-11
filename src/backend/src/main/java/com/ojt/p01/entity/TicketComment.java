package com.ojt.p01.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity mapping bảng ticket_comments.
 */
@Entity
@Table(name = "ticket_comments")
public class TicketComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(name = "comment_text", nullable = false, columnDefinition = "TEXT")
    private String commentText;

    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
        if (this.isInternal == null) {
            this.isInternal = false;
        }
    }

    public TicketComment() {}

    public TicketComment(Ticket ticket, User author, String commentText, Boolean isInternal) {
        this.ticket = ticket;
        this.author = author;
        this.commentText = commentText;
        this.isInternal = isInternal != null ? isInternal : false;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public void setTicket(Ticket ticket) { this.ticket = ticket; }

    public User getAuthor() { return author; }
    public void setAuthor(User author) { this.author = author; }

    public String getCommentText() { return commentText; }
    public void setCommentText(String commentText) { this.commentText = commentText; }

    public Boolean getIsInternal() { return isInternal; }
    public void setIsInternal(Boolean isInternal) { this.isInternal = isInternal; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
