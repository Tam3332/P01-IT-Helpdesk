#!/usr/bin/env bash
# SEC-TC-HD-06 (workflow bypass + mass assignment) – P01 IT Helpdesk – T4-02
# CHẠY TRÊN: máy Kali tester. ĐÍCH: chỉ VM Ubuntu lab của nhóm (build tag từ main).
# Mật khẩu KHÔNG ghi trong file. Export trước khi chạy:
#   export BASE=https://<IP_VM_LAB>
#   export PW_EMP1=... PW_EMP2=... PW_STAFF=... PW_STAFF2=... PW_MGR=... PW_ADMIN=...
# Expected lấy theo code nhánh tri/be commit d45753b (TicketStatusTransitionValidator, TicketService, ReopenService).
set -u
: "${BASE:?export BASE=https://<IP_VM_LAB>}"
OUT=evidence/T4-02; mkdir -p "$OUT"
LOG="$OUT/result_$(date +%Y%m%d_%H%M).txt"
API="$BASE/api/v1"
NOTE_OK="Da thay cap mang va kiem tra ket noi on dinh."      # >= 20 ký tự
REASON_OK="Su co van con, may van mat ket noi sau 10 phut."  # >= 20 ký tự

login() {  # $1=email $2=password -> accessToken. Nghỉ 7s vì Nginx giới hạn login 10 request/phút
  sleep 7
  curl -sk -X POST "$API/auth/login" -H 'Content-Type: application/json' \
    -d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r '.data.accessToken'
}
# call <token> <method> <path> [json] -> in body (dùng cho bước chuẩn bị)
call() {
  local tok=$1 m=$2 p=$3 body=${4:-}
  local args=(-sk -X "$m" "$API$p" -H "Authorization: Bearer $tok")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  curl "${args[@]}"
}
# check <mã ca> <expected> <token> <method> <path> [json]
check() {
  local id=$1 exp=$2 tok=$3 m=$4 p=$5 body=${6:-}
  local args=(-sk -o /dev/null -w '%{http_code}' -X "$m" "$API$p" -H "Authorization: Bearer $tok")
  [ -n "$body" ] && args+=(-H 'Content-Type: application/json' -d "$body")
  local got; got=$(curl "${args[@]}")
  local r=FAIL; [ "$got" = "$exp" ] && r=PASS
  printf '%-8s %-5s %-40s expected=%s actual=%s %s\n' "$id" "$m" "$p" "$exp" "$got" "$r" | tee -a "$LOG"
}
# same <mã ca> <mô tả> <giá trị mong đợi> <giá trị thực tế>
same() {
  local r=FAIL; [ "$3" = "$4" ] && r=PASS
  printf '%-8s %-46s expected=%s actual=%s %s\n' "$1" "$2" "$3" "$4" "$r" | tee -a "$LOG"
}
status_of() { call "$T_MGR" GET "/tickets/$1" | jq -r '.data.status'; }

T_EMP1=$(login employee@helpdesk.local  "$PW_EMP1")
T_EMP2=$(login employee2@helpdesk.local "$PW_EMP2")
T_STAFF=$(login staff@helpdesk.local    "$PW_STAFF")
T_STAFF2=$(login staff2@helpdesk.local  "$PW_STAFF2")
T_MGR=$(login manager@helpdesk.local    "$PW_MGR")
T_ADMIN=$(login admin@helpdesk.local    "$PW_ADMIN")
for v in T_EMP1 T_EMP2 T_STAFF T_STAFF2 T_MGR T_ADMIN; do
  { [ -z "${!v}" ] || [ "${!v}" = null ]; } && { echo "Login lỗi: $v"; exit 1; }
done

ME_EMP1=$(call "$T_EMP1" GET /auth/me | jq -r '.data.id')
ME_MGR=$(call "$T_MGR" GET /auth/me | jq -r '.data.id')
STAFF_LIST=$(call "$T_MGR" GET /users/it-staff)
ID_STAFF=$(echo "$STAFF_LIST"  | jq -r '.data[] | select(.email=="staff@helpdesk.local")  | .id')
ID_STAFF2=$(echo "$STAFF_LIST" | jq -r '.data[] | select(.email=="staff2@helpdesk.local") | .id')
CAT=$(call "$T_EMP1" GET /categories | jq -r '.data[0].id')
NEW_BODY="{\"title\":\"SEC-TC-HD-06 target\",\"description\":\"ticket test workflow\",\"categoryId\":$CAT,\"impactLevel\":\"LOW\"}"
TK=$(call "$T_EMP1" POST /tickets "$NEW_BODY" | jq -r '.data.id')
PRIO0=$(call "$T_MGR" GET "/tickets/$TK" | jq -r '.data.priority')
{ echo "Build: ${BUILD_TAG:-chua ghi}  BASE=$BASE"; echo "Ticket của Employee A: id=$TK priority=$PRIO0 staff=$ID_STAFF staff2=$ID_STAFF2"; } | tee -a "$LOG"

echo "--- (1) Nhảy trạng thái bỏ bước -> 422 ---" | tee -a "$LOG"
check 06.1.1 422 "$T_MGR" PATCH "/tickets/$TK/status" "{\"status\":\"RESOLVED\",\"resolutionNote\":\"$NOTE_OK\"}"   # NEW -> RESOLVED
check 06.1.2 422 "$T_MGR" PATCH "/tickets/$TK/status" '{"status":"CLOSED"}'                                        # NEW -> CLOSED
check 06.1.3 422 "$T_MGR" PATCH "/tickets/$TK/status" '{"status":"IN_PROGRESS"}'                                   # NEW -> IN_PROGRESS
check 06.1.4 422 "$T_EMP1" POST "/tickets/$TK/close"                                                               # đóng khi chưa RESOLVED
same  06.1.5 "trạng thái vẫn NEW sau 4 ca trên" NEW "$(status_of "$TK")"

echo "--- (5) Phân công trái quyền / sai người (BR-HD-05) ---" | tee -a "$LOG"
check 06.5.1 403 "$T_EMP1"  PUT "/tickets/$TK/assign" "{\"assigneeId\":$ID_STAFF}"
check 06.5.2 403 "$T_STAFF" PUT "/tickets/$TK/assign" "{\"assigneeId\":$ID_STAFF}"     # Staff tự phân công
check 06.5.3 422 "$T_MGR"   PUT "/tickets/$TK/assign" "{\"assigneeId\":$ME_MGR}"       # Manager tự gán mình
check 06.5.4 422 "$T_MGR"   PUT "/tickets/$TK/assign" "{\"assigneeId\":$ME_EMP1}"      # gán cho Employee
check 06.5.5 200 "$T_MGR"   PUT "/tickets/$TK/assign" "{\"assigneeId\":$ID_STAFF2}"    # chuẩn bị: giao staff2
check 06.5.6 200 "$T_MGR"   PUT "/tickets/$TK/assign" "{\"assigneeId\":$ID_STAFF}"     # điều phối lại sang staff
check 06.5.7 404 "$T_STAFF2" GET "/tickets/$TK"                                        # người cũ mất quyền -> chỉ còn 1 người phụ trách
same  06.5.8 "assigneeId hiện tại = staff" "$ID_STAFF" "$(call "$T_MGR" GET "/tickets/$TK" | jq -r '.data.assigneeId')"

echo "--- (7) IT Staff đổi trạng thái ticket không được giao -> 404 ---" | tee -a "$LOG"
check 06.7.1 404 "$T_STAFF2" PATCH "/tickets/$TK/status" '{"status":"IN_PROGRESS"}'
check 06.7.2 403 "$T_EMP1"   PATCH "/tickets/$TK/status" '{"status":"IN_PROGRESS"}'    # chủ ticket cũng không được đổi

echo "--- (1) tiếp + (2) BR-HD-01 ---" | tee -a "$LOG"
check 06.1.6 422 "$T_STAFF" PATCH "/tickets/$TK/status" "{\"status\":\"RESOLVED\",\"resolutionNote\":\"$NOTE_OK\"}" # ASSIGNED -> RESOLVED
check 06.1.7 200 "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"IN_PROGRESS"}'                                 # chuẩn bị
check 06.1.8 422 "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"CLOSED"}'                                      # IN_PROGRESS -> CLOSED
check 06.1.9 422 "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"KHONG_TON_TAI"}'
check 06.2.1 400 "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"RESOLVED"}'                                    # không có giải pháp
check 06.2.2 400 "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"RESOLVED","resolutionNote":"da xu ly"}'        # < 20 ký tự
check 06.2.3 400 "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"RESOLVED","resolutionNote":"                         "}'  # toàn khoảng trắng
same  06.2.4 "trạng thái vẫn IN_PROGRESS" IN_PROGRESS "$(status_of "$TK")"

echo "--- (6) Mass assignment: trường thừa phải bị bỏ qua ---" | tee -a "$LOG"
check 06.6.1 200 "$T_STAFF" PATCH "/tickets/$TK/status" \
  "{\"status\":\"RESOLVED\",\"resolutionNote\":\"$NOTE_OK\",\"assigneeId\":$ID_STAFF2,\"requesterId\":$ME_MGR,\"priority\":\"CRITICAL\",\"isSlaBreached\":false,\"resolutionDeadline\":\"2099-01-01T00:00:00Z\"}"
D=$(call "$T_MGR" GET "/tickets/$TK")
same 06.6.2 "priority không đổi"    "$PRIO0"    "$(echo "$D" | jq -r '.data.priority')"
same 06.6.3 "assigneeId không đổi"  "$ID_STAFF" "$(echo "$D" | jq -r '.data.assigneeId')"
same 06.6.4 "requesterId không đổi" "$ME_EMP1"  "$(echo "$D" | jq -r '.data.requesterId')"
M=$(call "$T_EMP1" POST /tickets \
  "{\"title\":\"SEC-TC-HD-06 mass\",\"description\":\"mass assignment\",\"categoryId\":$CAT,\"impactLevel\":\"LOW\",\"status\":\"CLOSED\",\"priority\":\"CRITICAL\",\"assigneeId\":$ID_STAFF,\"requesterId\":$ME_MGR,\"role\":\"ADMIN\",\"resolutionDeadline\":\"2099-01-01T00:00:00Z\"}")
TM=$(echo "$M" | jq -r '.data.id'); DM=$(call "$T_MGR" GET "/tickets/$TM")
same 06.6.5 "ticket mới: status = NEW"           NEW        "$(echo "$DM" | jq -r '.data.status')"
same 06.6.6 "ticket mới: priority như ticket thường" "$PRIO0" "$(echo "$DM" | jq -r '.data.priority')"
same 06.6.7 "ticket mới: chưa có assignee"       null       "$(echo "$DM" | jq -r '.data.assigneeId')"
same 06.6.8 "ticket mới: requester = Employee A" "$ME_EMP1" "$(echo "$DM" | jq -r '.data.requesterId')"
C=$(call "$T_EMP1" POST "/tickets/$TK/comments" "{\"commentText\":\"mass assignment\",\"isInternal\":true,\"authorId\":$ME_MGR}")
same 06.6.9 "Employee gửi isInternal=true bị ép false" false "$(echo "$C" | jq -r '.data.isInternal')"

echo "--- (3) Đóng / mở lại trái BR-HD-02 (ticket đang RESOLVED) ---" | tee -a "$LOG"
check 06.3.1 403 "$T_STAFF" POST "/tickets/$TK/close"
check 06.3.2 403 "$T_ADMIN" POST "/tickets/$TK/close"
check 06.3.3 404 "$T_EMP2"  POST "/tickets/$TK/close"                                          # Employee không sở hữu
check 06.3.4 403 "$T_STAFF" POST "/tickets/$TK/reopen" "{\"reopenReason\":\"$REASON_OK\"}"
check 06.3.5 403 "$T_MGR"   POST "/tickets/$TK/reopen" "{\"reopenReason\":\"$REASON_OK\"}"
check 06.3.6 404 "$T_EMP2"  POST "/tickets/$TK/reopen" "{\"reopenReason\":\"$REASON_OK\"}"     # Employee không sở hữu
check 06.3.7 400 "$T_EMP1"  POST "/tickets/$TK/reopen" '{"reopenReason":"chua duoc"}'          # lý do < 20 ký tự
same  06.3.8 "trạng thái vẫn RESOLVED" RESOLVED "$(status_of "$TK")"

echo "--- (8) Reopen: Employee tự duyệt / sai role ---" | tee -a "$LOG"
check 06.8.1 201 "$T_EMP1" POST "/tickets/$TK/reopen" "{\"reopenReason\":\"$REASON_OK\"}"      # chuẩn bị: RESOLVED -> REOPEN_PENDING
RID=$(call "$T_MGR" GET "/reopen-requests?status=PENDING" | jq -r ".data[] | select(.ticketId==$TK) | .id")
echo "reopen-request id=$RID" | tee -a "$LOG"
check 06.8.2 403 "$T_EMP1"  GET  "/reopen-requests"
check 06.8.3 403 "$T_EMP1"  POST "/reopen-requests/$RID/approve" '{}'                          # Employee tự duyệt
check 06.8.4 403 "$T_EMP1"  POST "/reopen-requests/$RID/reject" '{"reviewNote":"tu choi yeu cau nay"}'
check 06.8.5 403 "$T_STAFF" POST "/reopen-requests/$RID/approve" '{}'
check 06.8.6 403 "$T_ADMIN" POST "/reopen-requests/$RID/approve" '{}'
check 06.8.7 422 "$T_EMP1"  POST "/tickets/$TK/reopen" "{\"reopenReason\":\"$REASON_OK\"}"     # gửi lần 2 khi đang chờ duyệt
check 06.8.8 422 "$T_MGR"   POST "/tickets/$TK/close"                                          # đóng khi REOPEN_PENDING
check 06.8.9 400 "$T_MGR"   POST "/reopen-requests/$RID/reject" '{}'                           # từ chối không có reviewNote
same  06.8.10 "trạng thái vẫn REOPEN_PENDING" REOPEN_PENDING "$(status_of "$TK")"
check 06.8.11 200 "$T_MGR"  POST "/reopen-requests/$RID/reject" '{"reviewNote":"Su co da duoc xu ly, vui long tao ticket moi."}'  # về RESOLVED

echo "--- (4) Thao tác trên ticket CLOSED (BR-HD-04) -> 422 ---" | tee -a "$LOG"
check 06.4.1 200 "$T_EMP1"  POST  "/tickets/$TK/close"                                         # chuẩn bị: chủ ticket đóng
check 06.4.2 422 "$T_EMP1"  POST  "/tickets/$TK/comments" '{"commentText":"binh luan sau khi dong"}'
check 06.4.3 422 "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"IN_PROGRESS"}'
check 06.4.4 422 "$T_MGR"   PATCH "/tickets/$TK/status" "{\"status\":\"RESOLVED\",\"resolutionNote\":\"$NOTE_OK\"}"
check 06.4.5 422 "$T_MGR"   PUT   "/tickets/$TK/assign" "{\"assigneeId\":$ID_STAFF2}"
check 06.4.6 422 "$T_EMP1"  POST  "/tickets/$TK/reopen" "{\"reopenReason\":\"$REASON_OK\"}"
check 06.4.7 422 "$T_EMP1"  POST  "/tickets/$TK/close"
F=$(mktemp --suffix=.txt); echo "sec-tc-hd-06" > "$F"
got=$(curl -sk -o /dev/null -w '%{http_code}' -X POST "$API/tickets/$TK/attachments" -H "Authorization: Bearer $T_EMP1" -F "file=@$F;type=text/plain"); rm -f "$F"
same  06.4.8 "upload vào ticket CLOSED (HTTP)" 422 "$got"
same  06.4.9 "trạng thái vẫn CLOSED" CLOSED "$(status_of "$TK")"

P=$(grep -c ' PASS$' "$LOG"); Fl=$(grep -c ' FAIL$' "$LOG")
echo "Tổng: PASS=$P FAIL=$Fl  -> $LOG (không chứa token)" | tee -a "$LOG"
