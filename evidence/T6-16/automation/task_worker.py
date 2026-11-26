from pathlib import Path
import datetime, json, os, re, signal, subprocess, sys, time
P=Path(__file__).resolve().parent
assert str(P).startswith('/home/khiem/P01-T6-16-VA-') and P.parent==Path('/home/khiem')
if os.geteuid()!=0:raise SystemExit('Chay bang sudo sau khi chu may tu xac thuc.')
PROJECT='p01-t616-before'; ZAP='ghcr.io/zaproxy/zaproxy:stable'; ZNAME='p01-t616-zap'
COMPOSE=['docker','compose','--progress','plain','--project-name',PROJECT,'--env-file',str(P/'private/before.env'),'-f',str(P/'compose.before.yml')]
ROLES={'ANON','EMPLOYEE','IT_STAFF','IT_MANAGER','ADMIN'}
stopping=False
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def stop(*args):
    global stopping;stopping=True
signal.signal(signal.SIGINT,stop);signal.signal(signal.SIGTERM,stop)
def run(argv,jobid,timeout=300,env=None):
    log=P/'logs'/f'{jobid}.log'
    with log.open('wb') as stream:
        stream.write(('START '+now()+'\nCOMMAND '+json.dumps(argv)+'\n').encode());stream.flush()
        try:r=subprocess.run(argv,stdout=stream,stderr=subprocess.STDOUT,timeout=timeout,env=env);rc=r.returncode
        except subprocess.TimeoutExpired:rc=124;stream.write(b'\nTIMEOUT\n')
        stream.write(('\nEND '+now()+' RC='+str(rc)+'\n').encode())
    os.chmod(log,0o644)
    return {'rc':rc,'log':str(log.relative_to(P))}
def action(job,jobid):
    name=job['action']
    if name=='inventory':
        return run(['docker','ps','--format','{{json .}}'],jobid)
    if name=='images':return run(['docker','images','--format','{{json .}}'],jobid)
    if name=='build':return run(COMPOSE+['build','backend','frontend'],jobid,1500)
    if name=='up':
        # Names/project are fixed; this cannot select the existing application project.
        return run(COMPOSE+['up','-d','--no-build','--wait','--wait-timeout','180'],jobid,240)
    if name=='ps':return run(COMPOSE+['ps','--format','json'],jobid)
    if name=='app-logs':return run(COMPOSE+['logs','--no-color','--tail','100','backend'],jobid)
    if name=='pull-zap':return run(['docker','pull',ZAP],jobid,1200)
    if name=='zap-info':return run(['docker','image','inspect',ZAP,'--format','{{json .RepoDigests}} {{.Id}} {{.Config.User}}'],jobid)
    if name=='app-images':
        return run(['docker','image','inspect','p01-t616-before-backend:35c573d','p01-t616-before-frontend:35c573d','--format','{{.Id}} {{json .Config.Labels}}'],jobid)
    if name in ['stop-db','start-db']:
        return run(['docker','stop' if name=='stop-db' else 'start','p01-t616-before-postgres'],jobid,60)
    if name=='zap-stop':return run(['docker','rm','-f',ZNAME],jobid,30)
    if name=='zap-start':
        role=job.get('role','ANON')
        if role not in ROLES:raise ValueError('Role ngoai scope')
        env=dict(os.environ);env['ZAP_API_KEY']=(P/'private/zap.key').read_text().strip()
        argv=['docker','run','-d','--rm','--name',ZNAME,'--network','host','--memory','1g','--cpus','1','--pids-limit','256','--mount','type=bind,src='+str(P/'zapraw')+',dst=/zap/wrk','--env','ZAP_API_KEY','--env','JAVA_OPTS=-Xmx640m']
        if role!='ANON':
            tokens=json.loads((P/'private/tokens.json').read_text())
            env['ZAP_AUTH_HEADER_VALUE']='Bearer '+tokens[role]
            argv+=['--env','ZAP_AUTH_HEADER_VALUE','--env','ZAP_AUTH_HEADER=Authorization','--env','ZAP_AUTH_HEADER_SITE=127.0.0.1']
        argv+=['--entrypoint','/bin/sh',ZAP,'-c','exec /zap/zap.sh -daemon -host 127.0.0.1 -port 28090 -config "api.key=$ZAP_API_KEY" -config autoupdate.checkOnStart=false']
        return run(argv,jobid,60,env=env)
    if name=='zap-logs':return run(['docker','logs','--tail','80',ZNAME],jobid)
    if name=='restore-seed':
        marker=P/'private/project-owned.json'
        if not marker.exists():raise ValueError('Thieu dau vet project rieng; khong xoa volume')
        expected=json.loads(marker.read_text())
        if expected.get('project')!=PROJECT or expected.get('root')!=str(P):raise ValueError('Sai project')
        first=run(COMPOSE+['down','--volumes','--timeout','30'],jobid+'-down',120)
        if first['rc']:return first
        return run(COMPOSE+['up','-d','--no-build','--wait','--wait-timeout','180'],jobid+'-up',240)
    if name=='finish':return {'rc':0,'finish':True}
    raise ValueError('Job ngoai danh sach co dinh')
print('P01 T6-16 TASK WORKER - TEMPORARY, PERSONAL LAB ONLY',flush=True)
print('Root:',P,'Target: https://127.0.0.1:28443',flush=True)
print('Fixed jobs only; no general shell API; expires after 90 minutes.',flush=True)
(P/'evidence/worker-start.json').write_text(json.dumps({'utc':now(),'pid':os.getpid(),'uid':os.getuid(),'root':str(P),'expires_after_minutes':90,'project':PROJECT,'target':'https://127.0.0.1:28443'}))
os.chmod(P/'evidence/worker-start.json',0o644)
deadline=time.monotonic()+5400
while not stopping and time.monotonic()<deadline:
    queued=sorted((P/'queue').glob('*.job.json'))
    if not queued:time.sleep(1);continue
    for file in queued:
        jobid=file.name.removesuffix('.job.json')
        if not re.fullmatch(r'[a-z0-9-]{1,64}',jobid):continue
        resultfile=P/'results'/f'{jobid}.json'
        if resultfile.exists():continue
        try:
            job=json.loads(file.read_text());print(now(),jobid,job.get('action'),job.get('role',''), 'START',flush=True)
            result={'job':jobid,'action':job['action'],'started_utc':now(),**action(job,jobid),'completed_utc':now()}
        except Exception as error:
            result={'job':jobid,'rc':125,'error':str(error),'completed_utc':now()}
        resultfile.write_text(json.dumps(result,indent=2));os.chmod(resultfile,0o644)
        print(now(),jobid,'RC',result['rc'],flush=True)
        if result.get('finish'):stopping=True;break
(P/'evidence/worker-stop.json').write_text(json.dumps({'utc':now(),'reason':'finish or timeout or interrupt','no_persistent_service_installed':True}))
os.chmod(P/'evidence/worker-stop.json',0o644)
print('WORKER STOPPED - no persistent service/sudoers/group changes',flush=True)
