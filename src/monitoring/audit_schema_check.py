#!/usr/bin/env python3
"""T5-03 / SEC-TC-HD-04: summarize Backend audit schema without copying secrets.

Input: JSONL from authorized lab, kept local only. Output: aggregate, redacted
Markdown report (no raw actor, email, IP, ticket ID, request body or log lines).
This is a precheck; it cannot prove that an event is semantically correct.
"""
from __future__ import annotations

import argparse
from collections import Counter
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import re
from typing import Any

REQUIRED = (
    'timestamp', 'event_type', 'actor_id', 'role', 'target_type',
    'target_id', 'action', 'outcome', 'src_ip', 'correlation_id', 'http_status'
)
CHANGE_EVENTS = frozenset({
    'TICKET_ASSIGNED', 'TICKET_STATUS_CHANGED', 'TICKET_CLOSED',
    'REOPEN_REQUESTED', 'REOPEN_APPROVED', 'REOPEN_REJECTED',
    'ROLE_CHANGED', 'SLA_BREACHED',
})
CREATION_EVENTS = frozenset({'FILE_UPLOADED', 'COMMENT_ADDED'})
SENSITIVE_FIELD_KEYS = frozenset({
    'password', 'password_hash', 'secret', 'jwt', 'access_token', 'refresh_token',
    'authorization', 'bearer_token', 'token', 'file_content', 'comment_text',
    'original_filename', 'filename', 'file_bytes',
})
JWT_PATTERN = re.compile(r'\beyJ[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\.[A-Za-z0-9_-]{5,}\b')
UUID_PATTERN = re.compile(r'^(?:[0-9a-fA-F]{32}|[0-9a-fA-F]{8}(?:-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12})$')


def timestamp_valid(value: Any) -> bool:
    if not isinstance(value, str):
        return False
    try:
        dt = datetime.fromisoformat(value.replace('Z', '+00:00'))
        return dt.tzinfo is not None and dt.utcoffset().total_seconds() == 0
    except (ValueError, TypeError, AttributeError):
        return False


def contains_sensitive_field(item: Any) -> bool:
    """Detect forbidden field names or JWT-shaped values, without echoing them."""
    if isinstance(item, dict):
        for key, val in item.items():
            if str(key).lower() in SENSITIVE_FIELD_KEYS or contains_sensitive_field(val):
                return True
    elif isinstance(item, list):
        return any(contains_sensitive_field(value) for value in item)
    elif isinstance(item, str):
        return bool(JWT_PATTERN.search(item))
    return False


def check(source: Path) -> dict[str, Any]:
    digest = hashlib.sha256()
    counts: Counter[str] = Counter()
    issues: Counter[str] = Counter()
    n = 0
    if not source.is_file():
        raise FileNotFoundError(source)
    with source.open('rb') as f:
        for raw in f:
            digest.update(raw)
            if not raw.strip():
                continue
            try:
                ev = json.loads(raw.decode('utf-8-sig'))
                if not isinstance(ev, dict):
                    raise ValueError('JSONL row must be an object')
            except (ValueError, UnicodeDecodeError):
                issues['invalid_jsonl'] += 1
                continue
            n += 1
            event_type = ev.get('event_type')
            if not isinstance(event_type, str) or not event_type:
                issues['missing_event_type'] += 1
                event_type = '(missing)'
            counts[event_type] += 1
            if any(k not in ev for k in REQUIRED):
                issues['missing_required_field'] += 1
            if not timestamp_valid(ev.get('timestamp')):
                issues['timestamp_not_utc'] += 1
            if not isinstance(ev.get('correlation_id'), str) or not UUID_PATTERN.fullmatch(ev.get('correlation_id')):
                issues['invalid_correlation_uuid'] += 1
            if isinstance(ev.get('actor_id'), bool) or ev.get('actor_id') is not None and (not isinstance(ev.get('actor_id'), int) or ev['actor_id'] <= 0):
                issues['actor_id_invalid'] += 1
            if contains_sensitive_field(ev):
                issues['sensitive_field_or_jwt_pattern'] += 1
            if event_type in CHANGE_EVENTS and ev.get('outcome') == 'SUCCESS':
                if not isinstance(ev.get('before'), dict) or not isinstance(ev.get('after'), dict):
                    issues['before_after_missing_for_change'] += 1
            if event_type in CREATION_EVENTS and ev.get('outcome') == 'SUCCESS':
                if not isinstance(ev.get('after'), dict):
                    issues['after_missing_for_creation'] += 1
            if event_type == 'FILE_UPLOAD_REJECTED' and not isinstance(ev.get('reason'), str):
                issues['reject_reason_missing'] += 1
            if event_type == 'SLA_BREACHED':
                if ev.get('outcome') != 'SUCCESS':
                    issues['sla_nonstandard_outcome'] += 1
                before, after = ev.get('before'), ev.get('after')
                if not isinstance(before, dict) or not isinstance(after, dict) or before.get('is_sla_breached') is not False or after.get('is_sla_breached') is not True:
                    issues['sla_transition_invalid'] += 1
            if event_type == 'CHANGE_ROLE':
                issues['legacy_change_role_event'] += 1
    return {'sha256':digest.hexdigest(),'events':n,'event_counts':dict(sorted(counts.items())),
            'issues':dict(sorted(issues.items()))}


def format_report(result: dict[str, Any], mode: str) -> str:
    out = [
        '# SEC-TC-HD-04 — Audit schema check (tổng hợp đã che)',
        '',
        f'**Chế độ:** {mode.upper()} — {"chưa phải evidence live VM" if mode=="before" else "chỉ là kiểm tra schema, cần đối chiếu với thao tác live"}',
        f'**SHA-256 log đầu vào:** `{result["sha256"]}`',
        f'**Sự kiện JSON hợp lệ:** {result["events"]}',
        '',
        '## Loại event và số lượng',
        '',
        '| Event type | Số lượng |', '|---|---:|',
    ]
    for name,number in result['event_counts'].items():
        # Prevent untrusted event_type from injecting Markdown or URLs.
        safe = re.sub(r'[^A-Za-z0-9_()-]', '_', name)[:80]
        out.append(f'| `{safe}` | {number} |')
    out += ['', '## Dấu hiệu cần xem xét', '', '| Loại vấn đề | Số sự kiện |', '|---|---:|']
    if result['issues']:
        out += [f'| `{name}` | {number} |' for name,number in result['issues'].items()]
    else:
        out.append('| Không phát hiện lỗi schema theo bộ kiểm tra này | 0 |')
    out += [
        '',
        '## Giới hạn',
        '',
        '- Báo cáo không chứa email, actor ID, IP gốc, mã ticket, correlation ID cụ thể, nội dung bình luận/tệp hoặc dòng log gốc.',
        '- PASS schema **không** đồng nghĩa event đã sinh đúng từ thao tác thực tế, sidecar đã nhận/ghi alert hoặc build VM đã đạt.',
        '- Cần đối chiếu thao tác ↔ ticket_history ↔ Audit JSONL ↔ kết quả alert trên đúng commit/tag VM.',
        '- Các sự kiện tạo mới có thể chỉ có `after`; event đổi trạng thái yêu cầu `before` + `after`. ',
        '- Log gateway bị chặn trước Backend sẽ không có dòng Backend audit tương ứng.',
        '',
    ]
    return '\n'.join(out)


def main() -> int:
    p = argparse.ArgumentParser(description='Aggregate-only Audit JSONL schema checker')
    p.add_argument('--input', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--mode', choices=('before','after'), default='before')
    a=p.parse_args()
    if a.input.resolve()==a.output.resolve():
        p.error('input and output must differ')
    try:
        res=check(a.input)
        a.output.parent.mkdir(parents=True,exist_ok=True)
        a.output.write_text(format_report(res,a.mode),encoding='utf-8')
    except (OSError, ValueError) as e:
        p.error(str(e))
    print('events:',res['events'],'issues:',res['issues'],'source_sha256:',res['sha256'])
    return 0

if __name__=='__main__':
    raise SystemExit(main())
