-- =============================================================================
-- Hệ thống SecDesk - Quản lý Yêu cầu Dịch vụ CNTT & Giám sát An toàn (P01)
-- Cơ sở dữ liệu: PostgreSQL 16 | Chuẩn hóa 3NF | Ràng buộc An toàn Thông tin
-- Tác giả: Nhóm 2 - FPT University OJT Fall 2026
-- =============================================================================


-- 1. BẢNG VAI TRÒ (ROLES)
CREATE TABLE IF NOT EXISTS roles (
    id SERIAL PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL UNIQUE, -- EMPLOYEE, IT_STAFF, IT_MANAGER, ADMIN
    description VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. BẢNG NGƯỜI DÙNG (USERS) - Ràng buộc IA-HD05 (Khóa 15m sau 5 lần sai)
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL, -- BCrypt Hash cost 12
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    department VARCHAR(100),
    role_id INT NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    failed_login_attempts INT NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role_id ON users(role_id);

-- 3. BẢNG DANH MỤC SỰ CỐ / YÊU CẦU (TICKET_CATEGORIES) - UC-10
CREATE TABLE IF NOT EXISTS ticket_categories (
    id SERIAL PRIMARY KEY,
    category_code VARCHAR(50) NOT NULL UNIQUE, -- HARDWARE, SOFTWARE, NETWORK, ACCOUNT, OTHER
    category_name VARCHAR(100) NOT NULL,
    description TEXT,
    default_priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' 
        CHECK (default_priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. BẢNG CẤU HÌNH SLA (SLA_CONFIGS) - UC-09, UC-10
CREATE TABLE IF NOT EXISTS sla_configs (
    id SERIAL PRIMARY KEY,
    priority_level VARCHAR(20) NOT NULL UNIQUE 
        CHECK (priority_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    response_time_hours NUMERIC(5,2) NOT NULL CHECK (response_time_hours > 0),
    resolution_time_hours NUMERIC(5,2) NOT NULL CHECK (resolution_time_hours > 0),
    warning_threshold_pct INT NOT NULL DEFAULT 80 CHECK (warning_threshold_pct > 0 AND warning_threshold_pct <= 100),
    breach_threshold_pct INT NOT NULL DEFAULT 100 CHECK (breach_threshold_pct >= warning_threshold_pct),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. BẢNG PHIẾU YÊU CẦU (TICKETS) - Trung tâm nghiệp vụ
CREATE TABLE IF NOT EXISTS tickets (
    id BIGSERIAL PRIMARY KEY,
    ticket_number VARCHAR(30) NOT NULL UNIQUE, -- HD-YYYY-XXXX (ví dụ HD-2026-0001)
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id INT NOT NULL REFERENCES ticket_categories(id) ON DELETE RESTRICT,
    impact_level VARCHAR(20) NOT NULL CHECK (impact_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(30) NOT NULL DEFAULT 'NEW' 
        CHECK (status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED')),
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    current_assignee_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL,
    response_deadline TIMESTAMPTZ NOT NULL,
    resolution_deadline TIMESTAMPTZ NOT NULL,
    first_responded_at TIMESTAMPTZ NULL,
    resolved_at TIMESTAMPTZ NULL,
    closed_at TIMESTAMPTZ NULL,
    reopened_at TIMESTAMPTZ NULL,
    reopen_count INT NOT NULL DEFAULT 0,
    reopen_reason TEXT NULL,
    resolution_note TEXT NULL,
    is_sla_breached BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Chỉ mục tối ưu hiệu năng (NFR-01, NFR-02)
CREATE INDEX idx_tickets_requester_status ON tickets(requester_id, status);
CREATE INDEX idx_tickets_assignee_status ON tickets(current_assignee_id, status);
CREATE INDEX idx_tickets_filter ON tickets(status, priority, created_at DESC);
CREATE INDEX idx_tickets_sla_worker ON tickets(status, resolution_deadline) 
    WHERE status IN ('NEW', 'ASSIGNED', 'IN_PROGRESS', 'REOPENED');

-- 6. BẢNG LỊCH SỬ PHÂN CÔNG (TICKET_ASSIGNMENTS) - BR-HD-05
CREATE TABLE IF NOT EXISTS ticket_assignments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    assignee_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assigned_by BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unassigned_at TIMESTAMPTZ NULL,
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_note VARCHAR(255) NULL
);

-- Ràng buộc Partial Unique: Mỗi ticket chỉ có DUY NHẤT 1 người xử lý chính active tại 1 thời điểm
CREATE UNIQUE INDEX uq_ticket_active_assignment 
    ON ticket_assignments (ticket_id) 
    WHERE is_current = TRUE;

CREATE INDEX idx_ticket_assignments_ticket_id ON ticket_assignments(ticket_id);
CREATE INDEX idx_ticket_assignments_assignee_id ON ticket_assignments(assignee_id);

-- 7. BẢNG BÌNH LUẬN & TRAO ĐỔI (TICKET_COMMENTS) - BR-HD-06, IA-HD06
CREATE TABLE IF NOT EXISTS ticket_comments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    comment_text TEXT NOT NULL,
    is_internal BOOLEAN NOT NULL DEFAULT FALSE, -- Internal note: chỉ IT Staff/Manager thấy
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_comments_ticket_id ON ticket_comments(ticket_id, created_at ASC);

-- 8. BẢNG TỆP TIN ĐÍNH KÈM (TICKET_ATTACHMENTS) - IA-HD03
CREATE TABLE IF NOT EXISTS ticket_attachments (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    comment_id BIGINT NULL REFERENCES ticket_comments(id) ON DELETE SET NULL,
    uploader_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL, -- Đường dẫn lưu trữ ngoài webroot
    file_size BIGINT NOT NULL CHECK (file_size > 0 AND file_size <= 10485760), -- Tối đa 10 MB
    mime_type VARCHAR(100) NOT NULL, -- Whitelist: image/png, image/jpeg, application/pdf, docx
    file_hash_sha256 VARCHAR(64) NOT NULL, -- Checksum SHA-256 xác thực toàn vẹn
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_attachments_ticket_id ON ticket_attachments(ticket_id);

-- 9. BẢNG NHẬT KÝ KIỂM TOÁN BẤT BIẾN (TICKET_HISTORY) - BR-HD-07, NFR-07
CREATE TABLE IF NOT EXISTS ticket_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE RESTRICT, -- Ngăn cascade delete mất log
    actor_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action VARCHAR(50) NOT NULL, -- CREATE, ASSIGN, REASSIGN, STATUS_CHANGE, COMMENT, SLA_WARNING, SLA_BREACH, REOPEN, CLOSE
    field_changed VARCHAR(50) NULL,
    old_value TEXT NULL,
    new_value TEXT NULL,
    reason_or_note TEXT NULL,
    ip_address VARCHAR(45) NOT NULL, -- IPv4 / IPv6
    timestamp TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_history_ticket_id ON ticket_history(ticket_id, timestamp DESC);
CREATE INDEX idx_ticket_history_actor_id ON ticket_history(actor_id);

-- 10. BẢNG ĐÁNH GIÁ HÀI LÒNG KHÁCH HÀNG (TICKET_FEEDBACKS) - HD15 CSAT
CREATE TABLE IF NOT EXISTS ticket_feedbacks (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL UNIQUE REFERENCES tickets(id) ON DELETE CASCADE, -- 1 ticket có tối đa 1 feedback
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    rating_stars INT NOT NULL CHECK (rating_stars >= 1 AND rating_stars <= 5),
    feedback_comment VARCHAR(500) NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ticket_feedbacks_ticket_id ON ticket_feedbacks(ticket_id);


-- =============================================================================
-- BỔ SUNG YÊU CẦU AN NINH & NGHIỆP VỤ (27/10 - E1 & E2)
-- =============================================================================

-- 11. BẢNG YÊU CẦU MỞ LẠI TICKET (REOPEN_REQUESTS) - E1 (UC-08, BR-HD-04)
CREATE TABLE IF NOT EXISTS reopen_requests (
    id BIGSERIAL PRIMARY KEY,
    ticket_id BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    reopen_reason TEXT NOT NULL, -- Lý do mở lại bắt buộc >= 20 ký tự (BR-HD-04)
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' 
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    reviewed_by BIGINT NULL REFERENCES users(id) ON DELETE RESTRICT, -- IT Manager phê duyệt
    review_note TEXT NULL, -- Ghi chú duyệt hoặc lý do từ chối
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMPTZ NULL
);

-- Ràng buộc Partial Unique: Mỗi ticket chỉ có tối đa 1 yêu cầu PENDING chờ duyệt
CREATE UNIQUE INDEX uq_ticket_pending_reopen 
    ON reopen_requests (ticket_id) 
    WHERE status = 'PENDING';

CREATE INDEX idx_reopen_requests_ticket_id ON reopen_requests(ticket_id);
CREATE INDEX idx_reopen_requests_status ON reopen_requests(status);
CREATE INDEX idx_reopen_requests_requester_id ON reopen_requests(requester_id);

-- 12. BẢNG DANH SÁCH ĐEN TOKEN THU HỒI (TOKEN_BLACKLIST) - E2 (ADR-02, SDD §6.1, IA-HD01)
CREATE TABLE IF NOT EXISTS token_blacklist (
    id BIGSERIAL PRIMARY KEY,
    jti VARCHAR(36) NOT NULL UNIQUE, -- JWT ID duy nhất từ token payload
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NULL, -- Chuỗi token hoặc hash để đối soát
    expires_at TIMESTAMPTZ NOT NULL, -- Thời điểm token hết hạn để TTL worker dọn dẹp
    revoked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_token_blacklist_jti ON token_blacklist(jti);
CREATE INDEX idx_token_blacklist_expires_at ON token_blacklist(expires_at);
CREATE INDEX idx_token_blacklist_user_id ON token_blacklist(user_id);
