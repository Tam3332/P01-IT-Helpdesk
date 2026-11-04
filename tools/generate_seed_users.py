#!/usr/bin/env python3
"""
generate_seed_users.py — Sinh file SQL seed users với BCrypt hash từ biến môi trường.

Cách dùng:
    # Đặt mật khẩu qua env (KHÔNG commit vào repo):
    $env:SEED_PASSWORD = "Demo@12345"
    python tools/generate_seed_users.py > database/init_demo_users.sql

Yêu cầu: bcrypt (pip install bcrypt)
"""

import os
import sys

try:
    import bcrypt
except ImportError:
    print("Lỗi: Cần cài bcrypt. Chạy: pip install bcrypt", file=sys.stderr)
    sys.exit(1)

SEED_PASSWORD = os.environ.get("SEED_PASSWORD")
if not SEED_PASSWORD:
    print("Lỗi: Biến môi trường SEED_PASSWORD chưa được đặt.", file=sys.stderr)
    print("Ví dụ: $env:SEED_PASSWORD = 'Demo@12345'", file=sys.stderr)
    sys.exit(1)

# Sinh BCrypt hash (cost 12)
def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt(rounds=12)).decode("utf-8")

print(f"-- Tạo lúc: {__import__('datetime').datetime.now().isoformat()}")
print("-- CẢNH BÁO: File này được sinh tự động. KHÔNG commit lên repo.")
print("-- Mật khẩu được đọc từ biến môi trường SEED_PASSWORD.")
print("-- Dùng cho môi trường dev/demo, KHÔNG dùng trên production.")
print()

users = [
    ("employee",  "employee@helpdesk.local",  "Nguyễn Văn An",     "Kế toán", 1),
    ("employee2", "employee2@helpdesk.local", "Trần Thị Mai",       "Kế toán", 1),  # Dùng để test IDOR
    ("it.staff",  "staff@helpdesk.local",     "Trần Thị Bình",      "IT",      2),
    ("it.manager","manager@helpdesk.local",   "Lê Minh Cường",      "IT",      3),
    ("sysadmin",  "admin@helpdesk.local",     "Phạm Quốc Dũng",     "IT",      4),
]

print("INSERT INTO users (username, email, password_hash, full_name, department, role_id) VALUES")
rows = []
for i, (username, email, full_name, dept, role_id) in enumerate(users):
    pw_hash = hash_password(SEED_PASSWORD)
    # Escape dấu nháy đơn
    full_name_escaped = full_name.replace("'", "''")
    row = f"('{username}', '{email}', '{pw_hash}', '{full_name_escaped}', '{dept}', {role_id})"
    rows.append(row)
    print(f"-- Đang hash user {i+1}/{len(users)}: {username} ...", file=sys.stderr)

print(",\n".join(rows))
print("ON CONFLICT (email) DO NOTHING;")
print()
print("-- Đồng bộ sequence sau khi insert")
print("SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT MAX(id) FROM users));")
