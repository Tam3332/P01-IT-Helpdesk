package com.ojt.p01.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Một lần phân công người phụ trách (bảng ticket_assignments, SDD §3, BR-HD-05).
 * Mỗi ticket chỉ có 1 dòng is_current = true (partial unique index uq_ticket_active_assignment).
 */
@Entity
@Table(name = "ticket_assignments")
public class TicketAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id", nullable = false)
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by", nullable = false)
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt;

    @Column(name = "unassigned_at")
    private OffsetDateTime unassignedAt;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent;

    @Column(name = "assignment_note", length = 255)
    private String assignmentNote;

    protected TicketAssignment() {
        // JPA cần constructor rỗng
    }

    public TicketAssignment(Ticket ticket, User assignee, User assignedBy,
                            OffsetDateTime assignedAt, String assignmentNote) {
        this.ticket = ticket;
        this.assignee = assignee;
        this.assignedBy = assignedBy;
        this.assignedAt = assignedAt;
        this.assignmentNote = assignmentNote;
        this.isCurrent = true;
    }

    /** Kết thúc lần phân công này (khi điều phối lại cho người khác, SDD §3.4). */
    public void end(OffsetDateTime at) {
        this.isCurrent = false;
        this.unassignedAt = at;
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public User getAssignee() { return assignee; }
    public User getAssignedBy() { return assignedBy; }
    public OffsetDateTime getAssignedAt() { return assignedAt; }
    public OffsetDateTime getUnassignedAt() { return unassignedAt; }
    public Boolean getIsCurrent() { return isCurrent; }
    public String getAssignmentNote() { return assignmentNote; }
}
