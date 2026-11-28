-- CHỈ DÙNG CHO LAB / DEMO. Mật khẩu chung (bcrypt cost 12) lấy qua kênh nội bộ của nhóm, không ghi trong repo (BUG-HD-012). Không dùng ở môi trường thật.
-- Nếu Backend đã có DataInitializer tự tạo tài khoản thì bỏ file này khỏi docker-compose.
INSERT INTO users (username, email, password_hash, full_name, department, role_id) VALUES
('employee', 'employee@helpdesk.local', '$2b$12$22MlQ/2kbwI9IrHWlcZQzu.7ZC5jFEYmPHALvzzLAyhqZtgMXufF6', 'Nguyễn Văn An', 'Kế toán', 1),
('employee2', 'employee2@helpdesk.local', '$2b$12$/zr52a27qFmENqNvJ71nX.11mra3jySB29V8zq3HPvU1T0SFPIC/6', 'Lê Thị Cúc', 'Kế toán', 1),
('it.staff', 'staff@helpdesk.local', '$2b$12$wIAuNYhKDRcLVdLMduDV8uH.q4IAD3rTw21Os6Z6vIH8e0utjBPEO', 'Trần Thị Bình', 'IT', 2),
('it.staff2', 'staff2@helpdesk.local', '$2b$12$boA90zH1mnGU5i7Us7Umv.iHHjbpoSkgzr9A1f6H.7ztL6p2SoC9C', 'Hoàng Văn Nam', 'IT', 2),
('it.manager', 'manager@helpdesk.local', '$2b$12$e3JA/6LB54w4RDz9GBsKnOcZnSWWF8/tZb4dZblqVbFxW.0DhmJw6', 'Lê Minh Cường', 'IT', 3),
('sysadmin', 'admin@helpdesk.local', '$2b$12$zEQT1CNh/Dc.QL4dzL54oOBs30rInMDrnPu3dyZ7wVrw2kchlBiB6', 'Phạm Quốc Dũng', 'IT', 4)
ON CONFLICT (email) DO NOTHING;
