-- =============================================================================
-- Migration V3: Bổ sung cột credentials_updated_at vào bảng users
-- Áp dụng cho DB lab đã có dữ liệu. DB tạo mới thì chạy schema.sql là đủ.
-- Liên quan: BUG-HD-008, IA-HD01, SEC-TC-HD-05, ADR-02 v2
-- (Thu hồi JWT token phiên cũ sau khi đổi mật khẩu, đổi vai trò hoặc khóa tài khoản)
-- =============================================================================

BEGIN;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS credentials_updated_at TIMESTAMPTZ NULL;

COMMENT ON COLUMN users.credentials_updated_at IS
    'Mốc thời gian đổi mật khẩu, đổi vai trò hoặc khóa tài khoản gần nhất (UTC), dùng để thu hồi token cũ có iat < mốc này';

COMMIT;
