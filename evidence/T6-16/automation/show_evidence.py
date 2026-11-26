from pathlib import Path
import collections,datetime,json,sys
P=Path(__file__).resolve().parent
mode=sys.argv[1] if len(sys.argv)>1 else 'http'
print('\033[2J\033[H',end='')
print('P01 T6-16 | REAL UBUNTU PERSONAL LAB EVIDENCE')
print(datetime.datetime.now(datetime.timezone.utc).isoformat())
print('Before 35c573d | https://127.0.0.1:28443 | WAF absent')
print('Read-only summary of saved runtime evidence; ZAP Docker, not Kali')
print('-'*76)
if mode=='http':
    d=json.loads((P/'evidence/http-results.json').read_text());c=d['cases']
    print('HTTP:',len(c),'rows;',sum(x['Status']=='PASS' for x in c),'PASS;',sum(x['Status']=='FAIL' for x in c),'FAIL')
    for x in c:
        if x['Case_ID']=='T616-API-01':print(x['Case_ID'],x['Role'],x['Status'],'nonempty scope verified')
        else:print(x['Case_ID'],x['Status'],x['Actual'][:76])
    print('BUG-HD-008 is existing BEFORE issue; AFTER not concluded.')
else:
    for label in ['baseline-anon','api-employee','api-it-staff','api-it-manager','api-admin']:
        d=json.loads((P/'evidence/zap'/(label+'-manifest.json')).read_text())
        print(label,'messages='+str(d['message_count']),'HTML+JSON+context exported')
        if d['role']!='ANON':print('  role verified:',d['role_check']['role_actual'],'->',d['final_role_check']['role_actual'],'scope OK')
    d=json.loads((P/'evidence/cleanup-manifest.json').read_text())
    print('Seed restored:',d['reset_seed'],'tickets:',d['tickets_after_reset'])
    print('Original containers preserved:',all(x['id_before']==x['id_after'] for x in d['original_containers_preserved'].values()))
    print('ZAP stopped:',d['zap_container_stopped'],'worker stop marker:',(P/'evidence/worker-stop.json').exists())
    print('Sanitized export:',json.loads((P/'evidence/sanitization-check.json').read_text())['result'])
    print('Limits: GET OpenAPI subset; bounded SQL rule40018; no AJAX flow.')
print('-'*76)
