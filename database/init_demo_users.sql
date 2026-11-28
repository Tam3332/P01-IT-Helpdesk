-- CHỈ DÙNG CHO LAB / DEMO. Mật khẩu chung (bcrypt cost 12) lấy qua kênh nội bộ của nhóm, không ghi trong repo (BUG-HD-012). Không dùng ở môi trường thật.
-- Nếu Backend đã có DataInitializer tự tạo tài khoản thì bỏ file này khỏi docker-compose.
INSERT INTO users (username, email, password_hash, full_name, department, role_id) VALUES
('employee', 'employee@helpdesk.local', '$2b$12$SNRLQyCG8nfFjdw7fNOhlOGUP9tSK5AxRcjAWf2vMl45/sAmdCYh.', 'Nguyễn Văn An', 'Kế toán', 1),
('employee2', 'employee2@helpdesk.local', '$2b$12$1YF/VnLQPQq0cgQDy6MIwezLy3Ct.wJX4kuf39vQ5cbwpXChxAOsq', 'Lê Thị Cúc', 'Kế toán', 1),
('it.staff', 'staff@helpdesk.local', '$2b$12$QAIMiyIA8eOh/BuKNa9/du4z8PdoNe5izdhmOHhGHEJmdm2yc4Ck2', 'Trần Thị Bình', 'IT', 2),
('it.staff2', 'staff2@helpdesk.local', '$2b$12$B3c8.B.obv7WwAfhvcIp2OEzOjVutqi0Q44OJj4WohWfhFPQ5zj06', 'Hoàng Văn Nam', 'IT', 2),
('it.manager', 'manager@helpdesk.local', '$2b$12$O2E2O0Vg7f8Xu3iOVLvy0.hNQQJ6fDdn7BuKQlggNmayiA2ZH7gV.', 'Lê Minh Cường', 'IT', 3),
('sysadmin', 'admin@helpdesk.local', '$2b$12$ZwBXSdvJsY/3KnxBUObyl.FiU718WSSzsZxcUdourVlhZAXi81KHO', 'Phạm Quốc Dũng', 'IT', 4)
ON CONFLICT (email) DO NOTHING;
