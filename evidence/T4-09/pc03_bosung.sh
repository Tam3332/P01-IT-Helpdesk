#!/usr/bin/env bash
# PC-03 ca bổ sung (2) Admin tải tệp → 403, (4) Employee không phải chủ gửi feedback → 404 – T4-09
# CHẠY TRÊN: máy có Tailscale (VM của Khánh). ĐÍCH: chỉ lab của nhóm. Không in token.
# Cần export trước: BASE, PW_EMP1, PW_EMP2, PW_STAFF, PW_MGR, PW_ADMIN
set -u
: "${BASE:?export BASE=https://100.124.80.30:8443}"
OUT=evidence/T4-09; mkdir -p "$OUT"; LOG="$OUT/pc03_bosung_$(date +%Y%m%d_%H%M).txt"
API="$BASE/api/v1"
login() { sleep 7; curl -sk -X POST "$API/auth/login" -H 'Content-Type: application/json' -d "{\"email\":\"$1\",\"password\":\"$2\"}" | jq -r '.data.accessToken'; }
call()  { local t=$1 m=$2 p=$3 b=${4:-}; local a=(-sk -X "$m" "$API$p" -H "Authorization: Bearer $t"); [ -n "$b" ] && a+=(-H 'Content-Type: application/json' -d "$b"); curl "${a[@]}"; }
code()  { local t=$1 m=$2 p=$3 b=${4:-}; local a=(-sk -o /dev/null -w '%{http_code}' -X "$m" "$API$p" -H "Authorization: Bearer $t"); [ -n "$b" ] && a+=(-H 'Content-Type: application/json' -d "$b"); curl "${a[@]}"; }
res()   { local r=FAIL; [ "$3" = "$4" ] && r=PASS; printf '%-8s %-52s expected=%s actual=%s %s\n' "$1" "$2" "$3" "$4" "$r" | tee -a "$LOG"; }

T_EMP1=$(login employee@helpdesk.local "$PW_EMP1"); T_EMP2=$(login employee2@helpdesk.local "$PW_EMP2")
T_STAFF=$(login staff@helpdesk.local "$PW_STAFF");  T_MGR=$(login manager@helpdesk.local "$PW_MGR")
T_ADMIN=$(login admin@helpdesk.local "$PW_ADMIN")
STAFF_ID=$(call "$T_MGR" GET /users/it-staff | jq -r '.data[] | select(.email=="staff@helpdesk.local") | .id')
CAT=$(call "$T_EMP1" GET /categories | jq -r '.data[0].id')

# Ticket của Employee A, có 1 tệp đính kèm, đi hết tới CLOSED
TK=$(call "$T_EMP1" POST /tickets "{\"title\":\"PC-03 bo sung\",\"description\":\"ticket test PC-03\",\"categoryId\":$CAT,\"impactLevel\":\"LOW\"}" | jq -r '.data.id')
F=$(mktemp --suffix=.txt); echo "pc03" > "$F"
ATT=$(curl -sk -X POST "$API/tickets/$TK/attachments" -H "Authorization: Bearer $T_EMP1" -F "file=@$F;type=text/plain" | jq -r '.data.id'); rm -f "$F"
echo "Build: ${BUILD_TAG:-chua ghi}  BASE=$BASE  ticket=$TK attachment=$ATT" | tee -a "$LOG"

res PC03.2a "Employee chủ tải tệp của mình (đối chứng)"  200 "$(code "$T_EMP1"  GET "/tickets/$TK/attachments/$ATT")"
res PC03.2b "Admin tải tệp (Separation of Duties)"       403 "$(code "$T_ADMIN" GET "/tickets/$TK/attachments/$ATT")"
res PC03.2c "Admin xem danh sách tệp (metadata, Read-only)" 200 "$(code "$T_ADMIN" GET "/tickets/$TK/attachments")"

call "$T_MGR"   PUT   "/tickets/$TK/assign" "{\"assigneeId\":$STAFF_ID}" >/dev/null
call "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"IN_PROGRESS"}' >/dev/null
call "$T_STAFF" PATCH "/tickets/$TK/status" '{"status":"RESOLVED","resolutionNote":"Da kiem tra va xu ly xong su co."}' >/dev/null
call "$T_EMP1"  POST  "/tickets/$TK/close" >/dev/null
res PC03.0 "ticket đã CLOSED" CLOSED "$(call "$T_MGR" GET "/tickets/$TK" | jq -r '.data.status')"

FB='{"ratingStars":5,"feedbackComment":"pc03"}'
res PC03.4a "Employee B (không phải chủ) gửi feedback"   404 "$(code "$T_EMP2"  POST "/tickets/$TK/feedback" "$FB")"
res PC03.4b "Admin gửi feedback"                          403 "$(code "$T_ADMIN" POST "/tickets/$TK/feedback" "$FB")"
res PC03.4c "Employee A (chủ) gửi feedback (đối chứng)"   201 "$(code "$T_EMP1"  POST "/tickets/$TK/feedback" "$FB")"
res PC03.3a "IT Staff được giao xem feedback"             200 "$(code "$T_STAFF" GET  "/tickets/$TK/feedback")"
res PC03.3b "IT Manager xem feedback"                     200 "$(code "$T_MGR"   GET  "/tickets/$TK/feedback")"
res PC03.3c "Admin xem feedback (Read-only)"              200 "$(code "$T_ADMIN" GET  "/tickets/$TK/feedback")"
res PC03.3d "Employee B xem feedback ticket của A"        404 "$(code "$T_EMP2"  GET  "/tickets/$TK/feedback")"
echo "Tổng: PASS=$(grep -c ' PASS$' "$LOG") FAIL=$(grep -c ' FAIL$' "$LOG") -> $LOG" | tee -a "$LOG"
