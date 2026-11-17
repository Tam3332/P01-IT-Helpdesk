#!/usr/bin/env python3
"""T5-03: generate an offline, local-only alert dashboard (stdlib only).

This is an EXPORT of alerts.jsonl, not a live API nor proof of live delivery.
Source IP and actor/login references are replaced with per-export HMAC pseudonyms.
Does not contact a server, run JavaScript or load external resources.
"""
from __future__ import annotations

import argparse
from collections import Counter, deque
from datetime import datetime, timezone
import hashlib
import hmac
import html
import json
from pathlib import Path
import secrets

ALLOWED_RULES = {"R1_BRUTE_FORCE_LOGIN", "R2_UNAUTHORIZED_TICKET_ACCESS", "R3_WAF_BLOCKED"}


def load_alerts(path: Path, limit: int = 200) -> tuple[list[dict], int]:
    if limit < 1 or limit > 5000:
        raise ValueError('limit must be 1..5000')
    if not path.is_file():
        raise FileNotFoundError(path)
    rows: deque[dict] = deque(maxlen=limit)
    rejected = 0
    with path.open('r', encoding='utf-8-sig') as f:
        for line in f:
            if not line.strip():
                continue
            try:
                data = json.loads(line)
                if not isinstance(data, dict) or data.get('rule_id') not in ALLOWED_RULES:
                    raise ValueError('unsupported alert record')
                rows.append(data)
            except (ValueError, json.JSONDecodeError):
                rejected += 1
    return list(rows), rejected


def pseudonym(value: object, prefix: str, key: bytes) -> str:
    if value is None or not isinstance(value, (str, int)) or not str(value).strip():
        return '—'
    digest = hmac.new(key, str(value).encode('utf-8'), hashlib.sha256).hexdigest()[:8]
    return f'{prefix}#{digest}'


def make_html(alerts: list[dict], rejected: int, kind: str,
              key: bytes | None = None) -> str:
    key = key or secrets.token_bytes(32)
    counters = Counter(str(a['rule_id']) for a in alerts)
    heading = ('DEMO — DỮ LIỆU GIẢ LẬP' if kind == 'synthetic'
               else 'BÁO CÁO LOG PHÒNG LAB — CHƯA XÁC NHẬN PIPELINE LIVE')
    lines = []
    for a in alerts:
        values = [
            str(a.get('detected_at') or '—'),
            str(a['rule_id']),
            str(a.get('severity') or '—'),
            pseudonym(a.get('login_ref') or a.get('actor_id'), 'actor', key),
            pseudonym(a.get('src_ip'), 'ip', key),
            str(a.get('event_count', '—')),
            str(a.get('status') or '—'),
            str(a.get('source_event_type') or '—')
        ]
        lines.append('<tr>' + ''.join('<td>' + html.escape(v, quote=True) + '</td>' for v in values) + '</tr>')
    summary = ' · '.join(f'{html.escape(k)}: {v}' for k,v in sorted(counters.items())) or 'Không có alert hợp lệ'
    return f'''<!doctype html>
<html lang="vi"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; base-uri 'none'; form-action 'none'">
<title>P01 – Monitoring Alert Dashboard</title>
<style>
:root {{color-scheme: light; font-family: system-ui, sans-serif}}
body {{max-width: 1250px; margin: 2rem auto; padding: 0 1.2rem; background:#f8fafc; color:#0f172a}}
header {{border-left: 5px solid #2563eb; padding: 1rem; background:white}}
h1 {{font-size:1.5rem; margin:.3rem 0}} .meta {{color:#475569; margin:0.5rem 0}}
table {{border-collapse: collapse; width:100%; background:white; margin:1.3rem 0}}
th,td {{padding:.7rem; border-bottom:1px solid #cbd5e1; text-align:left; font-size:.9rem; overflow-wrap:anywhere}}
th {{background:#dbeafe}} tr:nth-child(even) {{background:#f1f5f9}}
small {{display:block; color:#475569; line-height:1.6}} .wrap {{overflow:auto}}
</style></head><body>
<header><strong>{html.escape(heading)}</strong><h1>P01 — Security Alert Dashboard</h1>
<p class="meta">T5-03 · IA-HD05 · EV-HD-22/24 · {len(alerts)} cảnh báo hiển thị · {rejected} dòng bỏ qua</p>
<p class="meta">{summary}</p></header>
<div class="wrap"><table><thead><tr><th>Thời gian (UTC)</th><th>Rule</th><th>Mức</th><th>Actor</th><th>IP</th><th>Số sự kiện</th><th>Trạng thái</th><th>Nguồn</th></tr></thead>
<tbody>{''.join(lines) if lines else '<tr><td colspan="8">Không có cảnh báo hợp lệ</td></tr>'}</tbody></table></div>
<small>Xuất báo cáo cục bộ từ alerts.jsonl, không kết nối VM và không tự cập nhật theo thời gian thực.<br>
Actor/IP dùng mã HMAC ngẫu nhiên riêng cho lần xuất này; không hiện email/IP gốc, mật khẩu hay token.<br>
Bảng dùng dữ liệu đã cung cấp, không chứng minh alerts.jsonl được ghi bởi sidecar trên VM.</small>
</body></html>'''


def main() -> int:
    p = argparse.ArgumentParser(description='Export redacted Monitoring alerts to an offline HTML dashboard')
    p.add_argument('--input', required=True, type=Path, help='alerts.jsonl (never raw audit log)')
    p.add_argument('--output', required=True, type=Path, help='local HTML output')
    p.add_argument('--source-kind', choices=('synthetic','lab'), default='synthetic')
    p.add_argument('--limit', type=int, default=200)
    a = p.parse_args()
    if a.input.resolve() == a.output.resolve():
        p.error('--input and --output must differ')
    try:
        alerts, rejected = load_alerts(a.input, a.limit)
        a.output.parent.mkdir(parents=True, exist_ok=True)
        a.output.write_text(make_html(alerts, rejected, a.source_kind), encoding='utf-8')
    except (OSError, ValueError) as exc:
        p.error(str(exc))
    counts = Counter(x['rule_id'] for x in alerts)
    print(f'exported {len(alerts)} alerts; rejected {rejected} invalid; rules={dict(counts)}; source={a.source_kind}')
    return 0

if __name__ == '__main__':
    raise SystemExit(main())
