package com.ojt.p01.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity mapping bảng tickets — trung tâm nghiệp vụ Helpdesk.
 * Bám sát schema.sql (BR-HD-03, BR-HD-04).
 */
@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã vé: HD-YYYY-XXXX (ví dụ HD-2026-0001) — sinh bởi TicketService */
    @Column(name = "ticket_number", nullable = false, unique = true, length = 30)
    private String ticketNumber;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private TicketCategory category;

    /** Mức độ ảnh hưởng: LOW | MEDIUM | HIGH | CRITICAL */
    @Column(name = "impact_level", nullable = false, length = 20)
    private String impactLevel;

    /** Mức ưu tiên xử lý: LOW | MEDIUM | HIGH | CRITICAL */
    @Column(name = "priority", nullable = false, length = 20)
    private String priority;

    /** Trạng thái: NEW | ASSIGNED | IN_PROGRESS | RESOLVED | CLOSED | REOPENED | REOPEN_PENDING */
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    /** Người tạo ticket (Employee) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    /** Người IT Staff đang xử lý (nullable) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_assignee_id")
    private User currentAssignee;

    // SLA Deadlines (BR-HD-03)
    @Column(name = "response_deadline", nullable = false)
    private OffsetDateTime responseDeadline;

    @Column(name = "resolution_deadline", nullable = false)
    private OffsetDateTime resolutionDeadline;

    // Timestamps sự kiện
    @Column(name = "first_responded_at")
    private OffsetDateTime firstRespondedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "reopened_at")
    private OffsetDateTime reopenedAt;

    // Reopen tracking (BR-HD-04)
    @Column(name = "reopen_count", nullable = false)
    private Integer reopenCount;

    @Column(name = "reopen_reason", columnDefinition = "TEXT")
    private String reopenReason;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    /** Cờ SLA vi phạm — KHÔNG bao giờ reset về false */
    @Column(name = "is_sla_breached", nullable = false)
    private Boolean isSlaBreached;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) this.status = "NEW";
        if (this.reopenCount == null) this.reopenCount = 0;
        if (this.isSlaBreached == null) this.isSlaBreached = false;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Ticket() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicketNumber() { return ticketNumber; }
    public void setTicketNumber(String ticketNumber) { this.ticketNumber = ticketNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TicketCategory getCategory() { return category; }
    public void setCategory(TicketCategory category) { this.category = category; }

    public String getImpactLevel() { return impactLevel; }
    public void setImpactLevel(String impactLevel) { this.impactLevel = impactLevel; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User getRequester() { return requester; }
    public void setRequester(User requester) { this.requester = requester; }

    public User getCurrentAssignee() { return currentAssignee; }
    public void setCurrentAssignee(User currentAssignee) { this.currentAssignee = currentAssignee; }

    public OffsetDateTime getResponseDeadline() { return responseDeadline; }
    public void setResponseDeadline(OffsetDateTime responseDeadline) { this.responseDeadline = responseDeadline; }

    public OffsetDateTime getResolutionDeadline() { return resolutionDeadline; }
    public void setResolutionDeadline(OffsetDateTime resolutionDeadline) { this.resolutionDeadline = resolutionDeadline; }

    public OffsetDateTime getFirstRespondedAt() { return firstRespondedAt; }
    public void setFirstRespondedAt(OffsetDateTime firstRespondedAt) { this.firstRespondedAt = firstRespondedAt; }

    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }

    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime closedAt) { this.closedAt = closedAt; }

    public OffsetDateTime getReopenedAt() { return reopenedAt; }
    public void setReopenedAt(OffsetDateTime reopenedAt) { this.reopenedAt = reopenedAt; }

    public Integer getReopenCount() { return reopenCount; }
    public void setReopenCount(Integer reopenCount) { this.reopenCount = reopenCount; }

    public String getReopenReason() { return reopenReason; }
    public void setReopenReason(String reopenReason) { this.reopenReason = reopenReason; }

    public String getResolutionNote() { return resolutionNote; }
    public void setResolutionNote(String resolutionNote) { this.resolutionNote = resolutionNote; }

    public Boolean getIsSlaBreached() { return isSlaBreached; }
    public void setIsSlaBreached(Boolean isSlaBreached) { this.isSlaBreached = isSlaBreached; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
