# EV-HD-22 — Security authentication testing (live HTTP/JWT + offline replay real audit)

**Tester:** Huỳnh Minh Hòa
**Date:** 2026-11-14
**Environment:** P01 Ubuntu VM lab via Tailscale, HTTPS port 8443
**Task:** T4-10 — SEC-TC-HD-02
**Overall status:** PARTIALLY VERIFIED — live HTTP/JWT PASS; R1 replay PASS; remaining checks below.

## 1. Live HTTP/JWT test results

| Test | Expected | Actual | Result |
|---|---:|---:|---|
| Request without JWT | 401 | 401 | PASS |
| Invalid login, attempt 1 | 401 | 401 | PASS |
| Invalid login, attempt 2 | 401 | 401 | PASS |
| Invalid login, attempt 3 | 401 | 401 | PASS |
| Invalid login, attempt 4 | 401 | 401 | PASS |
| Invalid login, attempt 5 | 401 | 401 | PASS |
| Valid login, successful retest | 200 + JWT | 200 + JWT | PASS |
| GET /auth/me with valid JWT | 200 | 200 | PASS |
| POST /auth/logout | 200 | 200 | PASS |
| GET /auth/me with revoked JWT | 401 | 401 | PASS |

**Total:** 10 confirmed PASS assertions across two CSV evidence files. The initial `LOGIN_SUCCESS = NOT_CONFIRMED [CHECK]` remains unchanged in the first CSV, documenting the initial attempt; the valid-login retest and JWT/logout checks were confirmed separately in `EV-HD-22_JWT_20261114_225013.csv`. Do not count that initial CHECK as PASS.

## 2. R1 Monitoring — offline replay of real Backend audit data

- Input: `audit_login_failed_r1.jsonl` supplied as a copy of audit log from the VM, SHA-256 `1762f7fd39d80893dce434f8704cfe8d2c9ffa7ac70d94c6610a3ba060a8318a`.
- Monitoring v1 offline replay: **8 events processed, 0 skipped, 1 alert**.
- Rule triggered: **R1_BRUTE_FORCE_LOGIN**, severity HIGH, 5 failed logins in a 300-second window.
- Detection timestamp: `2026-11-14T15:32:15Z` (22:32:15 Vietnam time).
- Evidence: `EV-HD-22_R1_live_replay_check.md` and `EV-HD-22_R1_live_replay_redacted.csv`.
- **Result:** R1 detection PASS for offline replay using actual audit events. **This is not evidence that the Monitoring sidecar is running continuously on the VM.**

## 3. Earlier local precheck (historical snapshot)

- `offline_precheck.txt`: 18/18 monitoring unit tests passed; 12 synthetic events yielded 2 alerts.
- The precheck file records that VM connectivity was unavailable *at the time of the precheck*. The later HTTP/JWT live test establishes that VM connectivity was subsequently restored.
- `offline_alerts_redacted.csv` is synthetic-sample evidence, not the Nov 14 live-audit replay result.

## 4. Pending checks / known limitations

- **Monitoring sidecar on VM:** deployment, continuous reading of `audit_logs` volume, and writing to `monitoring_alerts` volume are awaiting confirmation from Nhi/Chính.
- **Nginx HTTP 429:** not live-tested by this tester; check available T4-11 evidence separately. IP rate limiting is not equivalent to account lockout.
- **Backend account lockout:** not demonstrated; the v0.4-week4 source did not implement five-failure account lockout. Confirm the deployed backend version before final classification.
- **JWT time-based expiration (15 minutes):** not live-tested in this run; only logout/revocation was validated.
- **Traceability:** confirmation of current deployed commit/tag should be attached when available.

## 5. Evidence inventory

- `EV-HD-22_lab_http_20261114_223144.csv` — initial live no-token and 5 invalid-login checks; contains one retained CHECK.
- `EV-HD-22_JWT_20261114_225013.csv` — successful login retest, valid JWT, logout, revoked JWT.
- `EV-HD-22_R1_live_replay_check.md` — real-audit replay method/results and source fingerprint.
- `EV-HD-22_R1_live_replay_redacted.csv` — redacted R1 alert details.
- `offline_precheck.txt` — earlier unit tests and synthetic replay.
- `offline_alerts_redacted.csv` — synthetic replay alerts.

## 6. Security and conclusion

The evidence in this folder contains no plain-text password, JWT, secret, full authentication request body, email address, or actual source IP. The original unredacted audit log is not included in this folder and should not be committed without approval and appropriate sanitization.

**Conclusion:** Live HTTP/JWT flow PASS (10 assertions). R1 detection on a real VM audit extract PASS (offline replay). **EV-HD-22 is not yet fully PASS** because live sidecar, 429, account lockout, and token expiration checks are unresolved or outside this test session. Proposed task status: `Review` with explicit pending notes.
