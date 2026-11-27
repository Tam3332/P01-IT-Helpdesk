-- =============================================================================
-- T6-06 · Seed 1.000 ticket để đo hiệu năng API (sheet ghi NFR04; SRS hiện tại: NFR-02 Performance)
-- CHỈ chạy trên môi trường test/lab, KHÔNG chạy trên dữ liệu thật. Xóa bằng cleanup_perf_tickets.sql sau khi đo.
-- Ticket seed có mã PERF-0001…PERF-1000 nên không ảnh hưởng cách đánh số HD-YYYY-XXXX của hệ thống.
-- Chạy:  docker compose exec -T postgres psql -U p01_admin -d p01_db < tests/perf/seed_perf_tickets.sql
-- =============================================================================
BEGIN;

INSERT INTO tickets (ticket_number, title, description, category_id, impact_level, priority, status,
                     requester_id, current_assignee_id, response_deadline, resolution_deadline,
                     first_responded_at, resolved_at, closed_at, resolution_note, created_at, updated_at)
SELECT
    'PERF-' || lpad(n::text, 4, '0'),
    '[PERF] Sự cố mẫu số ' || n || ' – ' || (ARRAY['Máy in','Mạng','VPN','Email','Phần mềm kế toán'])[1 + n % 5],
    'Ticket sinh tự động để đo hiệu năng danh sách, tìm kiếm và dashboard (T6-06).',
    (SELECT id FROM ticket_categories WHERE is_active ORDER BY id LIMIT 1 OFFSET (n % 3)),
    p.lvl, p.lvl,
    s.st,
    -- 2/3 của employee, 1/3 của employee2 để thử phạm vi theo người tạo
    (SELECT id FROM users WHERE email = CASE WHEN n % 3 = 0 THEN 'employee2@helpdesk.local' ELSE 'employee@helpdesk.local' END),
    CASE WHEN s.st = 'NEW' THEN NULL ELSE (SELECT id FROM users WHERE email = 'staff@helpdesk.local') END,
    -- Hạn đặt xa để SLA Worker không gắn cờ vi phạm hàng loạt trong lúc đo
    now() + interval '30 days', now() + interval '60 days',
    CASE WHEN s.st IN ('IN_PROGRESS','RESOLVED','CLOSED') THEN c.t + interval '1 hour' END,
    CASE WHEN s.st IN ('RESOLVED','CLOSED') THEN c.t + interval '5 hours' END,
    CASE WHEN s.st = 'CLOSED' THEN c.t + interval '1 day' END,
    CASE WHEN s.st IN ('RESOLVED','CLOSED') THEN 'Đã xử lý xong trong đợt đo hiệu năng (dữ liệu mẫu).' END,
    c.t, c.t
FROM generate_series(1, 1000) AS n
CROSS JOIN LATERAL (SELECT (ARRAY['LOW','MEDIUM','HIGH','CRITICAL'])[1 + n % 4] AS lvl) p
CROSS JOIN LATERAL (SELECT (ARRAY['NEW','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'])[1 + n % 5] AS st) s
CROSS JOIN LATERAL (SELECT now() - (n % 30) * interval '1 day' - (n % 24) * interval '1 hour' AS t) c
ON CONFLICT (ticket_number) DO NOTHING;

COMMIT;

SELECT count(*) AS perf_tickets FROM tickets WHERE ticket_number LIKE 'PERF-%';
