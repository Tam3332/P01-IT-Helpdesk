#!/usr/bin/env bash
# SEC-TC-HD-01 (IDOR) + SEC-TC-HD-05 (leo thang quyền dọc) – P01 IT Helpdesk
# CHẠY TRÊN: máy Kali tester. ĐÍCH: chỉ VM Ubuntu lab của nhóm.
# Mật khẩu KHÔNG ghi trong file: export trước khi chạy, ví dụ
#   export BASE=https://<IP_VM_LAB>
#   export PW_EMP1=... PW_EMP2=... PW_STAFF=... PW_MGR=... PW_ADMIN=...
set -u
: "${BASE:?export BASE=https://<IP_VM_LAB>}"
OUT=evidence/T3-02; mkdir -p "$OUT"
LOG="$OUT/result_$(date +%Y%m%d_%H%M).txt"

login() {  # $1=email $2=password -> in ra accessToken
  curl -sk -X POST "$BASE/api/v1/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r '.data.accessToken'
}

# check <mã ca> <expected code> <token> <method> <path> [json body]
check() {
  local id=$1 exp=$2 tok=$3 m=$4 p=$5 body=${6:-}
  local args=(-sk -o /dev/null -w '%{http_code}' -X "$m" "$BASE$p" -H "Authorization: Bearer $tok")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  local got; got=$(curl "${args[@]}")
  local r=FAIL; [ "$got" = "$exp" ] && r=PASS
  printf '%-10s %-6s %-48s expected=%s actual=%s %s\n' "$id" "$m" "$p" "$exp" "$got" "$r" | tee -a "$LOG"
}

T_EMP1=$(login employee@helpdesk.local  "$PW_EMP1")
T_EMP2=$(login employee2@helpdesk.local "$PW_EMP2")
T_STAFF=$(login staff@helpdesk.local    "$PW_STAFF")
T_MGR=$(login manager@helpdesk.local    "$PW_MGR")
T_ADMIN=$(login admin@helpdesk.local    "$PW_ADMIN")
for v in T_EMP1 T_EMP2 T_STAFF T_MGR T_ADMIN; do
  [ -z "${!v}" ] || [ "${!v}" = null ] && { echo "Login lỗi: $v"; exit 1; }
done

# Employee B (employee2) tạo 1 ticket làm mục tiêu
CAT=$(curl -sk "$BASE/api/v1/categories" -H "Authorization: Bearer $T_EMP2" | jq -r '.data[0].id')
TB=$(curl -sk -X POST "$BASE/api/v1/tickets" -H "Authorization: Bearer $T_EMP2" \
  -H 'Content-Type: application/json' \
  -d "{\"title\":\"SEC-TC-HD-01 target\",\"description\":\"ticket cua B\",\"categoryId\":$CAT,\"impactLevel\":\"LOW\"}" \
  | jq -r '.data.id')
echo "Ticket của B: id=$TB" | tee -a "$LOG"

echo "--- SEC-TC-HD-01: IDOR (object-level -> 404 + AUTHZ_DENIED) ---" | tee -a "$LOG"
check 01.1 200 "$T_EMP2" GET  "/api/v1/tickets/$TB"                 # đối chứng: chủ ticket xem được
check 01.2 404 "$T_EMP1" GET  "/api/v1/tickets/$TB"
check 01.3 404 "$T_EMP1" GET  "/api/v1/tickets/$TB/history"
check 01.4 404 "$T_EMP1" GET  "/api/v1/tickets/$TB/comments"
check 01.5 404 "$T_EMP1" POST "/api/v1/tickets/$TB/comments" '{"commentText":"idor","isInternal":false}'
check 01.6 404 "$T_EMP1" GET  "/api/v1/tickets/$TB/attachments"
check 01.7 404 "$T_EMP1" POST "/api/v1/tickets/$TB/close"
check 01.8 404 "$T_STAFF" GET "/api/v1/tickets/$TB"                 # Staff chưa được giao
check 01.9 404 "$T_EMP1" GET  "/api/v1/tickets/999999"              # không tồn tại: phải giống 01.2

echo "--- SEC-TC-HD-05: leo thang quyền dọc (role-level -> 403) ---" | tee -a "$LOG"
check 05.1 403 "$T_EMP1"  PUT   "/api/v1/tickets/$TB/assign" '{"assigneeId":1}'
check 05.2 403 "$T_STAFF" PUT   "/api/v1/tickets/$TB/assign" '{"assigneeId":1}'
check 05.3 403 "$T_EMP1"  GET   "/api/v1/users/it-staff"
check 05.4 403 "$T_EMP1"  GET   "/api/v1/admin/sla-configs"
check 05.5 403 "$T_MGR"   PUT   "/api/v1/admin/sla-configs/HIGH" '{"responseTimeHours":1,"resolutionTimeHours":2,"warningThresholdPct":80,"breachThresholdPct":100}'
check 05.6 403 "$T_STAFF" GET   "/api/v1/admin/categories"
check 05.7 403 "$T_MGR"   POST  "/api/v1/admin/categories" '{"categoryCode":"X1","categoryName":"x","description":"x","defaultPriority":"LOW"}'
check 05.8 403 "$T_EMP2"  PATCH "/api/v1/tickets/$TB/status" '{"status":"RESOLVED","resolutionNote":"x"}'
check 05.9 401 ""         GET   "/api/v1/tickets"                   # không token

echo "Kết quả: $LOG  (không chứa token)"
