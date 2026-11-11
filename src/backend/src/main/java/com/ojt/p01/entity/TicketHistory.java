package com.ojt.p01.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Nhật ký xử lý ticket, chỉ thêm mới, không sửa/xóa (bảng ticket_history).
 * Không có setter: tạo một lần qua constructor rồi lưu.
 */
@Entity
@Table(name = "ticket_history")
public class TicketHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false, updatable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id", nullable = false, updatable = false)
    private User actor;

 /** CREATE, ASSIGN, REASSIGN, STATUS_CHANGE... (theo comment trong schema.sql) */
    @Column(name = "action", nullable = false, length = 50, updatable = false)
    private String action;

    @Column(name = "field_changed", length = 50, updatable = false)
    private String fieldChanged;

    @Column(name = "old_value", columnDefinition = "TEXT", updatable = false)
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT", updatable = false)
    private String newValue;

    @Column(name = "reason_or_note", columnDefinition = "TEXT", updatable = false)
    private String reasonOrNote;

    @Column(name = "ip_address", nullable = false, length = 45, updatable = false)
    private String ipAddress;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private OffsetDateTime timestamp;

    protected TicketHistory() {
        // JPA cần constructor rỗng
    }

    public TicketHistory(Ticket ticket, User actor, String action, String fieldChanged,
                         String oldValue, String newValue, String reasonOrNote,
                         String ipAddress, OffsetDateTime timestamp) {
        this.ticket = ticket;
        this.actor = actor;
        this.action = action;
        this.fieldChanged = fieldChanged;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reasonOrNote = reasonOrNote;
        this.ipAddress = ipAddress;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public User getActor() { return actor; }
    public String getAction() { return action; }
    public String getFieldChanged() { return fieldChanged; }
    public String getOldValue() { return oldValue; }
    public String getNewValue() { return newValue; }
    public String getReasonOrNote() { return reasonOrNote; }
    public String getIpAddress() { return ipAddress; }
    public OffsetDateTime getTimestamp() { return timestamp; }
}
