package com.ojt.p01.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * Token đã bị thu hồi khi đăng xuất (bảng token_blacklist, ADR-02, SDD §6.1).
 * Chỉ lưu jti (mã riêng của token), không lưu token gốc (IA-HD07).
 */
@Entity
@Table(name = "token_blacklist")
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    // Lưu thẳng user_id, không cần nạp cả đối tượng User khi thu hồi token
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // Thời điểm token hết hạn: sau mốc này bản ghi có thể dọn đi (token đã tự hết hiệu lực)
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at", nullable = false)
    private OffsetDateTime revokedAt;

    protected TokenBlacklist() {
        // JPA cần constructor rỗng
    }

    public TokenBlacklist(String jti, Long userId, OffsetDateTime expiresAt) {
        this.jti = jti;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        // Cột revoked_at NOT NULL: gán giờ hiện tại vì Hibernate sẽ gửi null nếu để trống
        if (revokedAt == null) {
            revokedAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public String getJti() { return jti; }
    public Long getUserId() { return userId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
}
