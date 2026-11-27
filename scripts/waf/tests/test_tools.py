"""Tool-contract tests with simulated inputs; not VM/WAF runtime evidence."""
import importlib.util
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]

def module(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / (name + '.py'))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod

preflight = module('preflight')
smoke = module('smoke')
privacy = module('privacy_check')

class PreflightTests(unittest.TestCase):
    def test_private_snapshot_rejects_repository_and_existing_directory(self):
        with tempfile.TemporaryDirectory() as temp:
            repo = Path(temp) / 'repo'
            repo.mkdir()
            with self.assertRaises(ValueError):
                preflight.private_dir(repo / 'secret', repo)
            with self.assertRaises(ValueError):
                preflight.private_dir(Path(temp), repo)

    def test_snapshot_pins_actual_image_env_and_external_resources(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            config_file = root / 'nginx.conf'
            config_file.write_text('events {}', encoding='utf-8')
            tls = root / 'tls'
            tls.mkdir()
            key = tls / 'server.key'
            key.write_text('DISPOSABLE_TEST_KEY_NOT_A_CERT', encoding='utf-8')
            out = root / 'private'
            out.mkdir()
            config = {'services': {'nginx': {
                'image': 'planned:image', 'environment': {'MODE': 'planned'},
                'depends_on': ['backend'], 'ports': [{'target': 443, 'published': '8443'}],
                'volumes': [
                    {'type': 'bind', 'source': str(config_file), 'target': '/etc/nginx/nginx.conf', 'read_only': True},
                    {'type': 'bind', 'source': str(tls), 'target': '/etc/nginx/ssl', 'read_only': True},
                    {'type': 'volume', 'source': 'logs', 'target': '/var/log/nginx'},
                ]}}}
            live = {'Image': 'sha256:actual-old-image', 'Config': {'Env': ['MODE=old', 'DUMMY=disposable']},
                'Mounts': [
                    {'Destination': '/etc/nginx/nginx.conf', 'Source': str(config_file)},
                    {'Destination': '/etc/nginx/ssl', 'Source': str(tls)},
                    {'Destination': '/var/log/nginx', 'Source': '/docker/volumes/logs', 'Name': 'real-project_logs'},
                ], 'NetworkSettings': {'Networks': {'real-project_p01-back': {}, 'real-project_p01-front': {}}}}
            result = preflight.snapshot_config(config, live, out)
            gateway = result['services']['nginx']
            self.assertEqual(list(result['services']), ['nginx'])
            self.assertEqual(gateway['image'], live['Image'])
            self.assertEqual(gateway['environment']['MODE'], 'old')
            self.assertNotIn('depends_on', gateway)
            config_file.write_text('CHANGED_AFTER_SNAPSHOT', encoding='utf-8')
            self.assertEqual(Path(gateway['volumes'][0]['source']).read_text(), 'events {}')
            self.assertEqual(gateway['volumes'][1]['source'], str(tls))
            self.assertEqual(key.read_text(), 'DISPOSABLE_TEST_KEY_NOT_A_CERT')
            self.assertEqual(result['volumes']['rollback_2'], {'external': True, 'name': 'real-project_logs'})
            self.assertTrue(all(n['external'] for n in result['networks'].values()))

    def test_mismatched_live_mount_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp:
            with self.assertRaises(ValueError):
                preflight.snapshot_config({'services': {'nginx': {'volumes': [{'target': '/missing'}]}}},
                    {'Image': 'old', 'Config': {'Env': []}, 'Mounts': []}, Path(temp))

    def test_config_rejects_exposed_database_and_disabled_engine(self):
        config = {'services': {'nginx': {'image': preflight.IMAGE, 'environment': {'MODSEC_RULE_ENGINE': 'Off'}},
            'postgres': {'ports': [{'target': 5432}]}, 'frontend': {}, 'backend': {}}}
        errors = preflight.validate_config(config)
        self.assertTrue(any('postgres' in error for error in errors))
        self.assertTrue(any('WAF mode' in error for error in errors))
        self.assertTrue(any('ANOMALY_INBOUND' in error for error in errors))

class Response:
    def __init__(self, code, body, headers):
        self.code, self.body, self.headers = code, body, headers
    def read(self, limit):
        return self.body[:limit]
    def __enter__(self):
        return self
    def __exit__(self, *args):
        return False

class Opener:
    def __init__(self, response):
        self.response = response
    def open(self, request, timeout):
        self.request = request
        return self.response

class SmokeTests(unittest.TestCase):
    def test_gateway_block_requires_matching_id_json_and_status(self):
        body = json.dumps({'code': 'GATEWAY_FORBIDDEN', 'requestId': 'rid1'}).encode()
        opener = Opener(Response(403, body, {'X-Request-Id': 'rid1', 'Content-Type': 'application/json'}))
        result = smoke.probe(opener, 'https://lab.invalid', 'On', smoke.PROBES[2])
        self.assertEqual(result['status'], 'PASS')
        self.assertIn('t615_probe=', opener.request.full_url)
        self.assertEqual(opener.request.get_method(), 'GET')
        self.assertNotIn('Authorization', opener.request.headers)
        opener.response.headers['X-Request-Id'] = 'different'
        self.assertEqual(smoke.probe(opener, 'https://lab.invalid', 'On', smoke.PROBES[2])['status'], 'FAIL')

    def test_plain_rbac_403_does_not_pass_waf_block(self):
        opener = Opener(Response(403, b'{"code":"ACCESS_DENIED"}', {'X-Request-Id': 'rid1'}))
        self.assertEqual(smoke.probe(opener, 'https://lab.invalid', 'On', smoke.PROBES[2])['status'], 'FAIL')

    def test_detection_does_not_expect_a_block(self):
        opener = Opener(Response(200, b'frontend', {'X-Request-Id': 'rid1'}))
        self.assertEqual(smoke.probe(opener, 'https://lab.invalid', 'DetectionOnly', smoke.PROBES[2])['status'], 'PASS')
        self.assertEqual(smoke.probe(opener, 'https://lab.invalid', 'On', smoke.PROBES[2])['status'], 'FAIL')

    def test_redirects_are_not_followed(self):
        self.assertIsNone(smoke.NoRedirect().redirect_request(None, None, 302, '', {}, 'https://elsewhere.invalid'))

class PrivacyTests(unittest.TestCase):
    def test_markers_are_reported_without_values(self):
        raw = '{"Authorization":"Bearer disposable", "password":"DISPOSABLE_SENTINEL_123"}'
        found = privacy.scan(raw, ['DISPOSABLE_SENTINEL_123'])
        self.assertIn('authorization_header', found)
        self.assertIn('password_field', found)
        self.assertIn('test_canary', found)
        self.assertNotIn('DISPOSABLE_SENTINEL_123', json.dumps(found))

    def test_clean_metadata_can_pass_screening(self):
        self.assertEqual(privacy.scan('{"http_status":403,"request_id":"id1"}', ['DISPOSABLE_SENTINEL_123']), [])

    def test_empty_logs_do_not_pass_and_existing_output_is_preserved(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            log, canary, output = root/'raw', root/'canary', root/'out.json'
            log.write_text('', encoding='utf-8')
            canary.write_text('DISPOSABLE_SENTINEL_123', encoding='utf-8')
            cmd = [sys.executable, str(ROOT/'privacy_check.py'), '--input', str(log), '--canary-file', str(canary), '--output', str(output)]
            self.assertEqual(subprocess.run(cmd, capture_output=True).returncode, 1)
            self.assertEqual(json.loads(output.read_text())['files'][0]['status'], 'MISSING_OR_EMPTY')
            original = output.read_bytes()
            self.assertNotEqual(subprocess.run(cmd, capture_output=True).returncode, 0)
            self.assertEqual(output.read_bytes(), original)

if __name__ == '__main__':
    unittest.main(verbosity=2)
