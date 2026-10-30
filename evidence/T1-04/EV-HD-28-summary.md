# EV-HD-28 — IA-HD07 Security Hardening Evidence

## 1. Environment
- Platform: Ubuntu VirtualBox VM
- Container platform: Docker
- Web server: Nginx
- Security requirement: IA-HD07

## 2. Security Validation

| Test Case | Validation | Result |
|---|---|---|
| SEC-TC-HD-08.1 | HTTPS and TLS configuration | Partially Verified |
| SEC-TC-HD-08.2 | HTTP Security Headers | Partially Verified |
| SEC-TC-HD-08.3 | Configuration secrets review | Finding - Remediation Required |
| SEC-TC-HD-08.4 | Container hardening and network ports | Partially Verified |
| SEC-TC-HD-08.5 | Nginx log inspection | Partially Verified |

## 3. Evidence Files
- ia-hd07-hardening-retest.txt
- ia-hd07-secrets-logs-ports-review.txt
- ia-hd07-network-review.txt
- ia-hd07-docker-network.txt

## 4. Findings
- Fixed password and JWT secret fallback values in Compose.
- UFW inactive.
- SSH listens on all IPv4/IPv6 interfaces.
- Dedicated Docker bridge network created but not yet used.
- Application/backend security validation pending.

## 5. Conclusion
IA-HD07 is partially verified in the laboratory.
Full acceptance requires remediation and integrated application testing.
