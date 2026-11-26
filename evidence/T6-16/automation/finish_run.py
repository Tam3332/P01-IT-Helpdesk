from pathlib import Path
import datetime,hashlib,json,os,re,ssl,time,urllib.request
P=Path(__file__).resolve().parent
exec((P/'zap_runs.py').read_text().split('a=argparse.ArgumentParser();')[0])
for role in ROLE_EMAIL:
    label='api-'+role.lower().replace('_','-');f=P/'evidence/zap'/(label+'-manifest.json')
    if not f.is_file():raise SystemExit('Scan still incomplete: '+role)
    m=json.loads(f.read_text());assert m['role_check']['role_actual']==role and m['final_role_check']['role_actual']==role and not m['outside_origin']
deadline=time.monotonic()+45
while not (P/'results/z99-stop.json').is_file():
    if time.monotonic()>deadline:raise TimeoutError('ZAP stop job')
    time.sleep(1)
assert json.loads((P/'results/z99-stop.json').read_text())['rc']==0
job('restore-seed','post-scan-restore-seed')
time.sleep(2)
refresh('ADMIN')
token=json.loads((P/'private/tokens.json').read_text())['ADMIN']
req=urllib.request.Request(BASE+'/api/v1/tickets?page=0&size=100',headers={'Authorization':'Bearer '+token})
with direct.open(req,timeout=20) as r:body=json.load(r);status=r.status
page=body['data'];assert status==200 and page['totalElements']==0
job('inventory','post-scan-inventory')
def inventory(name):
    return {d['Names']:d for line in (P/'logs'/name).read_text().splitlines() if line.startswith('{') for d in [json.loads(line)]}
before=inventory('01-inventory.log');after=inventory('post-scan-inventory.log')
assert all(name in after and before[name]['ID']==after[name]['ID'] and after[name]['State']=='running' for name in before)
assert 'p01-t616-zap' not in after
record={'utc':now(),'project':'p01-t616-before','reset_seed':True,'tickets_after_reset':0,'http_after_reset':status,'admin_login_after_reset':True,'existing_project':'p01-it-helpdesk','original_containers_preserved':{name:{'id_before':d['ID'],'id_after':after[name]['ID'],'state_after':after[name]['State']} for name,d in before.items()},'zap_container_stopped':True,'scope':'Only disposable Before volumes reset; original project containers/volumes untouched','private_runtime_retained':'VM run folder private/ and zapraw/ mode0700; not exported','no_persistent_sudoers_or_service_added':True}
save(P/'evidence/cleanup-manifest.json',record)
logdir=P/'evidence/logs';logdir.mkdir(exist_ok=True)
# Exclude backend application logs: source and full stack logs are not needed in the handoff.
for file in (P/'logs').glob('*.log'):
    if file.name in ['09-app-logs.log']:continue
    save(logdir/file.name,file.read_text(errors='replace'))
save(P/'evidence/zap-image.json',{'inspect_log':'logs/06-zap-info.log','image':'ghcr.io/zaproxy/zaproxy@sha256:9624669db72f54d86c4b46a7064518bd9bf79d8c61a4c31befa9923489f328af','version':'2.17.0'})
checks=[]
for f in (P/'evidence').rglob('*'):
    if not f.is_file():continue
    text=f.read_text(errors='replace')
    found=any(s and s in text for s in secrets) or re.search(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*',text) or re.search(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY',text)
    if found:raise RuntimeError('Sensitive material in export file '+str(f.relative_to(P)))
    checks.append(str(f.relative_to(P/'evidence')))
save(P/'evidence/sanitization-check.json',{'utc':now(),'checked_files':len(checks),'exact_runtime_secrets_checked':True,'jwt_patterns_checked':True,'private_key_markers_checked':True,'secret_values_not_exported':True,'result':'PASS'})
job('finish','post-scan-finish-worker')
deadline=time.monotonic()+15
while not (P/'evidence/worker-stop.json').exists():
    if time.monotonic()>deadline:raise TimeoutError('Worker stop marker')
    time.sleep(1)
print('FINISHED: seed restored; original containers preserved; ZAP stopped; worker stopped; sanitized export PASS',flush=True)
