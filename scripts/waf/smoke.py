#!/usr/bin/env python3
"""Bounded GET-only WAF probes for an authorized P01 lab. No login or seed writes.

These probes do not replace authenticated P1/P3, upload, privacy or R3 acceptance.
"""
import argparse
from datetime import datetime, timezone
import json
from pathlib import Path
import ssl
import sys
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode, urlsplit
from urllib.request import Request, build_opener, HTTPSHandler, HTTPRedirectHandler

PROBES = [
    ("VALID-HOME", "/", None, None, 200),
    ("VALID-TEXT", "/", "Ghi chú C++: a < b && c > d; cấu hình JSON", None, 200),
    ("XSS", "/", "<script>alert(1)</script>", None, "attack"),
    ("SQLI", "/", "1 UNION SELECT username,password FROM users--", None, "attack"),
    ("TRAVERSAL", "/", "../../../../etc/passwd", None, "attack"),
    ("COMMAND", "/", ";cat /etc/passwd", None, "attack"),
    ("SCANNER-UA", "/", None, "sqlmap/1.9", "attack"),
    ("DOCS", "/v3/api-docs", None, None, 404),
    ("SWAGGER", "/swagger-ui.html", None, None, 404),
    ("ACTUATOR", "/actuator/health", None, None, 404),
    ("AUTH-REQUIRED", "/api/v1/tickets", None, None, 401),
]

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def probe(opener, base, mode, item):
    name, path, payload, ua, expected = item
    if expected == "attack":
        expected = 403 if mode == "On" else 200
    url = base.rstrip("/") + path
    if payload is not None:
        url += "?" + urlencode({"t615_probe": payload})
    request = Request(url, headers={"User-Agent": ua or "P01-T615-lab-probe/1.0", "Accept": "application/json,text/html"})
    try:
        try:
            response = opener.open(request, timeout=15)
        except HTTPError as error:
            response = error
        with response:
            status = response.code
            body = response.read(65536)
            rid = response.headers.get("X-Request-Id")
            content_type = response.headers.get("Content-Type", "")
        code = None
        body_rid = None
        try:
            parsed = json.loads(body)
            if isinstance(parsed, dict):
                code, body_rid = parsed.get("code"), parsed.get("requestId")
        except (ValueError, UnicodeError):
            pass
        passed = status == expected and bool(rid)
        if expected == 403:
            passed = passed and code == "GATEWAY_FORBIDDEN" and body_rid == rid and "application/json" in content_type
        return {"case": name, "expected_http": expected, "actual_http": status, "request_id": rid,
            "json_code": code, "status": "PASS" if passed else "FAIL"}
    except (URLError, OSError):
        return {"case": name, "expected_http": expected, "actual_http": None, "status": "ERROR", "error": "TLS/network failure (URL and raw response omitted)"}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", required=True)
    parser.add_argument("--mode", choices=["DetectionOnly", "On"], required=True)
    parser.add_argument("--sha", required=True)
    parser.add_argument("--run-id", required=True)
    parser.add_argument("--output", type=Path, required=True)
    tls = parser.add_mutually_exclusive_group()
    tls.add_argument("--ca-file")
    tls.add_argument("--insecure-lab", action="store_true", help="Only for the lab's self-signed certificate")
    args = parser.parse_args()
    base = urlsplit(args.base_url)
    if base.scheme != "https" or not base.hostname or base.username or base.password or base.query or base.fragment or base.path not in ("", "/"):
        parser.error("Use an HTTPS lab origin without credentials, path, query or fragment")
    if len(args.sha) != 40 or any(c not in "0123456789abcdef" for c in args.sha):
        parser.error("--sha must be the full lowercase Git SHA")
    if args.output.exists():
        parser.error("Output exists; choose a fresh run filename")
    context = ssl._create_unverified_context() if args.insecure_lab else ssl.create_default_context(cafile=args.ca_file)
    opener = build_opener(HTTPSHandler(context=context), NoRedirect())
    cases = [probe(opener, args.base_url, args.mode, item) for item in PROBES]
    report = {"run_id": args.run_id, "source_sha_declared": args.sha, "waf_mode_declared": args.mode,
        "observed_at": datetime.now(timezone.utc).isoformat(), "tls_verification": not args.insecure_lab,
        "scope": "11 GET-only probes; mode/SHA must be verified separately in container manifest",
        "cases": cases, "acceptance_not_run": ["authenticated P1/P3", "upload near 10 MB", "UI", "audit privacy", "R3", "CPU/RAM", "rollback"]}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    failures = sum(c["status"] != "PASS" for c in cases)
    print(json.dumps({"probes": len(cases), "failed": failures, "output": str(args.output)}))
    return 1 if failures else 0

if __name__ == "__main__":
    sys.exit(main())
