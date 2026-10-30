-- CHỈ DÙNG CHO LAB / DEMO. Mật khẩu chung: Demo@12345 (bcrypt cost 12). Không dùng ở môi trường thật.
-- Nếu Backend đã có DataInitializer tự tạo tài khoản thì bỏ file này khỏi docker-compose.
INSERT INTO users (username, email, password_hash, full_name, department, role_id) VALUES
('employee', 'employee@helpdesk.local', '$2b$12$Ja4UEqrtSMTxXjq1ooddVeAivE6IzkQXmwBieQ4lLOCqNWUijx9ju', 'Nguyễn Văn An', 'Kế toán', 1),
('it.staff', 'staff@helpdesk.local', '$2b$12$k7dexMVoGouH/B/KPiD2DOTIeWsm3t3cn3RLKLG8l/Dmx95bLQcA.', 'Trần Thị Bình', 'IT', 2),
('it.manager', 'manager@helpdesk.local', '$2b$12$NYv5ZYlMpZUAZLGiRIx9huqRYtRmAvl6KN.DTSxZUd55sHVG.UzaS', 'Lê Minh Cường', 'IT', 3),
('sysadmin', 'admin@helpdesk.local', '$2b$12$.SXszGqgiobrtPjGW19u8u9kotkfJgfz2FOjxDsoQmjXo.blNscwy', 'Phạm Quốc Dũng', 'IT', 4)
ON CONFLICT (email) DO NOTHING;
