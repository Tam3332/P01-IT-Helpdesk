"""T5-03 tests for Cường week-5 Backend lockout audit contract.

All data synthetic and restricted to offline processing. No requests to VM.
"""
import importlib.util
import json
from datetime import datetime, timedelta, timezone
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('monitor_t5', ROOT / 'monitor.py')
monitor = importlib.util.module_from_spec(spec)
spec.loader.exec_module(monitor)
BASE = datetime(2026, 11, 17, 3, 0, tzinfo=timezone.utc)


def event(event_type, second=0, **overrides):
    value = {
        'timestamp': monitor.iso_z(BASE + timedelta(seconds=second)),
        'event_type': event_type,
        'actor_id': 2,
        'role': 'EMPLOYEE',
        'target_type': 'USER_EMAIL',
        'target_id': 'employee@example.test',
        'action': 'LOGIN',
        'outcome': 'FAILURE',
        'src_ip': '192.0.2.10',
        'correlation_id': f'test-{second}',
        'http_status': 401,
    }
    value.update(overrides)
    return value


class Week5LockoutTests(unittest.TestCase):
    def test_four_401_then_429_then_account_locked_one_alert(self):
        d = monitor.Detector()
        entries = [event('LOGIN_FAILED', i*10) for i in range(4)]
        entries += [event('LOGIN_FAILED', 40, http_status=429),
                    event('ACCOUNT_LOCKED', 40, http_status=429),
                    event('LOGIN_FAILED', 50, actor_id=None, http_status=429),
                    event('LOGIN_FAILED', 60, actor_id=None, http_status=429)]
        alerts = [d.process(e) for e in entries]
        alerts = [a for a in alerts if a]
        self.assertEqual(len(alerts), 1)
        self.assertEqual(alerts[0]['event_count'], 5)
        self.assertEqual(alerts[0]['source_event_type'], 'ACCOUNT_LOCKED')

    def test_legacy_brute_force_detected_alias(self):
        d = monitor.Detector()
        for i in range(4):
            self.assertIsNone(d.process(event('LOGIN_FAILED', i * 10)))
        self.assertIsNone(d.process(event('LOGIN_FAILED', 40, http_status=429)))
        alert = d.process(event('BRUTE_FORCE_DETECTED', 41, http_status=429))
        self.assertEqual(alert['rule_id'], 'R1_BRUTE_FORCE_LOGIN')
        self.assertEqual(d.counts['alerts'], 1)

    def test_old_backend_five_401_and_lock_event_no_duplicate(self):
        d = monitor.Detector()
        results = [d.process(event('LOGIN_FAILED', i * 10)) for i in range(5)]
        results += [d.process(event('BRUTE_FORCE_DETECTED', 40, http_status=429)),
                    d.process(event('ACCOUNT_LOCKED', 40, http_status=429))]
        self.assertEqual(sum(bool(x) for x in results), 1)

    def test_no_lock_event_four_401_one_429_no_false_pass(self):
        d = monitor.Detector()
        for i in range(4):
            d.process(event('LOGIN_FAILED', i * 10))
        self.assertIsNone(d.process(event('LOGIN_FAILED', 40, http_status=429)))
        self.assertEqual(d.counts['alerts'], 0)

    def test_lock_event_without_prior_four_does_not_claim_five_observed(self):
        d = monitor.Detector()
        self.assertIsNone(d.process(event('ACCOUNT_LOCKED', 40, http_status=429)))
        self.assertEqual(d.counts['alerts'], 0)

    def test_wrong_ip_not_grouped(self):
        d = monitor.Detector()
        for i in range(4):
            d.process(event('LOGIN_FAILED', i * 10))
        self.assertIsNone(d.process(event('ACCOUNT_LOCKED', 40, src_ip='192.0.2.11', http_status=429)))

    def test_unknown_email_masked_and_grouped(self):
        d = monitor.Detector()
        for i in range(4):
            d.process(event('LOGIN_FAILED', i * 10, actor_id=None, target_id='UNKNOWN@EXAMPLE.TEST '))
        a = d.process(event('ACCOUNT_LOCKED', 40, actor_id=None, http_status=429,
                            target_id='unknown@example.test'))
        self.assertEqual(a['event_count'], 5)
        self.assertNotIn('unknown@example.test', json.dumps(a))
        self.assertTrue(a['login_ref'].startswith('email_sha256:'))

    def test_four_bad_password_and_disabled_account_403_not_counted(self):
        d = monitor.Detector()
        for i in range(4):
            d.process(event('LOGIN_FAILED', i * 10))
        self.assertIsNone(d.process(event('LOGIN_FAILED', 40, http_status=403)))
        self.assertEqual(d.counts['alerts'], 0)

    def test_r2_unchanged_with_sla_event(self):
        d = monitor.Detector()
        a = []
        for i in range(3):
            a.append(d.process(event('AUTHZ_DENIED', i*10, actor_id=4, role='EMPLOYEE',
                                   target_type='TICKET', target_id='HD-2026-0009',
                                   action='VIEW', outcome='DENIED', http_status=404)))
        self.assertEqual(a[-1]['rule_id'], 'R2_UNAUTHORIZED_TICKET_ACCESS')
        self.assertIsNone(d.process(event('SLA_BREACHED', 40, target_type='TICKET',
                                          action='MARK_SLA_BREACHED', outcome='SUCCESS',
                                          http_status=None, actor_id=None)))

    def test_offline_replay_week5_fixture(self):
        d = monitor.Detector()
        with tempfile.TemporaryDirectory() as tmp:
            infile = Path(tmp)/'audit.jsonl'
            outfile = Path(tmp)/'alerts.jsonl'
            entries = [event('LOGIN_FAILED', i*10) for i in range(4)]
            entries.extend([event('LOGIN_FAILED', 40, http_status=429),
                            event('ACCOUNT_LOCKED', 40, http_status=429)])
            infile.write_text(''.join(json.dumps(x)+'\n' for x in entries), encoding='utf-8')
            self.assertEqual(monitor.run_once(infile, outfile, d), 1)
            alerts = [json.loads(x) for x in outfile.read_text(encoding='utf-8').splitlines()]
            self.assertEqual(len(alerts), 1)
            self.assertEqual(alerts[0]['event_count'], 5)


if __name__ == '__main__':
    unittest.main()
