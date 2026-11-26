from pathlib import Path
import base64, csv, datetime, hashlib, hmac, io, json, os, re, ssl, subprocess, sys, time, urllib.error, urllib.parse, urllib.request, uuid
P=Path(__file__).resolve().parent
BASE='https://127.0.0.1:28443'; COMMIT='35c573d20d4244985e617e8a056f880faaadf1db'
E=P/'evidence/http';E.mkdir(parents=True,exist_ok=True)
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):return None
opener=urllib.request.build_opener(urllib.request.HTTPSHandler(context=ssl.create_default_context(cafile=str(P/'tls/server.crt'))),NoRedirect())
roles={'EMPLOYEE':'employee@helpdesk.local','IT_STAFF':'staff@helpdesk.local','IT_MANAGER':'manager@helpdesk.local','ADMIN':'admin@helpdesk.local'}
# Baseline demo password is consumed locally; never included in exported evidence.
demo_line=(P/'source/database/init_demo_users.sql').read_text().splitlines()[0]
password=re.search(r'Mật khẩu chung:\s*(\S+)',demo_line).group(1)
runtime=json.loads((P/'private/runtime.json').read_text())
events=[];cases=[];tokens={};last_request=0.;last_login=0.
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def clean(value,key=''):
    if re.search('password|token|secret|authorization|cookie',key,re.I):return '[REDACTED]'
    if key in ['email','username','fullName']:return '[SEED IDENTITY]'
    if isinstance(value,dict):return {k:clean(v,k) for k,v in value.items()}
    if isinstance(value,list):return [clean(v) for v in value]
    if isinstance(value,str):
        value=re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*','[REDACTED JWT]',value)
        for secret in [password,runtime['POSTGRES_PASSWORD'],runtime['JWT_SECRET'],*tokens.values()]:
            if secret:value=value.replace(secret,'[REDACTED]')
    return value
def request(method,path,token=None,body=None,extra_headers=None,timeout=45,case_hint=''):
    global last_request,last_login
    url=urllib.parse.urljoin(BASE,path)
    parsed=urllib.parse.urlsplit(url)
    if (parsed.scheme,parsed.hostname,parsed.port)!=('https','127.0.0.1',28443):raise ValueError('Origin outside P01 scope')
    wait=max(0,0.6-(time.monotonic()-last_request))
    if path=='/api/v1/auth/login':wait=max(wait,6.2-(time.monotonic()-last_login))
    if wait>0:time.sleep(wait)
    started=now();last_request=time.monotonic()
    if path=='/api/v1/auth/login':last_login=last_request
    headers={'Accept':'application/json','User-Agent':'P01-T6-16-controlled-check'}
    if token:headers['Authorization']='Bearer '+token
    if extra_headers:headers.update(extra_headers)
    data=None
    if body is not None:data=json.dumps(body).encode();headers['Content-Type']='application/json'
    req=urllib.request.Request(url,data=data,headers=headers,method=method)
    try:
        with opener.open(req,timeout=timeout) as response:status=response.status;raw=response.read();response_headers=dict(response.headers)
    except urllib.error.HTTPError as response:
        status=response.code;raw=response.read();response_headers=dict(response.headers)
    try:payload=json.loads(raw)
    except (ValueError,UnicodeDecodeError):payload=None
    safe_headers={k:v for k,v in response_headers.items() if k.lower() in ['content-type','location','strict-transport-security','x-content-type-options','x-frame-options'] or k.lower().startswith('access-control-')}
    event={'event_id':len(events)+1,'utc':started,'completed_utc':now(),'case_hint':case_hint,'method':method,'path':path,'authenticated':bool(token),'http_status':status,'headers':safe_headers,'raw_response_sha256':hashlib.sha256(raw).hexdigest(),'body':clean(payload) if payload is not None else clean(raw.decode(errors='replace')[:800])}
    name=f'{event["event_id"]:03d}.json';(E/name).write_text(json.dumps(event,ensure_ascii=False,indent=2)+'\n');events.append(event)
    return {'status':status,'json':payload,'raw':raw,'headers':response_headers,'event':name}
def data(r):
    b=r['json']
    return b.get('data',b) if isinstance(b,dict) else b
def record(case_id,scope,expected,actual,ok,refs,bug='',role=''):
    row={'Case_ID':case_id,'Role':role,'Scope':scope,'Expected':expected,'Actual':actual,'Status':'PASS' if ok else 'FAIL','Evidence':';'.join('http/'+r for r in refs),'BUG_ID':bug,'Build':COMMIT,'Environment':'Ubuntu cá nhân; Before riêng; WAF absent'}
    cases.append(row);save();print(case_id,role,row['Status'],actual,flush=True)
def save():
    (P/'evidence/http-results.json').write_text(json.dumps({'task':'T6-16','build':COMMIT,'target':BASE,'waf':'Absent','cases':cases,'events':len(events),'updated_utc':now()},ensure_ascii=False,indent=2)+'\n')
def login(role,pw=None):
    r=request('POST','/api/v1/auth/login',body={'email':roles[role],'password':pw or password},case_hint='Role verification/login')
    d=data(r)
    if r['status']!=200 or not isinstance(d,dict) or not d.get('accessToken'):raise RuntimeError('Login failed for '+role+' status='+str(r['status'])+' event='+r['event'])
    return d['accessToken'],r
def persist_tokens():
    file=P/'private/tokens.json';file.write_text(json.dumps(tokens));os.chmod(file,0o600)
def b64(v):return base64.urlsafe_b64encode(v).decode().rstrip('=')
def unb64(v):return base64.urlsafe_b64decode(v+'='*(-len(v)%4))
def forbidden_keys(value):
    result=[]
    if isinstance(value,dict):
        for k,v in value.items():
            if re.search('password|secret|hash',k,re.I):result.append(k)
            result.extend(forbidden_keys(v))
    elif isinstance(value,list):
        for v in value:result.extend(forbidden_keys(v))
    return result
def queue(action,name):
    target=P/'queue'/(name+'.job.json');temp=target.with_suffix('.tmp');temp.write_text(json.dumps({'action':action}));os.chmod(temp,0o600);os.replace(temp,target)
    deadline=time.monotonic()+180
    result=P/'results'/(name+'.json')
    while time.monotonic()<deadline:
        if result.exists():
            d=json.loads(result.read_text())
            if d['rc']!=0:raise RuntimeError('Worker job failed '+name)
            return
        time.sleep(1)
    raise TimeoutError('Worker timeout '+name)

print('T6-16 REAL HTTP CHECKS - PERSONAL BEFORE '+COMMIT,flush=True)
for role in roles:
    token,r=login(role);tokens[role]=token
    check=request('GET','/api/v1/auth/me',token)
    d=data(check);actual_role=d.get('role') if isinstance(d,dict) else None
    if check['status']!=200 or actual_role!=role:raise RuntimeError('Role verification failed '+role+' actual='+str(actual_role))
persist_tokens()
r=request('GET','/api/v1/tickets',case_hint='T616-AUTH-01')
record('T616-AUTH-01','Ticket API no auth','401','HTTP '+str(r['status']),r['status']==401,[r['event']])
original=tokens['EMPLOYEE'];head,payload,signature=original.split('.')
for case_id,key,value in [('T616-AUTH-02','role','ADMIN'),('T616-AUTH-03','sub','999999')]:
    claims=json.loads(unb64(payload));claims[key]=value;changed=head+'.'+b64(json.dumps(claims,separators=(',',':')).encode())+'.'+signature
    r=request('GET','/api/v1/auth/me',changed,case_hint=case_id)
    record(case_id,'JWT integrity '+key,'401','HTTP '+str(r['status']),r['status']==401,[r['event']])
changed=b64(json.dumps({'alg':'none','typ':'JWT'},separators=(',',':')).encode())+'.'+payload+'.'
r=request('GET','/api/v1/auth/me',changed,case_hint='T616-AUTH-04');record('T616-AUTH-04','JWT algorithm policy','401','HTTP '+str(r['status']),r['status']==401,[r['event']])
bad_signature=('A' if signature[0]!='A' else 'B')+signature[1:]
r=request('GET','/api/v1/auth/me',head+'.'+payload+'.'+bad_signature,case_hint='T616-AUTH-05');record('T616-AUTH-05','JWT wrong signature','401','HTTP '+str(r['status']),r['status']==401,[r['event']])
def signed_claims(expired=False):
    claims=json.loads(unb64(payload));t=int(time.time());claims.update({'iat':t-600 if expired else t,'exp':t-60 if expired else t+300,'jti':str(uuid.uuid4())})
    part=head+'.'+b64(json.dumps(claims,separators=(',',':')).encode())
    return part+'.'+b64(hmac.new(runtime['JWT_SECRET'].encode(),part.encode(),hashlib.sha256).digest())
control=request('GET','/api/v1/auth/me',signed_claims(False),case_hint='Expiry fixture control')
r=request('GET','/api/v1/auth/me',signed_claims(True),case_hint='T616-AUTH-06')
record('T616-AUTH-06','Expired HS256 fixture signed with isolated lab secret','Valid fixture200; expired fixture401','control='+str(control['status'])+' expired='+str(r['status']),control['status']==200 and r['status']==401,[control['event'],r['event']])
logout=request('POST','/api/v1/auth/logout',original,case_hint='T616-AUTH-07')
again=request('GET','/api/v1/auth/me',original,case_hint='T616-AUTH-07')
record('T616-AUTH-07','JWT after logout','logout200; old token401','logout='+str(logout['status'])+' old='+str(again['status']),logout['status']==200 and again['status']==401,[logout['event'],again['event']])
tokens['EMPLOYEE'],_=login('EMPLOYEE');persist_tokens()
a=request('POST','/api/v1/auth/login',body={'email':'not-a-user-t616@example.invalid','password':'invalid-fixture'},case_hint='T616-AUTH-08')
b=request('POST','/api/v1/auth/login',body={'email':roles['EMPLOYEE'],'password':'invalid-fixture'},case_hint='T616-AUTH-08')
def error_key(r):
    body=r['json'] or {};e=body.get('error') or body
    return r['status'],e.get('code'),e.get('message')
record('T616-AUTH-08','Neutral login error','Both401; same code/message',str(error_key(a))+' vs '+str(error_key(b)),a['status']==b['status']==401 and error_key(a)==error_key(b),[a['event'],b['event']])
for role,token in tokens.items():
    me=request('GET','/api/v1/auth/me',token,case_hint='T616-API-01')
    tickets=request('GET','/api/v1/tickets?page=0&size=20',token,case_hint='T616-API-01')
    users=request('GET','/api/v1/admin/users',token,case_hint='T616-API-01')
    forbidden=forbidden_keys(me['json'])+forbidden_keys(tickets['json'])+forbidden_keys(users['json'])
    expected_users=200 if role=='ADMIN' else 403
    good=me['status']==tickets['status']==200 and users['status']==expected_users and not forbidden
    record('T616-API-01','Response fields and administrative data boundary','me/tickets200; admin-users200 ADMIN else403; no password/hash fields','me='+str(me['status'])+' tickets='+str(tickets['status'])+' admin-users='+str(users['status'])+' forbidden_keys='+str(sorted(set(forbidden))),good,[me['event'],tickets['event'],users['event']],role=role)
r=request('GET','/api/v1/tickets?page=0&size=1000000',tokens['ADMIN'],case_hint='T616-API-02');d=data(r) or {};size=d.get('size',d.get('pageSize'));content=d.get('content',[])
record('T616-API-02','Large page-size cap','HTTP200, effective page size100 and <=100 items','HTTP='+str(r['status'])+' size='+str(size)+' count='+str(len(content)),r['status']==200 and size==100 and len(content)<=100,[r['event']],role='ADMIN')
r=request('PUT','/api/v1/auth/me',tokens['EMPLOYEE'],body={},case_hint='T616-API-03')
record('T616-API-03','Authenticated method outside contract','405 or method rejected by documented control','HTTP '+str(r['status']),r['status'] in [403,405],[r['event']],role='EMPLOYEE')
r=request('GET','/api/v1/auth/me',tokens['EMPLOYEE'],extra_headers={'Origin':'https://untrusted.example.invalid'},case_hint='T616-API-04')
pre=request('OPTIONS','/api/v1/auth/me',extra_headers={'Origin':'https://untrusted.example.invalid','Access-Control-Request-Method':'GET','Access-Control-Request-Headers':'Authorization'},case_hint='T616-API-04')
allow=[(q['headers'].get('Access-Control-Allow-Origin') or q['headers'].get('access-control-allow-origin')) for q in [r,pre]]
record('T616-API-04','CORS origin policy','No allow-origin for untrusted site','GET='+str(r['status'])+' preflight='+str(pre['status'])+' ACAO='+str(allow),all(not x for x in allow),[r['event'],pre['event']],role='EMPLOYEE')
for case_id,path in [('T616-EDGE-01','/swagger-ui/index.html'),('T616-EDGE-02','/v3/api-docs'),('T616-EDGE-03','/actuator/health')]:
    r=request('GET',path,case_hint=case_id);ct=r['headers'].get('Content-Type','');body=r['raw'].decode(errors='replace')
    is_spa='text/html' in ct and ('<div id="root"' in body or '/assets/' in body)
    protected=r['status'] in [401,403,404] or (case_id=='T616-EDGE-03' and is_spa)
    record(case_id,'Gateway '+path,'No exposed backend docs/operational data','HTTP='+str(r['status'])+' Content-Type='+ct+' SPA_fallback='+str(is_spa),protected,[r['event']])
# Reproduce existing BUG-HD-008 only in the isolated, disposable seed database.
current=tokens['EMPLOYEE'];new_password='T616-'+uuid.uuid4().hex[:20]+'!'
change=request('POST','/api/v1/auth/change-password',current,body={'currentPassword':password,'newPassword':new_password},case_hint='T616-AUTH-09 BUG-HD-008')
old=request('GET','/api/v1/auth/me',current,case_hint='T616-AUTH-09 BUG-HD-008')
record('T616-AUTH-09','Existing BUG-HD-008 old session after password change','change200; old token401','change='+str(change['status'])+' old_token='+str(old['status']),change['status']==200 and old['status']==401,[change['event'],old['event']],bug='BUG-HD-008',role='EMPLOYEE')
if change['status']==200:
    new_token,_=login('EMPLOYEE',new_password)
    restore=request('POST','/api/v1/auth/change-password',new_token,body={'currentPassword':new_password,'newPassword':password},case_hint='Restore isolated demo password')
    if restore['status']!=200:raise RuntimeError('Isolated demo password restore failed')
    tokens['EMPLOYEE'],_=login('EMPLOYEE');persist_tokens()
# Dependency interruption is limited to the newly-created, labelled Before DB.
queue('stop-db','http500-stop-db')
try:
    r=request('GET','/api/v1/tickets?page=0&size=1',tokens['ADMIN'],timeout=45,case_hint='T616-API-05')
    body=r['raw'].decode(errors='replace')
    leaks=bool(re.search(r'org\.postgresql|java\.lang\.|SQLException|at com\.ojt|SELECT\s+.+FROM|jdbc:postgresql',body,re.I))
    record('T616-API-05','Controlled server error; isolated DB paused','5xx without SQL/stack/secret','HTTP='+str(r['status'])+' sensitive_error_text='+str(leaks),500<=r['status']<600 and not leaks,[r['event']],role='ADMIN')
finally:queue('start-db','http500-start-db')
deadline=time.monotonic()+90
while True:
    try:
        ready=request('GET','/api/v1/auth/me',tokens['ADMIN'],timeout=10,case_hint='DB recovery')
        if ready['status']==200:break
    except Exception:pass
    if time.monotonic()>deadline:raise RuntimeError('DB recovery timeout')
    time.sleep(2)
persist_tokens();save()
columns=list(cases[0]);buffer=io.StringIO(newline='');writer=csv.DictWriter(buffer,fieldnames=columns,lineterminator='\n');writer.writeheader();writer.writerows(cases)
(P/'evidence/http-results.csv').write_text(buffer.getvalue())
print('HTTP CASES COMPLETE',json.dumps({'rows':len(cases),'unique_cases':len(set(c['Case_ID'] for c in cases)),'pass':sum(c['Status']=='PASS' for c in cases),'fail':sum(c['Status']=='FAIL' for c in cases),'events':len(events),'build':COMMIT}),flush=True)
