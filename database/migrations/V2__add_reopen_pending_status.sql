-- =============================================================================
-- Migration V2: Bổ sung trạng thái REOPEN_PENDING vào bảng tickets
-- Áp dụng cho DB lab đã có dữ liệu. DB tạo mới thì chạy schema.sql là đủ.
-- Liên quan: HD10, UC-08 (luồng mở lại có Manager duyệt)
-- =============================================================================

BEGIN;

-- 1. Xóa ràng buộc CHECK cũ trên cột status
ALTER TABLE tickets
    DROP CONSTRAINT IF EXISTS tickets_status_check;

-- 2. Thêm lại ràng buộc CHECK mới chứa đầy đủ 7 trạng thái vòng đời ticket
ALTER TABLE tickets
    ADD CONSTRAINT tickets_status_check
    CHECK (status IN (
        'NEW',
        'ASSIGNED',
        'IN_PROGRESS',
        'RESOLVED',
        'CLOSED',
        'REOPENED',
        'REOPEN_PENDING'
    ));

-- 3. Ghi mô tả cho ràng buộc (chỉ là metadata, không kiểm tra dữ liệu)
COMMENT ON CONSTRAINT tickets_status_check ON tickets IS
    'Ràng buộc trạng thái ticket: NEW, ASSIGNED, IN_PROGRESS, RESOLVED, CLOSED, REOPENED, REOPEN_PENDING';

COMMIT;

-- 4. Kiểm tra sau khi chạy: kết quả phải có REOPEN_PENDING
-- SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'tickets_status_check';
