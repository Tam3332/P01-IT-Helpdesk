"""Tests for JSONL shape produced by Cường/Trí's cuong/be AuditLogger.

No real authentication requests: all events are synthetic and stay offline.
"""
import importlib.util
from datetime import datetime, timedelta, timezone
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("monitor", ROOT / "monitor.py")
monitor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(monitor)
BASE = datetime(2026, 11, 5, 9, 0, tzinfo=timezone.utc)


def ev(kind, minute, **kwargs):
    row = {
        "timestamp": (BASE + timedelta(minutes=minute)).isoformat().replace("+00:00", "Z"),
        "event_type": kind,
        "actor_id": None,
        "role": None,
        "target_type": "USER_EMAIL",
        "target_id": "employee@helpdesk.local",
        "action": "LOGIN",
        "outcome": "FAILURE",
        "src_ip": "192.0.2.10",
        "correlation_id": f"req-{minute}",
        "http_status": 401,
    }
    row.update(kwargs)
    return row


class BackendContractTests(unittest.TestCase):
    def test_backend_known_actor_numeric_r1(self):
        d = monitor.Detector()
        entries = [ev("LOGIN_FAILED", m, actor_id=2) for m in range(5)]
        alerts = [d.process(e) for e in entries]
        self.assertEqual([a["rule_id"] for a in alerts if a], ["R1_BRUTE_FORCE_LOGIN"])
        self.assertEqual(alerts[-1]["login_ref"], "actor:2")
        self.assertNotIn("@", json.dumps(alerts[-1]))

    def test_success_resets_backend_actor_id(self):
        d = monitor.Detector()
        for m in range(4):
            d.process(ev("LOGIN_FAILED", m, actor_id=2))
        # Backend LOGIN_SUCCESS uses target_type USER, target_id "2", same actor_id.
        d.process(ev("LOGIN_SUCCESS", 4, actor_id=2, target_type="USER",
                     target_id="2", outcome="SUCCESS", http_status=200))
        self.assertIsNone(d.process(ev("LOGIN_FAILED", 5, actor_id=2)))

    def test_unknown_email_casefold_and_no_plain_email_in_alert(self):
        d = monitor.Detector()
        for m in range(4):
            self.assertIsNone(d.process(ev("LOGIN_FAILED", m, actor_id=None,
                                           target_id=" Unknown@Helpdesk.Local ")))
        alert = d.process(ev("LOGIN_FAILED", 4, actor_id=None, target_id="unknown@helpdesk.local"))
        self.assertEqual(alert["rule_id"], "R1_BRUTE_FORCE_LOGIN")
        self.assertTrue(alert["login_ref"].startswith("email_sha256:"))
        self.assertNotIn("unknown@helpdesk.local", json.dumps(alert))

    def test_disabled_user_403_not_bad_password(self):
        d = monitor.Detector()
        for m in range(4):
            d.process(ev("LOGIN_FAILED", m, actor_id=2))
        self.assertIsNone(d.process(ev("LOGIN_FAILED", 4, actor_id=2, http_status=403)))
        self.assertIsNotNone(d.process(ev("LOGIN_FAILED", 5, actor_id=2, http_status=401)))

    def test_backend_numeric_actor_r2_tickets_and_ticket_endpoint(self):
        d = monitor.Detector()
        rows = [
            ev("AUTHZ_DENIED", 10, actor_id=2, target_type="TICKET",
               target_id="HD-2026-001", action="VIEW_TICKET", outcome="DENIED", http_status=404),
            ev("AUTHZ_DENIED", 11, actor_id=2, target_type="ENDPOINT",
               target_id="PUT /api/v1/tickets/HD-2026-001/assign", action="PUT",
               outcome="DENIED", http_status=403),
            ev("AUTHZ_DENIED", 12, actor_id=2, target_type="TICKET",
               target_id="HD-2026-002", action="VIEW_TICKET", outcome="DENIED", http_status=404),
        ]
        alerts = [d.process(x) for x in rows]
        self.assertEqual(alerts[-1]["actor_id"], "2")
        self.assertEqual(alerts[-1]["event_count"], 3)

    def test_do_not_count_admin_endpoint_or_ordinary_404(self):
        d = monitor.Detector()
        x = ev("AUTHZ_DENIED", 0, actor_id=2, target_type="ENDPOINT",
               target_id="PUT /api/v1/admin/users/7/role", outcome="DENIED", http_status=403)
        self.assertIsNone(d.process(x))
        x = ev("RESOURCE_NOT_FOUND", 1, actor_id=2, target_type="TICKET",
               target_id="HD-2026-404", http_status=404)
        self.assertIsNone(d.process(x))
        x = ev("AUTHZ_DENIED", 2, actor_id=2, target_type="ENDPOINT",
               target_id="GET /api/v1/other", outcome="DENIED", http_status=403)
        self.assertIsNone(d.process(x))
        self.assertEqual(d.counts["alerts"], 0)

    def test_normalize_actor_id_reject_bool_or_float(self):
        self.assertEqual(monitor.actor_key(2), "2")
        self.assertEqual(monitor.actor_key("user_002"), "user_002")
        for x in [False, True, 2.1, None, ""]:
            self.assertIsNone(monitor.actor_key(x))

    def test_legacy_v0_sample_still_generates_two_alerts(self):
        source = ROOT / "examples" / "audit.sample.jsonl"
        with tempfile.TemporaryDirectory() as td:
            output = Path(td) / "alerts.jsonl"
            detector = monitor.Detector()
            n = monitor.run_once(source, output, detector)
            data = [json.loads(s) for s in output.read_text(encoding="utf-8").splitlines()]
        self.assertEqual(n, 2)
        self.assertEqual([a["rule_id"] for a in data], [
            "R1_BRUTE_FORCE_LOGIN", "R2_UNAUTHORIZED_TICKET_ACCESS"])

    def test_backend_contract_fixture_generates_two_alerts(self):
        source = ROOT / "examples" / "audit.backend.sample.jsonl"
        with tempfile.TemporaryDirectory() as td:
            output = Path(td) / "alerts.jsonl"
            detector = monitor.Detector()
            n = monitor.run_once(source, output, detector)
            data = [json.loads(s) for s in output.read_text(encoding="utf-8").splitlines()]
        self.assertEqual(n, 2)
        self.assertEqual([a["rule_id"] for a in data], [
            "R1_BRUTE_FORCE_LOGIN", "R2_UNAUTHORIZED_TICKET_ACCESS"])
        self.assertEqual(detector.counts["skipped"], 0)


if __name__ == "__main__":
    unittest.main()
