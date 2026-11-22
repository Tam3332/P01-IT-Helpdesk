#!/usr/bin/env bash
# SEC-TC-HD-07 (phần API) – SQLi qua tìm kiếm/lọc, IDOR qua search, leo quyền dọc – T5-02 (Nguyễn Tăng Minh Khánh)

# Payload vô hại: không DROP/UPDATE/DELETE. Ca đổi role dùng role "EMPLOYEE" (không đổi gì nếu lỡ lọt).
# Cần export trước: BASE, BUILD_TAG, WAF_MODE, PW_EMP1, PW_EMP2, PW_STAFF, PW_MGR, PW_ADMIN
set -u
: "${BASE:?export BASE=https://<ip-vm-lab>:8443}"
WAF_MODE=${WAF_MODE:-none}            # none | detection | on
OUT=evidence/T5-02; mkdir -p "$OUT"
LOG="$OUT/EV-HD-27-XSS_api_${WAF_MODE}_$(date +%Y%m%d_%H%M).txt"
API="$BASE/api/v1"

login() { sleep 7; curl -sk -X POST "$API/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r '.data.accessToken'; }
call()  { local t=$1 m=$2 p=$3 b=${4:-}; local a=(-sk -X "$m" "$API$p" -H "Authorization: Bearer $t"); [ -n "$b" ] && a+=(-H 'Content-Type: application/json' -d "$b"); curl "${a[@]}"; }
code()  { local t=$1 m=$2 p=$3 b=${4:-}; local a=(-sk -o /dev/null -w '%{http_code}' -X "$m" "$API$p" -H "Authorization: Bearer $t"); [ -n "$b" ] && a+=(-H 'Content-Type: application/json' -d "$b"); curl "${a[@]}"; }
enc()   { jq -rn --arg v "$1" '$v|@uri'; }
total() { call "$1" GET "/tickets?size=100&$2" | jq -r '.data.totalElements // "ERR"'; }
res()   { local r=FAIL; [ "$3" = "$4" ] && r=PASS; printf '%-7s %-58s expected=%-6s actual=%-6s %s\n' "$1" "$2" "$3" "$4" "$r" | tee -a "$LOG"; }
resn()  { local r=FAIL; [ "$4" != "$3" ] && [ "$4" != "000" ] && r=PASS; printf '%-7s %-58s expected=!%-5s actual=%-6s %s\n' "$1" "$2" "$3" "$4" "$r" | tee -a "$LOG"; }

T_EMP1=$(login employee@helpdesk.local "$PW_EMP1"); T_EMP2=$(login employee2@helpdesk.local "$PW_EMP2")
T_STAFF=$(login staff@helpdesk.local "$PW_STAFF");  T_MGR=$(login manager@helpdesk.local "$PW_MGR")
T_ADMIN=$(login admin@helpdesk.local "$PW_ADMIN")
for v in T_EMP1 T_EMP2 T_STAFF T_MGR T_ADMIN; do [ "${!v}" = "null" ] || [ -z "${!v}" ] && { echo "Đăng nhập lỗi: $v"; exit 1; }; done
EMP1_ID=$(call "$T_EMP1" GET /auth/me | jq -r '.data.id')
CAT=$(call "$T_EMP2" GET /categories | jq -r '.data[0].id')

# Ticket của Employee B có chuỗi đánh dấu duy nhất, chưa giao cho ai
MARK="SEC07-$(date +%s)"
BTK=$(call "$T_EMP2" POST /tickets "{\"title\":\"$MARK idor search\",\"description\":\"ticket test SEC-TC-HD-07\",\"categoryId\":$CAT,\"impactLevel\":\"LOW\"}")
BNUM=$(echo "$BTK" | jq -r '.data.ticketNumber')
# Dữ liệu nền để các ca so sánh phạm vi có ý nghĩa: A có 1 ticket riêng, ticket đó giao cho IT Staff.
# Tiêu đề KHÔNG chứa $MARK để không làm sai 07.4a.
STAFF_ID=$(call "$T_STAFF" GET /auth/me | jq -r '.data.id')
AID=$(call "$T_EMP1" POST /tickets "{\"title\":\"AOWN-$(date +%s) ticket rieng cua A\",\"description\":\"du lieu nen SEC-TC-HD-07\",\"categoryId\":$CAT,\"impactLevel\":\"LOW\"}" | jq -r '.data.id')
ASG=$(code "$T_MGR" PUT "/tickets/$AID/assign" "{\"assigneeId\":$STAFF_ID}")
A_TOTAL=$(total "$T_EMP1" ""); S_TOTAL=$(total "$T_STAFF" ""); G_TOTAL=$(total "$T_MGR" "")
echo "Build: ${BUILD_TAG:-chua ghi}  WAF=$WAF_MODE  BASE=$(echo "$BASE" | sed -E 's#//[0-9]+(\.[0-9]+){3}#//<IP-VM>#')  ngay=$(date '+%F %T')  ticketB=$BNUM  A_total=$A_TOTAL  Staff_total=$S_TOTAL  Global_total=$G_TOTAL  assign=$ASG" | tee -a "$LOG"
if [ "$A_TOTAL" -lt 1 ] || [ "$S_TOTAL" -lt 1 ] || [ "$G_TOTAL" -le "$A_TOTAL" ] || [ "$G_TOTAL" -le "$S_TOTAL" ]; then
  echo "CẢNH BÁO: dữ liệu nền chưa đủ (A, Staff phải >= 1 và nhỏ hơn Global) – kết quả 07.3b/07.5h/07.5i không có giá trị" | tee -a "$LOG"
fi

echo "== 07.4 IDOR qua search ==" | tee -a "$LOG"
res 07.4a "Employee A tìm chuỗi đánh dấu ticket của B"            0 "$(total "$T_EMP1"  "search=$MARK")"
res 07.4b "Employee A tìm theo mã ticket của B ($BNUM)"           0 "$(total "$T_EMP1"  "search=$BNUM")"
res 07.4c "IT Staff (không được giao) tìm ticket của B"           0 "$(total "$T_STAFF" "search=$MARK")"
res 07.4d "IT Manager tìm ticket của B (đối chứng)"               1 "$(total "$T_MGR"   "search=$MARK")"

echo "== 07.3 SQLi qua tham số tìm kiếm/lọc ==" | tee -a "$LOG"
for p in "' OR '1'='1" "1) OR (1=1" "' UNION SELECT NULL--" "\\' OR 1=1--"; do
  q="search=$(enc "$p")"
  resn 07.3a "search=$p → không lỗi 500"                     500 "$(code "$T_EMP1" GET "/tickets?$q")"
  res  07.3b "search=$p → không lộ ticket (0 kết quả)"         0 "$(total "$T_EMP1" "$q")"
done
t=$(curl -sk -o /dev/null -w '%{time_total}' "$API/tickets?search=$(enc "' OR pg_sleep(5)--")" -H "Authorization: Bearer $T_EMP1")
res  07.3c "pg_sleep(5) qua search → phản hồi < 3s (thực: ${t}s)"   yes "$(awk -v t="$t" 'BEGIN{print (t<3)?"yes":"no"}')"
res  07.3d "size=100000 → pageSize bị giới hạn 100"            100 "$(call "$T_EMP1" GET "/tickets?size=100000" | jq -r '.data.pageSize')"
resn 07.3e "page=-1 → không lỗi 500"                           500 "$(code "$T_EMP1" GET "/tickets?page=-1")"
resn 07.3f "sort=createdAt;SELECT 1 → không lỗi 500"           500 "$(code "$T_EMP1" GET "/tickets?sort=$(enc 'createdAt;SELECT 1')")"
resn 07.3g "status=NEW' OR '1'='1 → không lỗi 500"             500 "$(code "$T_EMP1" GET "/tickets?status=$(enc "NEW' OR '1'='1")")"
res  07.3h "createdFrom chứa SQLi → 400"                       400 "$(code "$T_EMP1" GET "/tickets?createdFrom=$(enc "2026-01-01' OR '1'='1")")"
resn 07.3i "categoryId=1 OR 1=1 → không lỗi 500"               500 "$(code "$T_EMP1" GET "/tickets?categoryId=$(enc '1 OR 1=1')")"

echo "== 07.5 Leo quyền dọc qua endpoint mới ==" | tee -a "$LOG"
res 07.5a "Employee GET /admin/users"                          403 "$(code "$T_EMP1"  GET /admin/users)"
res 07.5b "IT Staff GET /admin/users"                          403 "$(code "$T_STAFF" GET /admin/users)"
res 07.5c "IT Manager GET /admin/users"                        403 "$(code "$T_MGR"   GET /admin/users)"
res 07.5d "Admin GET /admin/users (đối chứng)"                 200 "$(code "$T_ADMIN" GET /admin/users)"
res 07.5e "Employee PATCH /admin/users/{id}/role"              403 "$(code "$T_EMP1"  PATCH "/admin/users/$EMP1_ID/role" '{"role":"EMPLOYEE"}')"
res 07.5f "IT Manager PATCH /admin/users/{id}/role"            403 "$(code "$T_MGR"   PATCH "/admin/users/$EMP1_ID/role" '{"role":"EMPLOYEE"}')"
res 07.5g "Employee GET /admin/audit-logs"                     403 "$(code "$T_EMP1"  GET /admin/audit-logs)"
# Dashboard: code hiện tại cho mọi role gọi nhưng lọc dữ liệu theo role; openapi ghi "dành riêng IT_MANAGER" → cần chốt với Trí
res 07.5h "Employee /dashboard/stats chỉ đếm ticket của mình"  "$A_TOTAL" "$(call "$T_EMP1"  GET /dashboard/stats | jq -r '.data.totalTickets')"
res 07.5i "IT Staff /dashboard/stats chỉ đếm vé được giao"     "$S_TOTAL" "$(call "$T_STAFF" GET /dashboard/stats | jq -r '.data.totalTickets')"
echo "Ghi chú 07.5h/i: dashboard trả 200 cho Employee/IT Staff (lọc theo role). Khác openapi (chỉ IT_MANAGER) → hỏi Trí/Khiêm chốt expected." | tee -a "$LOG"

echo "Tổng: PASS=$(grep -c ' PASS$' "$LOG") FAIL=$(grep -c ' FAIL$' "$LOG") -> $LOG" | tee -a "$LOG"
