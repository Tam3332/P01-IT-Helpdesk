#!/bin/sh
# =============================================================================
# T6-06 · Đo thời gian phản hồi API trên VM lab với ~1.000 ticket (sheet ghi "NFR04 – API < 2s";
# trong SRS hiện tại yêu cầu hiệu năng là NFR-02 Performance).
# Đo: GET /tickets (phân trang, tìm kiếm, lọc), GET /tickets/{id}, dashboard, upload tệp 10 MB.
# Kết quả: thời gian trung bình, p95, lớn nhất; ca vượt 2 giây cần ghi BUG vào 04 §4.
#
# Chạy trên VM lab sau khi seed (seed_perf_tickets.sql):
#   DEMO_PASSWORD='<mật khẩu demo>' sh tests/perf/nfr_perf.sh https://localhost:8443/api/v1 30
# Tham số: [1] địa chỉ API (mặc định https://localhost:8443/api/v1), [2] số lần đo mỗi API (mặc định 30).
# Mật khẩu chỉ lấy từ biến môi trường, không ghi vào file kết quả. Token không in ra.
# Script giãn cách 0,1 giây giữa các request để không chạm giới hạn 20 request/giây của Nginx.
# =============================================================================
set -eu
BASE="${1:-https://localhost:8443/api/v1}"
N="${2:-30}"
LIMIT_MS=2000
: "${DEMO_PASSWORD:?Hãy đặt DEMO_PASSWORD (mật khẩu demo) trước khi chạy}"
STAMP=$(date +%Y%m%d_%H%M)
OUT="nfr_perf_${STAMP}.md"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

login() {
  curl -ks -X POST "$BASE/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$DEMO_PASSWORD\"}" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

TOK_MANAGER=$(login manager@helpdesk.local)
TOK_EMPLOYEE=$(login employee@helpdesk.local)
[ -n "$TOK_MANAGER" ] && [ -n "$TOK_EMPLOYEE" ] || { echo "Không đăng nhập được, kiểm tra DEMO_PASSWORD và địa chỉ API"; exit 1; }

TOTAL=$(curl -ks "$BASE/tickets?size=1" -H "Authorization: Bearer $TOK_MANAGER" | sed -n 's/.*"totalElements":\([0-9]*\).*/\1/p')
PERF_ID=$(curl -ks "$BASE/tickets?search=PERF-0500&size=1" -H "Authorization: Bearer $TOK_MANAGER" | sed -n 's/.*"content":\[{"id":\([0-9]*\).*/\1/p')
UP_ID=$(curl -ks "$BASE/tickets?search=PERF-0001&size=1" -H "Authorization: Bearer $TOK_EMPLOYEE" | sed -n 's/.*"content":\[{"id":\([0-9]*\).*/\1/p')
[ -n "$PERF_ID" ] && [ -n "$UP_ID" ] || { echo "Chưa có ticket PERF-xxxx, hãy chạy seed_perf_tickets.sql trước"; exit 1; }

# Đo 1 API: measure "<tên>" "<vai trò>" "<số lần>" <tham số curl...>
measure() {
  name="$1"; role="$2"; times="$3"; shift 3
  : > "$TMP/t"; codes=""
  i=0
  while [ "$i" -lt "$times" ]; do
    r=$(curl -ks -o /dev/null -w '%{http_code} %{time_total}' "$@")
    code=${r% *}; sec=${r#* }
    echo "$sec" | awk '{printf "%d\n", $1 * 1000}' >> "$TMP/t"
    case " $codes " in *" $code "*) ;; *) codes="$codes $code" ;; esac
    i=$((i + 1)); sleep 0.1
  done
  sort -n "$TMP/t" | awk -v name="$name" -v role="$role" -v codes="${codes# }" -v lim="$LIMIT_MS" '
    { v[NR] = $1; s += $1 }
    END {
      p = int(NR * 0.95 + 0.999); if (p < 1) p = 1
      ok = (v[p] < lim) ? "Đạt" : "KHÔNG ĐẠT (ghi BUG)"
      printf "| %s | %s | %d | %s | %d | %d | %d | %s |\n", name, role, NR, codes, s / NR, v[p], v[NR], ok
    }' >> "$OUT"
  echo "  đã đo: $name"
}

{
  echo "# Kết quả đo thời gian phản hồi API (T6-06)"
  echo
  echo "- Thời điểm: $(date '+%d/%m/%Y %H:%M') · Máy: $(hostname) · API: $BASE"
  echo "- Tổng số ticket trong hệ thống lúc đo: ${TOTAL:-?} (gồm ticket PERF-xxxx seed)"
  echo "- Mỗi API đo $N lần (upload 5 lần vì giới hạn 5 tệp/ticket); ngưỡng p95 < ${LIMIT_MS} ms"
  echo
  echo "| API | Vai trò | Số lần | HTTP | Trung bình (ms) | p95 (ms) | Lớn nhất (ms) | Kết luận |"
  echo "|---|---|---|---|---|---|---|---|"
} > "$OUT"

AM="Authorization: Bearer $TOK_MANAGER"
AE="Authorization: Bearer $TOK_EMPLOYEE"
measure "GET /tickets?page=0&size=20" "IT Manager" "$N" -H "$AM" "$BASE/tickets?page=0&size=20"
measure "GET /tickets?page=0&size=100" "IT Manager" "$N" -H "$AM" "$BASE/tickets?page=0&size=100"
measure "GET /tickets?page=40&size=20 (trang sâu)" "IT Manager" "$N" -H "$AM" "$BASE/tickets?page=40&size=20"
measure "GET /tickets?search=PERF-05" "IT Manager" "$N" -H "$AM" "$BASE/tickets?search=PERF-05&size=20"
measure "GET /tickets?status=IN_PROGRESS&priority=HIGH" "IT Manager" "$N" -H "$AM" "$BASE/tickets?status=IN_PROGRESS&priority=HIGH&size=20"
measure "GET /tickets?page=0&size=20" "Employee" "$N" -H "$AE" "$BASE/tickets?page=0&size=20"
measure "GET /tickets/{id}" "IT Manager" "$N" -H "$AM" "$BASE/tickets/$PERF_ID"
measure "GET /dashboard/stats" "IT Manager" "$N" -H "$AM" "$BASE/dashboard/stats"
measure "GET /dashboard/sla-monitor" "IT Manager" "$N" -H "$AM" "$BASE/dashboard/sla-monitor"

# Tệp 10 MB (đúng giới hạn SRS HD04): chữ ký PNG + dữ liệu ngẫu nhiên, nhỏ hơn 10 MB một chút.
# Bỏ header "Expect: 100-continue" để curl không chờ thêm 1 giây trước khi gửi thân request (sai số đo).
F="$TMP/perf_10mb.png"
printf '\211PNG\r\n\032\n' > "$F"
head -c $((10 * 1024 * 1024 - 4096)) /dev/urandom >> "$F"
measure "POST /tickets/{id}/attachments (10 MB)" "Employee" 5 -H "$AE" -H "Expect:" -F "file=@$F;type=image/png" "$BASE/tickets/$UP_ID/attachments"

echo
echo "Đã ghi kết quả: $OUT"
cat "$OUT"
