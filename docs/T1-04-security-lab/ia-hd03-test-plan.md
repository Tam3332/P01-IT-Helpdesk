# IA-HD03 — Secure File Upload Test Plan

Status: NOT EXECUTED
Evidence ID: EV-HD-23

## Preconditions
- Backend application deployed.
- Upload and download endpoints available.
- Test accounts with different roles and ticket permissions.
- Test tickets in Open and Closed states.
- Authorized test files available.

## Test Cases

### SEC-TC-HD-03.1 — File Type Validation
- Upload an allowed file within 10 MB.
- Upload a disallowed extension.
- Upload a file with mismatched MIME/content signature.
Expected: Valid file accepted; invalid files rejected with HTTP 400.

### SEC-TC-HD-03.2 — Size and Count Limits
- Upload a file exceeding 10 MB.
- Attempt to add a sixth attachment to one ticket.
Expected: HTTP 400; invalid files are not stored.

### SEC-TC-HD-03.3 — Filename and Path Safety
- Test filenames containing path traversal sequences.
- Verify filename normalization and storage boundaries.
Expected: No file can escape the permitted storage directory.

### SEC-TC-HD-03.4 — Download Authorization
- Download with an authorized account.
- Download with an account lacking permission.
- Request an attachment belonging to another ticket.
Expected: Authorized access succeeds; unauthorized access
is denied according to the documented 403/404 rules.
Download URL expiry/revocation testing is pending design.

### SEC-TC-HD-03.5 — Closed Ticket and Logging
- Attempt upload to a Closed ticket.
- Review upload/download audit logs.
Expected: Closed ticket upload returns HTTP 422.
Logs do not expose passwords or JWT values.

## Execution Status
All test cases: NOT EXECUTED.
Pending backend integration and approved endpoint design.
