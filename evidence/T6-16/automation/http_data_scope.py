from pathlib import Path
P=Path(__file__).resolve().parent
exec((P/'http_cases.py').read_text().split("print('T6-16 REAL HTTP CHECKS")[0])
prior=json.loads((P/'evidence/http-results.json').read_text());cases=prior['cases']
events=[json.loads(f.read_text()) for f in sorted(E.glob('*.json'))]
tokens=json.loads((P/'private/tokens.json').read_text())
categories=request('GET','/api/v1/categories',tokens['EMPLOYEE'],case_hint='Disposable data-scope fixture categories')
catalog=data(categories)
if isinstance(catalog,dict):catalog=catalog.get('content',catalog.get('items',[]))
if categories['status']!=200 or not catalog:raise RuntimeError('Missing categories')
category_id=catalog[0]['id']
roles['EMPLOYEE2']='employee2@helpdesk.local'
second,_=login('EMPLOYEE2')
seed_users=request('GET','/api/v1/admin/users',tokens['ADMIN'],case_hint='Read isolated seed fixture user IDs')
users=data(seed_users)
if isinstance(users,dict):users=users.get('content',users.get('items',[]))
staff_ids={u['email']:u['id'] for u in users if u['email'] in ['staff@helpdesk.local','staff2@helpdesk.local']}
fixture_ids=[];refs=[]
for n,tok in [(1,tokens['EMPLOYEE']),(2,second)]:
    create=request('POST','/api/v1/tickets',tok,body={'title':'T616 disposable scope fixture '+str(n),'description':'Personal isolated lab fixture. Reset seed after assessment.','categoryId':category_id,'impactLevel':'LOW'},case_hint='T616-API-01 disposable fixture create')
    if create['status']!=201:raise RuntimeError('Fixture create status '+str(create['status'])+' event '+create['event'])
    ticket_id=data(create)['id'];fixture_ids.append(ticket_id);refs.append(create['event'])
    assign=request('PUT',f'/api/v1/tickets/{ticket_id}/assign',tokens['IT_MANAGER'],body={'assigneeId':staff_ids['staff@helpdesk.local' if n==1 else 'staff2@helpdesk.local'],'priority':'LOW','assignmentNote':'T616 temporary controlled data fixture'},case_hint='T616-API-01 fixture assign')
    if assign['status']!=200:raise RuntimeError('Fixture assign status '+str(assign['status']))
    refs.append(assign['event'])
for role,tok in tokens.items():
    listing=request('GET','/api/v1/tickets?page=0&size=100',tok,case_hint='T616-API-01 nonempty data scope')
    listed=data(listing).get('content',[]);actual_ids={x['id'] for x in listed}
    expected_ids=set(fixture_ids if role in ['ADMIN','IT_MANAGER'] else fixture_ids[:1])
    detail_refs=[];statuses=[];leaked=[]
    for n,ticket_id in enumerate(fixture_ids):
        r=request('GET',f'/api/v1/tickets/{ticket_id}',tok,case_hint='T616-API-01 detail boundary')
        statuses.append(r['status']);detail_refs.append(r['event'])
        if r['status']==200:
            body=data(r)
            leaked += [k for k in body if re.search('password_hash|passwordHash|secret|failedLoginAttempts|lockedUntil',k)]
    expected_statuses=[200,200] if role in ['ADMIN','IT_MANAGER'] else [200,404]
    ok=listing['status']==200 and actual_ids==expected_ids and statuses==expected_statuses and not leaked
    row=next(c for c in cases if c['Case_ID']=='T616-API-01' and c['Role']==role)
    previous_ok=row['Status']=='PASS'
    row['Actual']+='; assigned fixtures visible='+str(sorted(actual_ids))+' expected='+str(sorted(expected_ids))+' detail='+str(statuses)+' forbidden_detail_fields='+str(leaked)
    row['Expected']+='; two-owner/two-assignee fixtures: Employee/Staff see own assigned fixture, other404; Manager/Admin both200'
    row['Status']='PASS' if ok and previous_ok else 'FAIL'
    row['Evidence']+=';'+ ';'.join('http/'+x for x in refs+[listing['event']]+detail_refs)
    print('DATA-SCOPE',role,row['Status'],sorted(actual_ids),statuses,flush=True)
save()
buffer=io.StringIO(newline='');w=csv.DictWriter(buffer,fieldnames=list(cases[0]),lineterminator='\n');w.writeheader();w.writerows(cases);(P/'evidence/http-results.csv').write_text(buffer.getvalue())
(P/'evidence/fixture-manifest.json').write_text(json.dumps({'utc':now(),'created_in':'p01-t616-before only','category_id':category_id,'tickets':fixture_ids,'owners':'EMPLOYEE and EMPLOYEE2 seed accounts','assignees':'IT_STAFF and STAFF2 seed accounts','cleanup':'Reset isolated project seed after all ZAP roles','purpose':'Nonempty cross-owner and cross-assignee data scoping; unauthorized details404; allowed DTO fields follow baseline contract'},indent=2))
print('NONEMPTY DATA SCOPE COMPLETE',flush=True)
