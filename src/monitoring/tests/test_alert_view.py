"""Local export tests: never include sensitive identifiers or execute markup."""
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('alert_view', ROOT / 'alert_view.py')
view = importlib.util.module_from_spec(spec)
spec.loader.exec_module(view)

class AlertViewTests(unittest.TestCase):
    def test_redacts_actor_ip_and_escapes_unsafe_input(self):
        html = view.make_html([{'rule_id':'R1_BRUTE_FORCE_LOGIN',
                                'detected_at':'<script>alert(1)</script>',
                                'actor_id':'employee@example.test',
                                'src_ip':'192.0.2.10',
                                'event_count':5,'severity':'HIGH'}], 0, 'synthetic', key=b'a-secret-key')
        self.assertNotIn('<script>', html)
        self.assertNotIn('employee@example.test', html)
        self.assertNotIn('192.0.2.10', html)
        self.assertIn('DEMO — DỮ LIỆU GIẢ LẬP', html)
        self.assertIn('R1_BRUTE_FORCE_LOGIN', html)

    def test_invalid_and_unknown_alerts_not_included(self):
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'alerts.jsonl'
            p.write_text(json.dumps({'rule_id':'R2_UNAUTHORIZED_TICKET_ACCESS','event_count':3})+'\n'
                         + 'bad_json\n'
                         + json.dumps({'rule_id':'MALICIOUS_XSS'})+'\n',encoding='utf-8')
            rows,bad=view.load_alerts(p)
            self.assertEqual(len(rows),1)
            self.assertEqual(bad,2)

    def test_bounded_display_latest_entries(self):
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'alerts.jsonl'
            p.write_text(''.join(json.dumps({'rule_id':'R1_BRUTE_FORCE_LOGIN','event_count':i})+'\n' for i in range(10)),encoding='utf-8')
            rows,bad=view.load_alerts(p,limit=3)
            self.assertEqual(len(rows),3)
            self.assertEqual([x['event_count'] for x in rows],[7,8,9])
            self.assertEqual(bad,0)

if __name__=='__main__':
    unittest.main()
