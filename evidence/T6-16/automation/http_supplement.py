from pathlib import Path
P=Path(__file__).resolve().parent
# Load request/redaction helpers only; the first run is preserved, never rerun implicitly.
exec((P/'http_cases.py').read_text().split("print('T6-16 REAL HTTP CHECKS")[0])
prior=json.loads((P/'evidence/http-results.json').read_text())
(P/'evidence/http-results-attempt1.json').write_text(json.dumps(prior,ensure_ascii=False,indent=2))
cases=prior['cases'];events=[json.loads(f.read_text()) for f in sorted(E.glob('*.json'))]
tokens=json.loads((P/'private/tokens.json').read_text())
def forbidden_keys(value):
    result=[]
    if isinstance(value,dict):
        for k,v in value.items():
            if k.lower()!='mustchangepassword' and re.search('password|secret|hash',k,re.I):result.append(k)
            result.extend(forbidden_keys(v))
    elif isinstance(value,list):
        for v in value:result.extend(forbidden_keys(v))
    return result
for role in roles:
    tokens[role],_=login(role)
persist_tokens()
cases=[c for c in cases if c['Case_ID'] not in ['T616-API-01','T616-API-05']]
for role,token in tokens.items():
    me=request('GET','/api/v1/auth/me',token,case_hint='T616-API-01 corrected schema detector')
    tickets=request('GET','/api/v1/tickets?page=0&size=20',token,case_hint='T616-API-01')
    users=request('GET','/api/v1/admin/users',token,case_hint='T616-API-01')
    forbidden=forbidden_keys(me['json'])+forbidden_keys(tickets['json'])+forbidden_keys(users['json'])
    expected_users=200 if role=='ADMIN' else 403
    good=me['status']==tickets['status']==200 and users['status']==expected_users and not forbidden
    record('T616-API-01','Response DTO fields/admin boundary; mustChangePassword is documented boolean, not a secret','me/tickets200; admin-users200 ADMIN else403; no password/hash fields','me='+str(me['status'])+' tickets='+str(tickets['status'])+' admin-users='+str(users['status'])+' forbidden_keys='+str(forbidden),good,[me['event'],tickets['event'],users['event']],role=role)
queue('stop-db','http500b-stop-db')
try:
    r=request('POST','/api/v1/auth/login',body={'email':roles['EMPLOYEE'],'password':password},timeout=45,case_hint='T616-API-05 unauthenticated service path while isolated DB stopped')
    body=r['raw'].decode(errors='replace')
    leaks=bool(re.search(r'org\.postgresql|java\.lang\.|SQLException|at com\.ojt|SELECT\s+.+FROM|jdbc:postgresql',body,re.I))
    record('T616-API-05','Controlled DB error through public login; authenticated path previously returned401 in blacklist filter','5xx without SQL/stack/secret','HTTP='+str(r['status'])+' sensitive_error_text='+str(leaks),500<=r['status']<600 and not leaks,[r['event']])
finally:queue('start-db','http500b-start-db')
deadline=time.monotonic()+100
while True:
    try:
        r=request('GET','/api/v1/auth/me',tokens['ADMIN'],timeout=10,case_hint='Supplement DB recovery')
        if r['status']==200:break
    except Exception:pass
    if time.monotonic()>deadline:raise TimeoutError('DB recovery')
    time.sleep(2)
save()
buffer=io.StringIO(newline='');w=csv.DictWriter(buffer,fieldnames=list(cases[0]),lineterminator='\n');w.writeheader();w.writerows(cases);(P/'evidence/http-results.csv').write_text(buffer.getvalue())
(P/'evidence/http-corrections.json').write_text(json.dumps({'utc':now(),'first_attempt_preserved':'http-results-attempt1.json','API-01':'Initial detector incorrectly matched contract boolean mustChangePassword. Corrected against UserResponse.java and reran all four roles. Empty ticket list: role data scoping is not proven by this case.','API-05':'Initial authenticated DB-down request returned401 because blacklist lookup occurs in auth filter. Public login reaches service/global500 handler. Both raw events preserved.'},ensure_ascii=False,indent=2))
print('HTTP SUPPLEMENT COMPLETE',len(cases),'rows',sum(c['Status']=='PASS' for c in cases),'PASS',sum(c['Status']=='FAIL' for c in cases),'FAIL',flush=True)
