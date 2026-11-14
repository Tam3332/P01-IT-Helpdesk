# EV-HD-24 - SEC-TC-HD-04 - BEFORE

## 1. Test environment

- Task: T4-03
- Test case: SEC-TC-HD-04
- Phase: BEFORE
- Repository commit: a868b63
- Source lab build: v0.4-week4 (a378fdb)
- Input: evidence/T4-02/audit_events_lab_20261112.txt
- Test method: Offline replay of real VM audit events
- Monitoring: Python Monitoring v1

## 2. Audit log validation

| Check | Actual | Result |
|---|---|---|
| Total audit events | 33 | PASS |
| Missing required fields | 0 | PASS |
| Invalid UTC timestamp format | 0 | PASS |
| Empty src_ip | 0 | PASS |
| Empty correlation_id | 0 | PASS |
| Events containing before/after | 0 | GAP |
| Client IP authenticity | Not verified | NOT RUN |

## 3. Event distribution

| Event type | Count |
|---|---:|
| AUTHZ_DENIED | 31 |
| REOPEN_REQUESTED | 1 |
| REOPEN_REJECTED | 1 |
| TOTAL | 33 |

## 4. Monitoring results

| Check | Actual | Result |
|---|---|---|
| Processed events | 33 | PASS |
| Skipped events | 0 | PASS |
| R2 qualifying denials | 21 | PASS |
| R2 alerts generated | 3 | PASS |
| Distinct alerted actors | 3 | PASS |
| R2 threshold | 3 events / 300 seconds | PASS |
| R1 login detection | No login test data | NOT RUN |
| Live Monitoring on VM | Not verified | NOT RUN |

## 5. Known gaps

- BEFORE audit records do not contain before/after.
- Selected Backend audit events are absent in the tested main commit.
- R1 requires a separate authentication test dataset.
- Real client IP provenance has not been established.
- Monitoring sidecar execution on the VM is not verified.
- AFTER retest requires deployment of the updated Backend.

## 6. Conclusion

Result: PARTIAL PASS (BEFORE).

The Monitoring v1 detector successfully replayed 33 real
lab audit events, skipped 0 events, and generated 3 R2 alerts.
This verifies offline parsing and detection only.

Full SEC-TC-HD-04 acceptance remains pending live lab
verification and an AFTER retest.

## 7. Supporting evidence

- offline_replay_real_lab_before.txt
- schema_check_before.txt
- EV-HD-24_alerts_before_redacted.csv
- evidence/T4-02/audit_events_lab_20261112.txt
