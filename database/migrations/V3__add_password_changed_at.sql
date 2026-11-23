-- =============================================================================
-- Migration V3: Bổ sung cột password_changed_at vào bảng users
-- Áp dụng cho DB lab đã có dữ liệu. DB tạo mới thì chạy schema.sql là đủ.
-- Liên quan: BUG-HD-008 (Thu hồi JWT token phiên cũ sau khi đổi mật khẩu / đổi quyền)
-- =============================================================================

BEGIN;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_changed_at TIMESTAMPTZ NULL;

COMMENT ON COLUMN users.password_changed_at IS
    'Mốc thời gian đổi mật khẩu hoặc đổi vai trò gần nhất (UTC), dùng để thu hồi token cũ có iat < mốc này';

COMMIT;
