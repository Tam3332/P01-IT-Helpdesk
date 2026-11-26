#!/usr/bin/env python3
"""Check captured lab logs for secret markers without printing matched content.

Pass disposable test values using a private canary file (one value per line).
A clean result covers these patterns and canaries only, not every possible PII.
"""
import argparse
import json
from pathlib import Path
import re
import sys

PATTERNS = {
    "authorization_header": re.compile(r"authorization[\s\"']*[:=]", re.I),
    "cookie_header": re.compile(r"(?:set-cookie|cookie)[\s\"']*[:=]", re.I),
    "jwt": re.compile(r"eyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}"),
    "password_field": re.compile(r"(?:password|passwd)[\s\"']*[:=]", re.I),
    "private_key": re.compile(r"-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----"),
}

def scan(text, canaries):
    found = [name for name, pattern in PATTERNS.items() if pattern.search(text)]
    if any(value in text for value in canaries):
        found.append("test_canary")
    return found

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", type=Path, action="append", required=True)
    parser.add_argument("--canary-file", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if args.output.exists():
        parser.error("Output exists; choose a fresh filename")
    canaries = [x for x in args.canary_file.read_text(encoding="utf-8").splitlines() if x]
    if not canaries or any(len(x) < 8 for x in canaries):
        parser.error("Use one or more unique disposable canaries, each at least 8 characters")
    results = []
    for file in args.input:
        if not file.is_file() or file.stat().st_size == 0:
            results.append({"file": file.name, "status": "MISSING_OR_EMPTY", "markers": []})
            continue
        raw = file.read_bytes()
        try:
            text = raw.decode("utf-8", errors="strict")
        except UnicodeError:
            results.append({"file": file.name, "status": "UNREADABLE_ENCODING", "markers": []})
            continue
        found = scan(text, canaries)
        results.append({"file": file.name, "status": "FAIL" if found else "PASS", "markers": found})
    report = {"scope": "pattern/canary screening only; validate audit coverage and review private logs manually",
        "files": results, "status": "PASS" if all(r["status"] == "PASS" for r in results) else "FAIL"}
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"status": report["status"], "files": len(results)}))
    return 0 if report["status"] == "PASS" else 1

if __name__ == "__main__":
    sys.exit(main())
