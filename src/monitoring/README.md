# P01 — Security Monitoring v1 (T3-03 / T3-10)

**Status:** Python detector and sample-based Backend contract tests are complete. This has **not** been tested with real audit events or on the team's VM lab. `SEC-TC-HD-02` and `EV-HD-22` remain pending actual lab execution.

## Exact source contract (GitHub `cuong/be`, also `tri/be`, checked 05/11/2026)

The Spring Boot class `com.ojt.p01.audit.AuditLogger` writes **JSON Lines** to `audit.log-file`, default `logs/audit_events.jsonl`. The container runs in `/app` and already mounts `audit_logs:/app/logs` in `docker-compose.yml`. Its output fields are `timestamp` (UTC), `event_type`, `actor_id` (JSON integer or null), `role`, `target_type`, `target_id`, `action`, `outcome`, `src_ip`, `correlation_id`, `http_status`. The Backend does **not** currently emit `login_ref`.

Event sources:

| Event | Where emitted | Backend fields relevant for detection |
| --- | --- | --- |
| `LOGIN_FAILED` | `AuthService` | `actor_id` integer for existing account, null if email unknown; `target_type=USER_EMAIL`, `target_id=normalized email`, `src_ip`; 401 for wrong credentials; also 403 for an Admin-disabled account |
| `LOGIN_SUCCESS` | `AuthService` | same numeric `actor_id`, `target_type=USER`, `target_id=userId`, 200 |
| `AUTHZ_DENIED` | `TicketService` | `target_type=TICKET`, `actor_id` numeric, `http_status=404` when a real ticket is inaccessible |
| `AUTHZ_DENIED` | `SecurityConfig` | `target_type=ENDPOINT`, `target_id="METHOD /api/v1/..."`, 403 for role denial |
| `TICKET_CREATED` | `TicketService` | audit event after transaction commits; not a v1 alert trigger |

### Rules implemented

**R1 — Brute-force (SRS HD01, IA-HD05):** Count 5 consecutive invalid-credential `LOGIN_FAILED` events within 5 minutes, grouped by account identity and `src_ip`. For existing accounts, the stable identity is `actor_id`; for non-existing email addresses it is a SHA-256 digest of `USER_EMAIL.target_id` normalized with casefold/trim. When `LOGIN_SUCCESS` arrives from an existing account, its matching `actor_id` + IP clears the sequence. `LOGIN_FAILED/403` for a disabled account **does not** count as a failed password. Output field `login_ref` is `actor:<id>` or `email_sha256:<digest>` — **the original email is not written to alert output**. Existing v0 example events with pseudonymous `login_ref` remain supported.

**R2 — Unauthorized ticket access (IA-HD05 Proposed):** Count 3 explicit `AUTHZ_DENIED` events for the same actor within 5 minutes. Accept `target_type=TICKET`, or `target_type=ENDPOINT` **only** if `target_id` refers to `/api/v1/tickets...` (the access denied handler logs these). Exclude Admin endpoints and ordinary 404 responses which do not carry an `AUTHZ_DENIED` audit event. Numeric `actor_id` is normalized into a string in alert output.

`BRUTE_FORCE_DETECTED` will not trigger a duplicate alert when Backend implements account lockout. **Monitoring does not lock accounts or issue HTTP 429** (backend scheduled for week 5). The 3/5min R2 threshold remains Proposed until the IA lead approves it; the source-IP semantics also depend on the trusted reverse proxy.

## Windows PowerShell: test on local repo

From `E:\Projects\P01-IT-Helpdesk`:

```powershell
py -m unittest discover -s src/monitoring/tests -v
py src/monitoring/monitor.py --input src/monitoring/examples/audit.backend.sample.jsonl --output src/monitoring/examples/alerts.backend.actual.jsonl
Get-Content src/monitoring/examples/alerts.backend.actual.jsonl
```

Expected: **18 tests PASS** and **`offline: 12 audit events, 0 skipped, 2 alerts`** (R1 and R2). These events are synthetic but have the same JSON field names and types as `cuong/be`.

The earlier synthetic v0 fixture is also still supported:

```powershell
py src/monitoring/monitor.py --input src/monitoring/examples/audit.sample.jsonl --output src/monitoring/examples/alerts.actual.jsonl
```

### Live file follow (only when Backend is running in an approved lab)

```powershell
py src/monitoring/monitor.py --input "<actual-backend-audit-path>" --output "<writable-alert-output-path>" --follow
```

Use different paths for input and output, never copy real passwords, JWTs, bearer headers or user data into evidence. `--follow` skips existing lines by default. `--from-start` will replay existing logs; only use in an isolated run with a fresh output file. Monitoring currently does **not** store a durable checkpoint and may miss/duplicate alerts across restarts or log rotation. It is a lab prototype, not a production SIEM.

## Optional Docker Compose integration (requires Nhi/Chính review)

The package root includes **`docker-compose.monitoring.yml`**, which *adds* a Monitoring sidecar. It shares the already-declared `audit_logs` volume **read-only** with the Backend, puts generated alerts in an independent `monitoring_alerts` volume, and forces `AUDIT_LOG_FILE=/app/logs/audit_events.jsonl`. The sidecar has no network and drops capabilities; it still uses the image's default container user for lab convenience, so Nhi should review/strengthen permissions before release.

When approved on the VM, compose config can be checked without starting containers:

```powershell
docker compose -f docker-compose.yml -f docker-compose.monitoring.yml --profile monitoring config
```

Do not claim that the lab pipeline is running just because this configuration file parses. After the lab becomes available, Nhi/Chính must configure it on the same Compose project and verify `audit_events.jsonl` actually exists, can be read, and results in `monitoring_alerts:/alerts/alerts.jsonl`.

## Client IP / trusted proxy — still requires a Backend + gateway change

`AuditLogger` on `cuong/be` currently uses `request.getRemoteAddr()`. If the request passes through Nginx, this can be the reverse-proxy IP rather than the testing client. R1 will group by whatever `src_ip` the Backend emits; **Python must not trust `X-Forwarded-For` or guess the original IP**.

A restricted Backend patch example is under `integration_proposals/`: `ClientIpResolver.java` (trust only an explicitly allowlisted direct proxy, not arbitrary forwarded headers) and `BACKEND_IP_AND_DOCKER_REVIEW.md`. Nhi should configure a gateway that *overwrites* `X-Real-IP` with `$remote_addr`, restrict direct Backend access, and confirm the real trusted proxy IP before enabling this change. Do not apply that patch to Backend without Trí/Cường review. **This gap cannot be marked fixed until the VM configuration is verified.**

## Group review and evidence

- **Cường/Trí:** confirm AuditLogger schema, how `actor_id` and `src_ip` are produced, HTTP 401/403 interpretation, and remaining login lockout design.
- **Khánh:** confirm which ticket-related 403/404 cases should be `AUTHZ_DENIED`.
- **Nhi/Chính:** review shared volume configuration, permissions, trusted proxy, VM resources, and log rotation.
- **Khiêm:** approve proposed threshold/tool/retention and R-01 before marking T3-10 Approved.

This version's `evidence/T3-03` notes are **offline test evidence only**, not EV-HD-22. A lab test requires actual build/commit, timestamp, tested role, response status, scrubbed audit log and alert, Actual/Result, and a Defect Log entry for incomplete Backend lockout.
