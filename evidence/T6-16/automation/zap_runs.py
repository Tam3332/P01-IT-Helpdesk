from pathlib import Path
import argparse, datetime, hashlib, json, os, re, ssl, sys, time, urllib.error, urllib.parse, urllib.request
P=Path(__file__).resolve().parent
sys.path.insert(0,str(P/'vendor'))
import yaml
BASE='https://127.0.0.1:28443'; API='http://127.0.0.1:28090'
COMMIT='35c573d20d4244985e617e8a056f880faaadf1db'
ROLE_EMAIL={'EMPLOYEE':'employee@helpdesk.local','IT_STAFF':'staff@helpdesk.local','IT_MANAGER':'manager@helpdesk.local','ADMIN':'admin@helpdesk.local'}
E=P/'evidence/zap';E.mkdir(exist_ok=True)
key=(P/'private/zap.key').read_text().strip()
runtime=json.loads((P/'private/runtime.json').read_text())
password=re.search(r'Mật khẩu chung:\s*(\S+)',(P/'source/database/init_demo_users.sql').read_text().splitlines()[0]).group(1)
secrets=[key,password,runtime['POSTGRES_PASSWORD'],runtime['JWT_SECRET']]
class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self,*args,**kwargs):return None
direct=urllib.request.build_opener(urllib.request.HTTPSHandler(context=ssl.create_default_context(cafile=str(P/'tls/server.crt'))),NoRedirect())
def now():return datetime.datetime.now(datetime.timezone.utc).isoformat()
def clean(text):
    text=re.sub(r'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*','[REDACTED JWT]',text)
    for secret in secrets:
        if secret:text=text.replace(secret,'[REDACTED]')
    text=re.sub(r'[\w.+-]+@helpdesk\.local','[SEED IDENTITY]',text)
    return text
def save(file,data):
    text=data if isinstance(data,str) else json.dumps(data,ensure_ascii=False,indent=2)+'\n'
    file.write_text(clean(text),encoding='utf-8')
def api(component,kind,action,raw=False,timeout=90,**params):
    url=API+'/'+('OTHER' if raw else 'JSON')+'/'+component+'/'+kind+'/'+action+'/?'+urllib.parse.urlencode(params)
    req=urllib.request.Request(url,headers={'X-ZAP-API-Key':key})
    try:
        with urllib.request.urlopen(req,timeout=timeout) as r:body=r.read()
    except urllib.error.HTTPError as exc:raise RuntimeError(component+'/'+action+' HTTP '+str(exc.code)+' '+clean(exc.read().decode(errors='replace')[:1000]))
    if raw:return body.decode(errors='replace')
    result=json.loads(body)
    if isinstance(result,dict) and result.get('code'):raise RuntimeError(component+'/'+action+' '+clean(json.dumps(result)))
    return result
def job(action,jobid,role=None):
    file=P/'queue'/(jobid+'.job.json');result=P/'results'/(jobid+'.json')
    if file.exists() or result.exists():raise RuntimeError('Duplicate job '+jobid)
    d={'action':action}
    if role:d['role']=role
    temp=file.with_suffix('.tmp');temp.write_text(json.dumps(d));os.chmod(temp,0o600);os.replace(temp,file)
    deadline=time.monotonic()+180
    while not result.exists():
        if time.monotonic()>deadline:raise TimeoutError('Worker job '+jobid)
        time.sleep(1)
    data=json.loads(result.read_text())
    if data['rc']!=0:raise RuntimeError('Worker failed '+jobid+' '+json.dumps(data))
    return data
def wait_zap():
    deadline=time.monotonic()+180
    while True:
        try:return api('core','view','version')
        except Exception:
            if time.monotonic()>deadline:raise
            time.sleep(3)
def wait_passive():
    deadline=time.monotonic()+180
    while True:
        remaining=int(api('pscan','view','recordsToScan')['recordsToScan'])
        if remaining==0:return
        if time.monotonic()>deadline:raise TimeoutError('Passive queue '+str(remaining))
        time.sleep(2)
def configure(name):
    existing=api('context','view','contextList')['contextList']
    if name in existing:
        return api('context','view','context',contextName=name)['context']['id']
    if name in existing:api('context','action','removeContext',contextName=name)
    cid=api('context','action','newContext',contextName=name)['contextId']
    api('context','action','includeInContext',contextName=name,regex=r'https://127\.0\.0\.1:28443(?:/.*)?')
    api('context','action','setContextInScope',contextName=name,booleanInScope='true')
    api('core','action','setMode',mode='protect')
    api('network','action','addRateLimitRule',description='P01-2rps',enabled='true',matchRegex='false',matchString='127.0.0.1',requestsPerSecond='2',groupBy='host')
    api('ascan','action','setOptionThreadPerHost',Integer='1')
    api('ascan','action','setOptionDelayInMs',Integer='600')
    api('ascan','action','setOptionMaxRuleDurationInMins',Integer='1')
    api('ascan','action','setOptionMaxScanDurationInMins',Integer='2')
    return cid
def send(path):
    request='GET '+BASE+path+' HTTP/1.1\r\nHost: 127.0.0.1:28443\r\nAccept: application/json\r\nConnection: close\r\n\r\n'
    result=api('core','action','sendRequest',request=request,followRedirects='false')
    messages=result.get('sendRequest',result)
    if isinstance(messages,dict):messages=[messages]
    if not isinstance(messages,list) or not messages:raise RuntimeError('No ZAP response '+str(type(messages)))
    return messages[0]
def verify_role(role,label):
    msg=send('/api/v1/auth/me')
    body=json.loads(msg['responseBody']);status=msg['responseHeader'].splitlines()[0]
    actual=(body.get('data') or {}).get('role')
    auth_present='Authorization: Bearer ' in msg.get('requestHeader','')
    result={'utc':now(),'role_expected':role,'role_actual':actual,'status':status,'auth_header_present_in_ZAP_request':auth_present,'message_id':msg.get('id')}
    save(E/(label+'-role-check.json'),result)
    if status.split()[1]!='200' or actual!=role or not auth_present:raise RuntimeError('ZAP role verification failed '+json.dumps(result))
    return result
def reports(label,meta):
    wait_passive()
    alerts=api('core','view','alerts',baseurl=BASE,start='0',count='10000')
    urls=api('core','view','urls',baseurl=BASE)['urls']
    allurls=api('core','view','urls')['urls']
    outside=[u for u in allurls if not u.startswith(BASE+'/') and u!=BASE]
    if outside:raise RuntimeError('Out-of-origin URLs observed '+str(outside[:5]))
    for extension in ['json','html']:
        raw=api('core','other',extension+'report',raw=True)
        path=P/'zapraw'/(label+'.'+extension);path.write_text(raw);os.chmod(path,0o600)
        save(E/(label+'.'+extension),raw)
    save(E/(label+'-alerts.json'),alerts)
    # Exported contexts carry scope only. Header credentials stay in ephemeral container env.
    contextfile=label+'.context'
    api('context','action','exportContext',contextName=label,contextFile='/zap/wrk/'+contextfile)
    save(E/contextfile,(P/'zapraw'/contextfile).read_text())
    messages=api('core','view','messages',baseurl=BASE,start='0',count='10000')['messages']
    coverage=[]
    for m in messages:
        line=m.get('requestHeader','').splitlines()[0]
        status=m.get('responseHeader','').splitlines()[0]
        coverage.append({'id':m['id'],'request_line':line,'response_status':status,'auth_header_present':'Authorization: Bearer ' in m.get('requestHeader','')})
    save(E/(label+'-coverage.json'),coverage)
    meta.update({'completed_utc':now(),'urls':urls,'message_count':len(messages),'outside_origin':outside,'alerts':len(alerts['alerts']),'passive_remaining':0,'report_files':[label+'.json',label+'.html',contextfile]})
    save(E/(label+'-manifest.json'),meta)
    print(label,'COMPLETE',len(messages),'messages',len(alerts['alerts']),'alert instances',flush=True)
def refresh(role):
    req=urllib.request.Request(BASE+'/api/v1/auth/login',data=json.dumps({'email':ROLE_EMAIL[role],'password':password}).encode(),headers={'Content-Type':'application/json'},method='POST')
    with direct.open(req,timeout=20) as r:body=json.load(r)
    token=body['data']['accessToken'];secrets.append(token)
    file=P/'private/tokens.json';tokens=json.loads(file.read_text());tokens[role]=token;file.write_text(json.dumps(tokens));os.chmod(file,0o600)
def baseline():
    label='baseline-anon';version=wait_zap();cid=configure(label)
    api('spider','action','setOptionThreadCount',Integer='1')
    api('spider','action','setOptionMaxDuration',Integer='1')
    api('spider','action','setOptionMaxDepth',Integer='3')
    api('spider','action','setOptionProcessForm',Boolean='false')
    api('spider','action','setOptionPostForm',Boolean='false')
    sid=api('spider','action','scan',url=BASE+'/',maxChildren='10',recurse='true',contextName=label,subtreeOnly='true')['scan']
    deadline=time.monotonic()+100
    while int(api('spider','view','status',scanId=sid)['status'])<100:
        if time.monotonic()>deadline:api('spider','action','stop',scanId=sid);raise TimeoutError('Spider')
        time.sleep(3)
    reports(label,{'started_utc':started,'role':'ANON','type':'traditional spider + passive Baseline; no AJAX browser','version':version,'build':COMMIT,'target':BASE,'waf':'Absent','spider_status':'100','scope_context':cid})
def api_role(role,index,reuse=False):
    label='api-'+role.lower().replace('_','-')
    if not reuse:
        job('zap-stop',f'z{index}-stop')
        refresh(role)
        job('zap-start',f'z{index}-start',role)
    version=wait_zap();cid=configure(label);check=verify_role(role,label)
    original=yaml.safe_load((P/'source/openapi.yaml').read_text())
    original['servers']=[{'url':BASE+'/api/v1','description':'Isolated personal Before lab; deployment override'}]
    allops=[];included=[];paths={}
    for path,item in original['paths'].items():
        for method in item:
            if method.lower() in ['get','post','put','patch','delete','options','head']:allops.append(method.upper()+' '+path)
        if 'get' in item:
            paths[path]={k:v for k,v in item.items() if k in ['get','parameters']};included.append('GET '+path)
    original['paths']=paths
    spec=P/'zapraw'/'openapi-readonly.yaml';spec.write_text(yaml.safe_dump(original,allow_unicode=True,sort_keys=False))
    save(E/'openapi-readonly.yaml',spec.read_text())
    imported=api('openapi','action','importFile',file='/zap/wrk/openapi-readonly.yaml',target=BASE+'/api/v1',contextId=cid,timeout=150)
    save(E/(label+'-import.json'),imported)
    verify_role(role,label+'-post-import')
    # Finite SQL-injection rule on the ticket-list query only. No automatic write calls.
    policy='P01-readonly-SQL-check'
    api('ascan','action','addScanPolicy',scanPolicyName=policy)
    api('ascan','action','disableAllScanners',scanPolicyName=policy)
    api('ascan','action','enableScanners',ids='40018',scanPolicyName=policy)
    api('ascan','action','setScannerAttackStrength',id='40018',attackStrength='LOW',scanPolicyName=policy)
    send('/api/v1/tickets?page=0&size=1')
    sid=api('ascan','action','scan',url=BASE+'/api/v1/tickets?page=0&size=1',recurse='false',inScopeOnly='true',scanPolicyName=policy,method='GET',contextId=cid)['scan']
    deadline=time.monotonic()+170
    while int(api('ascan','view','status',scanId=sid)['status'])<100:
        if time.monotonic()>deadline:api('ascan','action','stop',scanId=sid);raise TimeoutError('Active scan '+role)
        time.sleep(3)
    final=verify_role(role,label+'-post-scan')
    reports(label,{'started_utc':now_start,'role':role,'version':version,'build':COMMIT,'target':BASE,'waf':'Absent','context_id':cid,'auth':'container env header injection; verified in actual ZAP message before/import/after scan','role_check':check,'final_role_check':final,'openapi_source_sha256':hashlib.sha256((P/'source/openapi.yaml').read_bytes()).hexdigest(),'openapi_import_sha256':hashlib.sha256(spec.read_bytes()).hexdigest(),'included_operations':included,'excluded_operations':[x for x in allops if x not in included],'active_rule_ids':['40018'],'active_scope':'GET /api/v1/tickets?page=0&size=1 only; LOW; one thread; 600ms delay; 1min/rule; 2min/scan','active_status':'100; finite time/rule limits apply; not an exhaustive scan','rate_limit_rps':2})
a=argparse.ArgumentParser();a.add_argument('mode',choices=['baseline','roles']);a.add_argument('--reuse-first',action='store_true');a.add_argument('--start-role',choices=list(ROLE_EMAIL),default='EMPLOYEE');a.add_argument('--job-offset',type=int,default=20);args=a.parse_args()
started=now()
if args.mode=='baseline':baseline()
else:
    selected=list(ROLE_EMAIL)[list(ROLE_EMAIL).index(args.start_role):]
    for index,role in enumerate(selected,args.job_offset):
        now_start=now();print('START',role,now_start,flush=True);api_role(role,index,reuse=args.reuse_first and index==args.job_offset)
    job('zap-stop','z99-stop')
print('ZAP RUNS FINISHED',args.mode,now(),flush=True)
