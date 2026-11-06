#!/bin/sh
# Kiểm tra nhanh hệ thống P01 trên VM Ubuntu lab sau khi `docker compose up -d --build`.
# Dùng:  sh scripts/lab-check.sh [cổng_https]     (mặc định 443)
# Chụp màn hình kết quả làm bằng chứng (EV) cho T3-14. Script chỉ đọc, không sửa dữ liệu.
PORT="${1:-443}"
BASE="https://localhost:${PORT}"
PASS="${DEMO_PASSWORD:-Demo@12345}"
ok() { printf '  [ OK ] %s\n' "$1"; }
bad() { printf '  [LỖI] %s\n' "$1"; FAIL=1; }
FAIL=0

echo "1) Trạng thái container (cần 5 dòng: db, backend, frontend, gateway đều Up; db/backend/frontend healthy)"
docker compose ps

echo; echo "2) Cổng mở ra ngoài máy (chỉ nên có ${PORT}; không có 5432, 8080)"
if command -v ss >/dev/null 2>&1; then ss -tln | grep -E ":(${PORT}|5432|8080|80)\b" || true; fi
ss -tln 2>/dev/null | grep -qE ':(5432|8080)\b' && bad "5432/8080 đang mở ra máy chủ" || ok "5432 và 8080 không mở ra máy chủ"

echo; echo "3) HTTPS và header bảo mật"
curl -ksI "$BASE/" | grep -iE '^HTTP|strict-transport|x-content-type|x-frame|content-security' || bad "không gọi được $BASE"

echo; echo "4) Swagger bị chặn ở gateway (mong đợi 404)"
code=$(curl -ks -o /dev/null -w '%{http_code}' "$BASE/swagger-ui.html"); [ "$code" = "404" ] && ok "swagger-ui: $code" || bad "swagger-ui trả $code"

echo; echo "5) API cần đăng nhập (mong đợi 401 khi không có token)"
code=$(curl -ks -o /dev/null -w '%{http_code}' "$BASE/api/v1/tickets"); [ "$code" = "401" ] && ok "GET /tickets không token: $code" || bad "GET /tickets không token trả $code"

echo; echo "6) Đăng nhập 4 vai trò (mật khẩu demo)"
for u in employee staff manager admin; do
  body=$(curl -ks -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$u@helpdesk.local\",\"password\":\"$PASS\"}")
  tok=$(printf '%s' "$body" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
  if [ -n "$tok" ]; then
    me=$(curl -ks "$BASE/api/v1/auth/me" -H "Authorization: Bearer $tok" | sed -n 's/.*"role[^"]*":"\([A-Z_]*\)".*/\1/p' | head -1)
    ok "$u@helpdesk.local đăng nhập được (vai trò: ${me:-?}); token không in ra"
  else
    bad "$u@helpdesk.local không đăng nhập được: $(printf '%s' "$body" | cut -c1-160)"
  fi
done

echo; echo "7) Volume ghi được (audit log của backend)"
docker compose exec -T backend sh -c 'ls -la /app/logs /app/uploads' 2>&1 | head -12

echo; [ "$FAIL" = 0 ] && echo "KẾT QUẢ: đạt" || { echo "KẾT QUẢ: có mục lỗi, xem các dòng [LỖI] ở trên"; exit 1; }
