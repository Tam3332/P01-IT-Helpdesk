#!/bin/sh
# Tạo chứng chỉ TLS tự ký cho lab (KHÔNG commit, đã nằm trong .gitignore).
# Dùng:  sh scripts/gen-self-signed-cert.sh [IP_hoặc_tên_máy_lab]
# Ví dụ: sh scripts/gen-self-signed-cert.sh 192.168.1.50
set -eu
cd "$(dirname "$0")/.."
mkdir -p nginx/ssl
HOST="${1:-}"
SAN="DNS:localhost,IP:127.0.0.1"
if [ -n "$HOST" ]; then
  case "$HOST" in
    *[!0-9.]*) SAN="$SAN,DNS:$HOST" ;;
    *)         SAN="$SAN,IP:$HOST" ;;
  esac
fi
openssl req -x509 -nodes -newkey rsa:2048 -days 365 \
  -keyout nginx/ssl/server.key -out nginx/ssl/server.crt \
  -subj "/C=VN/O=P01 SecDesk Lab/CN=${HOST:-localhost}" \
  -addext "subjectAltName=$SAN"
chmod 600 nginx/ssl/server.key
echo "Đã tạo nginx/ssl/server.crt và server.key (SAN: $SAN)"
