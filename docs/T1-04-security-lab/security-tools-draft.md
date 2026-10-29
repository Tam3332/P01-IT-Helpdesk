# Helpdesk Security Tools — Draft

## 1. Purpose
Document security tools used in the Ubuntu Docker
laboratory for IA-HD03 and IA-HD07 validation.

## 2. Tools Used

| Tool | Purpose | Related SR | Validation | Evidence |
|---|---|---|---|---|
| Docker CLI | Inspect container configuration and ports | IA-HD07 | docker ps, docker inspect | EV-HD-28 |
| Nginx | Configure and verify HTTPS and headers | IA-HD07 | nginx -t | EV-HD-28 |
| curl | Test HTTPS responses and security headers | IA-HD07 | curl -k -I | EV-HD-28 |
| OpenSSL | Inspect TLS configuration | IA-HD07 | openssl s_client | EV-HD-28 |
| grep | Review configuration for embedded secrets | IA-HD07 | grep -nEi | EV-HD-28 |
| ss | Inspect host listening ports | IA-HD07 | ss -lntup | EV-HD-28 |
| UFW | Inspect firewall status | IA-HD07 | ufw status verbose | EV-HD-28 |
| Docker Logs | Review Nginx container logs | IA-HD07 | docker logs | EV-HD-28 |

## 3. IA-HD03 — Planned Validation Tools

| Tool | Purpose | Status |
|---|---|---|
| curl | Test file upload and download endpoints | Pending backend |
| Postman | Test upload validation and authorization | Planned |
| File signature inspection | Compare file content and declared type | Planned |
| Docker Logs | Review application upload/download logs | Pending backend |

## 4. Security Findings

- Fixed credential fallback values found in Compose.
- UFW is inactive.
- SSH listens on all IPv4/IPv6 interfaces.
- Docker network helpdesk-internal has no attached containers.
- Full backend and database validation remains pending.

## 5. Current Status

IA-HD07: Partially Verified.
IA-HD03: Pending backend integration.

This document is a draft for team review.
