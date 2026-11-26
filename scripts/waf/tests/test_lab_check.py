"""Run the real POSIX lab-check script against controlled command stubs."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]

@unittest.skipUnless(shutil.which('sh'), 'Requires POSIX sh; run in Ubuntu')
class LabCheckTests(unittest.TestCase):
    def run_script(self, port='8443', login_fail=False):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            docker = root/'docker'
            docker.write_text('''#!/bin/sh
case "$*" in
  "compose ps") echo "p01-db Up; p01-backend Up; p01-frontend Up; p01-gateway Up" ;;
  "compose port postgres 5432"|"compose port backend 8080") echo ":0" ;;
  "compose port nginx "*)
    printf '%s' "$4" > "$PORT_CAPTURE"
    if [ "$4" = "$EXPECTED_PORT" ]; then echo '0.0.0.0:8443'; else exit 1; fi ;;
  "compose exec "*) echo '/app/logs /app/uploads' ;;
  *) exit 2 ;;
esac
''', encoding='utf-8')
            curl = root/'curl'
            curl.write_text('''#!/bin/sh
case "$*" in
  *"-ksI "*) printf 'HTTP/1.1 200 OK\\nStrict-Transport-Security: max-age=31536000\\n' ;;
  *swagger-ui*) printf '404' ;;
  *auth/login*)
    if [ "$LOGIN_FAIL" = 1 ]; then printf '{"password":"PRIVATE_LOGIN_SENTINEL_12345"}';
    else printf '{"accessToken":"DISPOSABLE_NOT_A_REAL_TOKEN"}'; fi ;;
  *auth/me*) printf '{"role":"EMPLOYEE"}' ;;
  *tickets*) printf '401' ;;
  *) exit 3 ;;
esac
''', encoding='utf-8')
            docker.chmod(0o755)
            curl.chmod(0o755)
            capture = root/'port.txt'
            env = os.environ | {'PATH': str(root)+os.pathsep+os.environ['PATH'], 'PORT_CAPTURE': str(capture),
                'EXPECTED_PORT': port, 'LOGIN_FAIL': '1' if login_fail else '0'}
            env.pop('GATEWAY_CONTAINER_PORT', None)
            if port != '8443':
                env['GATEWAY_CONTAINER_PORT'] = port
            result = subprocess.run(['sh', str(ROOT/'lab-check.sh'), '8443'], env=env,
                text=True, capture_output=True, timeout=30)
            return result, capture.read_text()

    def test_waf_container_port_is_8443(self):
        result, port = self.run_script()
        self.assertEqual(result.returncode, 0, result.stdout+result.stderr)
        self.assertEqual(port, '8443')

    def test_legacy_container_port_can_be_selected(self):
        result, port = self.run_script(port='443')
        self.assertEqual(result.returncode, 0, result.stdout+result.stderr)
        self.assertEqual(port, '443')

    def test_login_error_does_not_print_response_body(self):
        result, _ = self.run_script(login_fail=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('PRIVATE_LOGIN_SENTINEL_12345', result.stdout+result.stderr)
        self.assertNotIn('DISPOSABLE_NOT_A_REAL_TOKEN', result.stdout+result.stderr)

if __name__ == '__main__':
    unittest.main(verbosity=2)
