package com.ojt.p01.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Entity mapping bảng ticket_attachments.
 */
@Entity
@Table(name = "ticket_attachments")
public class TicketAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private TicketComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "storage_path", nullable = false, length = 500)
    private String storagePath;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "file_hash_sha256", nullable = false, length = 64)
    private String fileHashSha256;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }

    public TicketAttachment() {}

    public TicketAttachment(Ticket ticket, TicketComment comment, User uploader,
                            String fileName, String storagePath, Long fileSize,
                            String mimeType, String fileHashSha256) {
        this.ticket = ticket;
        this.comment = comment;
        this.uploader = uploader;
        this.fileName = fileName;
        this.storagePath = storagePath;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
        this.fileHashSha256 = fileHashSha256;
        this.createdAt = OffsetDateTime.now();
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public TicketComment getComment() { return comment; }
    public User getUploader() { return uploader; }
    public String getFileName() { return fileName; }
    public String getStoragePath() { return storagePath; }
    public Long getFileSize() { return fileSize; }
    public String getMimeType() { return mimeType; }
    public String getFileHashSha256() { return fileHashSha256; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
