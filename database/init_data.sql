-- =============================================================================
-- Dữ liệu Khởi tạo Mẫu (Seed Data) cho Hệ thống SecDesk (P01)
-- =============================================================================

-- 1. Seed Roles
INSERT INTO roles (id, role_name, description) VALUES
(1, 'EMPLOYEE', 'Người dùng nội bộ tạo phiếu yêu cầu hỗ trợ (Requester)'),
(2, 'IT_STAFF', 'Kỹ thuật viên IT tiếp nhận và xử lý sự cố (Assignee)'),
(3, 'IT_MANAGER', 'Trưởng nhóm IT điều phối, duyệt Reopen và giám sát SLA'),
(4, 'ADMIN', 'Quản trị viên hệ thống quản lý danh mục và cấu hình SLA')
ON CONFLICT (id) DO NOTHING;

-- 2. Seed Ticket Categories (UC-10)
INSERT INTO ticket_categories (category_code, category_name, description, default_priority, is_active) VALUES
('HARDWARE', 'Sự cố Phần cứng (PC/Laptop/Màn hình/Máy in)', 'Lỗi hỏng hóc thiết bị vật lý tại văn phòng', 'MEDIUM', TRUE),
('SOFTWARE', 'Sự cố Phần mềm & Ứng dụng', 'Lỗi cài đặt phần mềm chuyên môn, crash ứng dụng, bản quyền', 'MEDIUM', TRUE),
('NETWORK', 'Mạng nội bộ & VPN', 'Mất kết nối WiFi văn phòng, chậm mạng, không vào được VPN', 'HIGH', TRUE),
('ACCOUNT', 'Tài khoản & Phân quyền', 'Quên mật khẩu, khóa tài khoản AD, cấp quyền truy cập thư mục', 'HIGH', TRUE),
('OTHER', 'Yêu cầu CNTT khác', 'Các đề nghị tư vấn, mượn thiết bị hoặc hỗ trợ hội nghị', 'LOW', TRUE)
ON CONFLICT (category_code) DO NOTHING;

-- 3. Seed SLA Configs (UC-09, UC-10)
INSERT INTO sla_configs (priority_level, response_time_hours, resolution_time_hours, warning_threshold_pct, breach_threshold_pct) VALUES
('CRITICAL', 0.25, 4.00, 80, 100),   -- Phản hồi 15 phút, xử lý 4 giờ
('HIGH',     0.50, 8.00, 80, 100),   -- Phản hồi 30 phút, xử lý 8 giờ
('MEDIUM',   1.00, 24.00, 80, 100),  -- Phản hồi 1 giờ, xử lý 24 giờ (1 ngày)
('LOW',      2.00, 48.00, 80, 100)   -- Phản hồi 2 giờ, xử lý 48 giờ (2 ngày)
ON CONFLICT (priority_level) DO NOTHING;

-- 4. Đồng bộ sequence sau khi chèn id tường minh (tránh lỗi trùng khóa khi thêm role mới)
SELECT setval(pg_get_serial_sequence('roles', 'id'), (SELECT MAX(id) FROM roles));
