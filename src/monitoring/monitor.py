#!/usr/bin/env python3
"""P01 T3-03: IA-HD05 Security Audit Monitoring v1 (stdlib only).

Matches the *current* Cường/Trí Backend contract (cuong/be, 2026-11-05):
  LOGIN_FAILED: actor_id=<int|null>, target_type=USER_EMAIL, target_id=<email>, http_status=401
  LOGIN_SUCCESS: actor_id=<int>, target_type=USER, target_id=<userId>, http_status=200
  AUTHZ_DENIED: actor_id=<int>, target_type=TICKET or ENDPOINT, http_status=403/404
All of these carry timestamp UTC, src_ip, outcome, action and correlation_id.

Backward-compatible with v0 synthetic logs carrying login_ref. This program only
reads logs and writes alerts. Lockout, HTTP 429 and client IP provenance are Backend
and gateway responsibilities, not implemented by Monitoring.
"""
from __future__ import annotations

import argparse
from collections import defaultdict, deque
from datetime import datetime, timedelta, timezone
import hashlib
import json
from pathlib import Path
import re
import sys
import time
from typing import Any

WINDOW = timedelta(minutes=5)
LOGIN_THRESHOLD = 5   # SRS HD01/UC-01: 5 consecutive failures within 5 min
AUTHZ_THRESHOLD = 3   # IA-HD05: Proposed; needs team approval
TICKET_API = re.compile(r"^(?:GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\s+/api/v1/tickets(?:/|\?|$)", re.I)


def parse_utc(value: Any) -> datetime:
    if not isinstance(value, str):
        raise ValueError("timestamp must be ISO-8601 UTC string")
    dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
    if dt.tzinfo is None or dt.utcoffset() != timedelta(0):
        raise ValueError("timestamp must include UTC timezone (Z or +00:00)")
    return dt.astimezone(timezone.utc)


def iso_z(dt: datetime) -> str:
    return dt.isoformat(timespec="seconds").replace("+00:00", "Z")


def actor_key(value: Any) -> str | None:
    """Jackson serializes user IDs as numbers. Retain string IDs from v0 data."""
    if isinstance(value, bool) or value is None:
        return None
    if isinstance(value, int):
        return str(value) if value > 0 else None
    if isinstance(value, str) and value.strip():
        return value.strip()
    return None


def login_key(event: dict[str, Any]) -> str | None:
    """Stable login key compatible with Backend without adding a new field.

    Known account: actor_id (same numeric user ID on failed/successful login).
    Unknown email: hash normalized USER_EMAIL target_id *in memory* so different
    unknown emails do not merge and the alert does not expose raw email.
    Legacy v0: login_ref remains supported when the Backend actor is unavailable.
    """
    actor = actor_key(event.get("actor_id"))
    if actor is not None:
        return f"actor:{actor}"
    login_ref = event.get("login_ref")
    if isinstance(login_ref, str) and login_ref.strip():
        return login_ref.strip()  # existing v0 synthetic or future pseudonymous key
    if (event.get("target_type") or "").upper() == "USER_EMAIL":
        email = event.get("target_id")
        if isinstance(email, str) and email.strip():
            normalized = email.strip().casefold()
            return "email_sha256:" + hashlib.sha256(normalized.encode("utf-8")).hexdigest()
    return None


def is_ticket_denial(event: dict[str, Any]) -> bool:
    """Count only explicit authorization denials related to tickets.

    Backend TicketService logs target_type=TICKET even when it returns 404
    (anti-IDOR). Spring Security's AccessDeniedHandler logs target_type=ENDPOINT
    for role-based 403; count it only when its URL is a tickets API.
    An ordinary 404 without AUTHZ_DENIED must never be counted.
    """
    if event.get("event_type") != "AUTHZ_DENIED":
        return False
    if event.get("outcome") not in (None, "DENIED"):
        return False
    if event.get("http_status") not in (None, 403, 404):
        return False
    target = event.get("target_type")
    if target == "TICKET":
        return True
    if target == "ENDPOINT":
        identifier = event.get("target_id")
        return isinstance(identifier, str) and bool(TICKET_API.match(identifier))
    return False


class Detector:
    """Stateful sliding-window detector for timestamp-ordered events.

    No duplicate alerts while a window stays over threshold. Exact five-minute
    boundary counts as within the window. Running state is memory-only in v1.
    """

    def __init__(self, login_threshold: int = LOGIN_THRESHOLD, authz_threshold: int = AUTHZ_THRESHOLD):
        if login_threshold < 1 or authz_threshold < 1:
            raise ValueError("thresholds must be positive")
        self.login_threshold = login_threshold
        self.authz_threshold = authz_threshold
        self.failed: dict[tuple[str, str], deque[datetime]] = defaultdict(deque)
        self.denied: dict[str, deque[datetime]] = defaultdict(deque)
        self.counts = {"events": 0, "skipped": 0, "alerts": 0}

    @staticmethod
    def _trim(times: deque[datetime], now: datetime) -> None:
        while times and times[0] < now - WINDOW:
            times.popleft()

    def process(self, event: dict[str, Any]) -> dict[str, Any] | None:
        self.counts["events"] += 1
        event_type = event.get("event_type")
        if event_type not in {"LOGIN_SUCCESS", "LOGIN_FAILED", "AUTHZ_DENIED"}:
            # Backend may add BRUTE_FORCE_DETECTED in week 5. Do not double-alert.
            return None
        try:
            now = parse_utc(event.get("timestamp"))
        except (ValueError, TypeError, OverflowError):
            self.counts["skipped"] += 1
            return None

        if event_type in {"LOGIN_SUCCESS", "LOGIN_FAILED"}:
            # Backend logs disabled users with LOGIN_FAILED/403: not a bad password.
            # Only count actual invalid credentials (401); v0 synthetic logs
            # without http_status are accepted for backward compatibility.
            is_legacy_denied = (
                isinstance(event.get("login_ref"), str)
                and event.get("outcome") == "DENIED"
            )
            if event_type == "LOGIN_FAILED" and (
                event.get("http_status") not in (None, 401)
                or (event.get("outcome") not in (None, "FAILURE") and not is_legacy_denied)
            ):
                return None
            key_id = login_key(event)
            ip = event.get("src_ip")
            if key_id is None or not isinstance(ip, str) or not ip.strip():
                self.counts["skipped"] += 1
                return None
            key = (key_id, ip.strip())
            if event_type == "LOGIN_SUCCESS":
                self.failed.pop(key, None)
                return None
            times = self.failed[key]
            self._trim(times, now)
            prior = len(times)
            times.append(now)
            if prior < self.login_threshold <= len(times):
                self.counts["alerts"] += 1
                return {
                    "rule_id": "R1_BRUTE_FORCE_LOGIN", "severity": "HIGH",
                    "detected_at": iso_z(now), "event_type": "SECURITY_ALERT",
                    "login_ref": key_id,  # actor ID or SHA-256, never the raw email
                    "src_ip": ip.strip(), "event_count": len(times),
                    "window_seconds": int(WINDOW.total_seconds()),
                    "first_event_at": iso_z(times[0]), "last_event_at": iso_z(now),
                    "source_event_type": "LOGIN_FAILED", "status": "DETECTED",
                }
            return None

        if not is_ticket_denial(event):
            return None
        actor = actor_key(event.get("actor_id"))
        if actor is None:
            self.counts["skipped"] += 1
            return None
        times = self.denied[actor]
        self._trim(times, now)
        prior = len(times)
        times.append(now)
        if prior < self.authz_threshold <= len(times):
            self.counts["alerts"] += 1
            return {
                "rule_id": "R2_UNAUTHORIZED_TICKET_ACCESS", "severity": "MEDIUM",
                "detected_at": iso_z(now), "event_type": "SECURITY_ALERT",
                "actor_id": actor, "src_ip": event.get("src_ip"),
                "event_count": len(times), "window_seconds": int(WINDOW.total_seconds()),
                "first_event_at": iso_z(times[0]), "last_event_at": iso_z(now),
                "source_event_type": "AUTHZ_DENIED", "target_type": "TICKET",
                "status": "DETECTED",
            }
        return None


def decode_line(line: str, line_number: int) -> dict[str, Any] | None:
    if not line.strip():
        return None
    try:
        item = json.loads(line)
        if not isinstance(item, dict):
            raise ValueError("JSON value is not an object")
        return item
    except (json.JSONDecodeError, ValueError) as exc:
        print(f"[monitor] skip invalid JSONL at line {line_number}: {exc}", file=sys.stderr)
        return None


def run_once(source: Path, output: Path, detector: Detector) -> int:
    """Offline replay replaces output instead of accumulating duplicate alerts."""
    if source.resolve() == output.resolve():
        raise ValueError("input and output paths must differ")
    if not source.is_file():
        raise FileNotFoundError(source)
    output.parent.mkdir(parents=True, exist_ok=True)
    with source.open("r", encoding="utf-8") as inp, output.open("w", encoding="utf-8") as out:
        for line_no, line in enumerate(inp, 1):
            event = decode_line(line, line_no)
            if event is None:
                continue
            alert = detector.process(event)
            if alert is not None:
                out.write(json.dumps(alert, ensure_ascii=False, separators=(",", ":")) + "\n")
    return detector.counts["alerts"]


def follow(source: Path, output: Path, detector: Detector, interval: float = 0.5,
           from_start: bool = False) -> None:
    """Watch appended JSONL; handle rotation/truncation; restart has no checkpoint."""
    if source.resolve() == output.resolve():
        raise ValueError("input and output paths must differ")
    output.parent.mkdir(parents=True, exist_ok=True)
    print(f"[monitor] watch {source}; append alerts to {output}", file=sys.stderr)
    fp = None
    inode = None
    line_no = 0
    start_at_beginning = from_start
    try:
        with output.open("a", encoding="utf-8") as out:
            while True:
                try:
                    st = source.stat()
                except FileNotFoundError:
                    time.sleep(interval)
                    continue
                if fp is None or inode != (st.st_dev, st.st_ino) or st.st_size < fp.tell():
                    if fp is not None:
                        fp.close()
                    fp = source.open("r", encoding="utf-8")
                    inode = (st.st_dev, st.st_ino)
                    if not start_at_beginning:
                        fp.seek(0, 2)
                    start_at_beginning = True
                    line_no = 0
                position = fp.tell()
                line = fp.readline()
                if not line:
                    time.sleep(interval)
                    continue
                if not line.endswith("\n"):
                    fp.seek(position)
                    time.sleep(interval)
                    continue
                line_no += 1
                event = decode_line(line, line_no)
                if event is None:
                    continue
                alert = detector.process(event)
                if alert is not None:
                    out.write(json.dumps(alert, ensure_ascii=False, separators=(",", ":")) + "\n")
                    out.flush()
                    print(f"[monitor] alert {alert['rule_id']} {alert['detected_at']}", file=sys.stderr)
    finally:
        if fp is not None:
            fp.close()


def cli() -> int:
    parser = argparse.ArgumentParser(description="P01 Security Audit JSONL Monitoring (offline/streaming)")
    parser.add_argument("--input", required=True, type=Path, help="Backend audit_events.jsonl")
    parser.add_argument("--output", required=True, type=Path, help="Monitoring alerts.jsonl")
    parser.add_argument("--follow", action="store_true", help="Follow appended events")
    parser.add_argument("--from-start", action="store_true", help="Read existing log from beginning")
    parser.add_argument("--interval", type=float, default=0.5, help="Polling interval seconds")
    parser.add_argument("--login-threshold", type=int, default=LOGIN_THRESHOLD)
    parser.add_argument("--authz-threshold", type=int, default=AUTHZ_THRESHOLD)
    args = parser.parse_args()
    if args.interval <= 0:
        parser.error("--interval must be positive")
    detector = Detector(args.login_threshold, args.authz_threshold)
    try:
        if args.follow:
            follow(args.input, args.output, detector, args.interval, args.from_start)
        else:
            count = run_once(args.input, args.output, detector)
            print(f"offline: {detector.counts['events']} audit events, "
                  f"{detector.counts['skipped']} skipped, {count} alerts")
    except (ValueError, OSError) as exc:
        print(f"[monitor] error: {exc}", file=sys.stderr)
        return 1
    except KeyboardInterrupt:
        print("\n[monitor] stopped", file=sys.stderr)
    return 0

if __name__ == "__main__":
    sys.exit(cli())
