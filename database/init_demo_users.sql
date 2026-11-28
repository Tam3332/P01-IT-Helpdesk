-- CHỈ DÙNG CHO LAB / DEMO. Mật khẩu chung (bcrypt cost 12) lấy qua kênh nội bộ của nhóm, không ghi trong repo (BUG-HD-012). Không dùng ở môi trường thật.
-- Nếu Backend đã có DataInitializer tự tạo tài khoản thì bỏ file này khỏi docker-compose.
INSERT INTO users (username, email, password_hash, full_name, department, role_id) VALUES
('employee', 'employee@helpdesk.local', '$2b$12$4VvQJhYHLn0GWgIBsYL3i.4JMQifUtwlt6qTq9/fEgT.wRevWEuo2', 'Nguyễn Văn An', 'Kế toán', 1),
('employee2', 'employee2@helpdesk.local', '$2b$12$Glqo.z4z9ZEhNNOcHVw2CuFBzP5hsas9YChq9aod6gHR6N4xb/lPW', 'Lê Thị Cúc', 'Kế toán', 1),
('it.staff', 'staff@helpdesk.local', '$2b$12$04o3XUbAnllEXB7ZI0CLju.orpomahtC8VvKqrbwGHFzk/XJV4Daa', 'Trần Thị Bình', 'IT', 2),
('it.staff2', 'staff2@helpdesk.local', '$2b$12$pDg.ss.VSzg0fXRGqw0o/O999Ht/tl2/RoV0KHLfRFiuQsy3L8f9i', 'Hoàng Văn Nam', 'IT', 2),
('it.manager', 'manager@helpdesk.local', '$2b$12$qb/TVaSMHlSMyhYiX6cHSO63bH/cH4tbQ.gQIwEJ4DKPrekL5X.v6', 'Lê Minh Cường', 'IT', 3),
('sysadmin', 'admin@helpdesk.local', '$2b$12$iwoHg9ZqGVvKYpPe7476gebt90KidLDciF.zL/0cf3/hRp2DIQXda', 'Phạm Quốc Dũng', 'IT', 4)
ON CONFLICT (email) DO NOTHING;
