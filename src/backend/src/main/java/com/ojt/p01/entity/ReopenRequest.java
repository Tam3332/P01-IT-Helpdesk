package com.ojt.p01.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

/**
 * Yêu cầu mở lại ticket (bảng reopen_requests, HD10, UC-08 Nhánh B, BR-HD-04).
 * Mỗi ticket tối đa 1 yêu cầu PENDING (partial unique uq_ticket_pending_reopen trong schema.sql).
 * reviewedBy / reviewNote / reviewedAt do bước IT Manager duyệt hoặc từ chối điền (chưa triển khai).
 */
@Entity
@Table(name = "reopen_requests")
@Getter
@Setter
@NoArgsConstructor
public class ReopenRequest {

    public static final String STATUS_PENDING = "PENDING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "reopen_reason", nullable = false, columnDefinition = "TEXT")
    private String reopenReason;

    /** PENDING | APPROVED | REJECTED (CHECK constraint trong schema.sql) */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    public ReopenRequest(Ticket ticket, User requester, String reopenReason, OffsetDateTime createdAt) {
        this.ticket = ticket;
        this.requester = requester;
        this.reopenReason = reopenReason;
        this.status = STATUS_PENDING;
        this.createdAt = createdAt;
    }
}
