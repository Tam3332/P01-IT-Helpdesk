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


def ev(kind, minute=0, **fields):
    return {"timestamp": monitor.iso_z(BASE + timedelta(minutes=minute)),
            "event_type": kind, **fields}


class DetectorTests(unittest.TestCase):
    def test_r1_exact_threshold_and_no_duplicate(self):
        d = monitor.Detector()
        events = [ev("LOGIN_FAILED", i, login_ref="email-hash-A", src_ip="192.0.2.10") for i in (0, 1, 2, 3, 4, 4)]
        alerts = [d.process(e) for e in events]
        self.assertEqual([x["rule_id"] for x in alerts if x], ["R1_BRUTE_FORCE_LOGIN"])
        self.assertEqual(alerts[4]["event_count"], 5)

    def test_r1_group_by_login_ref_and_ip(self):
        d = monitor.Detector()
        for i in range(4):
            self.assertIsNone(d.process(ev("LOGIN_FAILED", i, login_ref="A", src_ip="192.0.2.10")))
        self.assertIsNone(d.process(ev("LOGIN_FAILED", 4, login_ref="A", src_ip="192.0.2.11")))
        self.assertIsNone(d.process(ev("LOGIN_FAILED", 4, login_ref="B", src_ip="192.0.2.10")))

    def test_success_resets_sequence(self):
        d = monitor.Detector()
        for i in range(4):
            d.process(ev("LOGIN_FAILED", i, login_ref="A", src_ip="192.0.2.10"))
        d.process(ev("LOGIN_SUCCESS", 4, login_ref="A", src_ip="192.0.2.10"))
        self.assertIsNone(d.process(ev("LOGIN_FAILED", 5, login_ref="A", src_ip="192.0.2.10")))

    def test_window_expiry(self):
        d = monitor.Detector()
        for i in (0, 1, 2, 3):
            d.process(ev("LOGIN_FAILED", i, login_ref="A", src_ip="192.0.2.10"))
        self.assertIsNone(d.process(ev("LOGIN_FAILED", 10, login_ref="A", src_ip="192.0.2.10")))

    def test_r2_only_tickets_same_actor(self):
        d = monitor.Detector()
        self.assertIsNone(d.process(ev("AUTHZ_DENIED", 0, actor_id="u1", target_type="TICKET", http_status=404)))
        self.assertIsNone(d.process(ev("AUTHZ_DENIED", 1, actor_id="u2", target_type="TICKET", http_status=404)))
        self.assertIsNone(d.process(ev("AUTHZ_DENIED", 2, actor_id="u1", target_type="USER", http_status=403)))
        self.assertIsNone(d.process(ev("RESOURCE_NOT_FOUND", 2, actor_id="u1", target_type="TICKET", http_status=404)))
        self.assertIsNone(d.process(ev("AUTHZ_DENIED", 3, actor_id="u1", target_type="TICKET", http_status=404)))
        a = d.process(ev("AUTHZ_DENIED", 4, actor_id="u1", target_type="TICKET", http_status=403))
        self.assertEqual(a["rule_id"], "R2_UNAUTHORIZED_TICKET_ACCESS")

    def test_r2_no_repeat_until_window_rearmed(self):
        d = monitor.Detector()
        for t in (0, 1):
            self.assertIsNone(d.process(ev("AUTHZ_DENIED", t, actor_id="u1", target_type="TICKET")))
        self.assertIsNotNone(d.process(ev("AUTHZ_DENIED", 2, actor_id="u1", target_type="TICKET")))
        self.assertIsNone(d.process(ev("AUTHZ_DENIED", 3, actor_id="u1", target_type="TICKET")))
        self.assertIsNone(d.process(ev("AUTHZ_DENIED", 10, actor_id="u1", target_type="TICKET")))
        self.assertIsNone(d.process(ev("AUTHZ_DENIED", 11, actor_id="u1", target_type="TICKET")))
        self.assertIsNotNone(d.process(ev("AUTHZ_DENIED", 12, actor_id="u1", target_type="TICKET")))

    def test_missing_grouping_fields_and_time_skipped(self):
        d = monitor.Detector()
        self.assertIsNone(d.process(ev("LOGIN_FAILED", 0, src_ip="192.0.2.10")))
        self.assertIsNone(d.process({"event_type": "LOGIN_FAILED", "timestamp": "not-a-time", "login_ref": "A", "src_ip": "192.0.2.10"}))
        self.assertEqual(d.counts["skipped"], 2)

    def test_brute_force_event_not_second_alert(self):
        d = monitor.Detector()
        for i in range(4):
            d.process(ev("LOGIN_FAILED", i, login_ref="A", src_ip="192.0.2.10"))
        self.assertIsNotNone(d.process(ev("LOGIN_FAILED", 4, login_ref="A", src_ip="192.0.2.10")))
        self.assertIsNone(d.process(ev("BRUTE_FORCE_DETECTED", 4, login_ref="A", src_ip="192.0.2.10")))
        self.assertEqual(d.counts["alerts"], 1)

    def test_offline_replay_replaces_output_not_appends(self):
        d = monitor.Detector()
        with tempfile.TemporaryDirectory() as td:
            source, output = Path(td) / "audit.jsonl", Path(td) / "alerts.jsonl"
            with source.open("w", encoding="utf-8") as f:
                for i in range(5):
                    f.write(json.dumps(ev("LOGIN_FAILED", i, login_ref="A", src_ip="192.0.2.10")) + "\n")
            self.assertEqual(monitor.run_once(source, output, d), 1)
            self.assertEqual(monitor.run_once(source, output, monitor.Detector()), 1)
            self.assertEqual(len(output.read_text(encoding="utf-8").splitlines()), 1)


if __name__ == "__main__":
    unittest.main()
