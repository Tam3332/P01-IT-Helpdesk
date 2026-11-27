-- =============================================================================
-- T6-06 · Xóa dữ liệu đo hiệu năng (ticket PERF-xxxx) sau khi đo xong.
-- ticket_history dùng ON DELETE RESTRICT nên xóa lịch sử của ticket PERF trước (nếu có, vd do SLA Worker ghi).
-- Bình luận, tệp đính kèm, phân công, đánh giá, yêu cầu mở lại tự xóa theo ON DELETE CASCADE.
-- Tệp vật lý đã tải lên trong lúc đo vẫn nằm trong volume uploads_data: xem tests/README.md mục "Dọn dẹp".
-- Chạy:  docker compose exec -T postgres psql -U p01_admin -d p01_db < tests/perf/cleanup_perf_tickets.sql
-- =============================================================================
BEGIN;
DELETE FROM ticket_history WHERE ticket_id IN (SELECT id FROM tickets WHERE ticket_number LIKE 'PERF-%');
DELETE FROM tickets WHERE ticket_number LIKE 'PERF-%';
COMMIT;
SELECT count(*) AS perf_tickets_con_lai FROM tickets WHERE ticket_number LIKE 'PERF-%';
