import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('audit_check', ROOT/'audit_schema_check.py')
checker=importlib.util.module_from_spec(spec)
spec.loader.exec_module(checker)

def row(event, **kw):
    e={'timestamp':'2026-11-17T05:00:00Z','event_type':event,
       'actor_id':None,'role':None,'target_type':'TICKET','target_id':'HD-2026-0009',
       'action':'MARK_SLA_BREACHED','outcome':'SUCCESS','src_ip':None,
       'correlation_id':'00000000-0000-4000-8000-000000000001','http_status':None}
    e.update(kw)
    return e

class SchemaCheckTests(unittest.TestCase):
    def check_rows(self,rows):
        with tempfile.TemporaryDirectory() as td:
            p=Path(td)/'audit.jsonl'
            p.write_text(''.join(json.dumps(x)+'\n' for x in rows),encoding='utf-8')
            return checker.check(p)

    def test_sla_correct_before_after(self):
        r=self.check_rows([row('SLA_BREACHED',before={'is_sla_breached':False},after={'is_sla_breached':True})])
        self.assertEqual(r['issues'],{})

    def test_missing_before_after_and_sla_transition_reported(self):
        r=self.check_rows([row('SLA_BREACHED')])
        self.assertEqual(r['issues']['before_after_missing_for_change'],1)
        self.assertEqual(r['issues']['sla_transition_invalid'],1)

    def test_accept_uuid_hex_32(self):
        r=self.check_rows([row('LOGIN_SUCCESS',outcome='SUCCESS',correlation_id='0123456789abcdef0123456789abcdef')])
        self.assertNotIn('invalid_correlation_uuid',r['issues'])

    def test_sla_old_outcome_is_not_accepted_as_after(self):
        r=self.check_rows([row('SLA_BREACHED',outcome='BREACHED',action='SLA_MONITOR')])
        self.assertEqual(r['issues']['sla_nonstandard_outcome'],1)
        self.assertEqual(r['issues']['sla_transition_invalid'],1)

    def test_rejected_upload_has_reason(self):
        r=self.check_rows([row('FILE_UPLOAD_REJECTED',outcome='FAILURE',action='UPLOAD_ATTACHMENT')])
        self.assertEqual(r['issues']['reject_reason_missing'],1)

    def test_sensitive_fields_detected_not_exposed_in_report(self):
        r=self.check_rows([row('COMMENT_ADDED', after={'comment_id':1,'comment_text':'private'})])
        self.assertEqual(r['issues']['sensitive_field_or_jwt_pattern'],1)
        report=checker.format_report(r,'after')
        self.assertNotIn('private',report)
        self.assertNotIn('HD-2026-0009',report)

    def test_legacy_role_event_detected(self):
        r=self.check_rows([row('CHANGE_ROLE',actor_id=2)])
        self.assertEqual(r['issues']['legacy_change_role_event'],1)

    def test_missing_required_field_and_non_utc_time(self):
        ev=row('TICKET_ASSIGNED')
        ev.pop('correlation_id')
        ev['timestamp']='2026-11-17T12:00:00+07:00'
        r=self.check_rows([ev])
        self.assertEqual(r['issues']['missing_required_field'],1)
        self.assertEqual(r['issues']['timestamp_not_utc'],1)
        self.assertEqual(r['issues']['invalid_correlation_uuid'],1)

if __name__ == '__main__': unittest.main()
