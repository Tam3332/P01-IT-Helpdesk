from pathlib import Path
import argparse, json, os, re
P=Path(__file__).resolve().parent
a=argparse.ArgumentParser();a.add_argument('verb',choices=['submit','status']);a.add_argument('job_id');a.add_argument('action',nargs='?');a.add_argument('--role');a.add_argument('--tail',type=int,default=0);args=a.parse_args()
if not re.fullmatch('[a-z0-9-]{1,64}',args.job_id):raise SystemExit('Invalid job id')
if args.verb=='submit':
    f=P/'queue'/(args.job_id+'.job.json');temp=f.with_suffix('.tmp')
    if f.exists() or (P/'results'/(args.job_id+'.json')).exists():raise SystemExit('Job already exists')
    data={'action':args.action}
    if args.role:data['role']=args.role
    temp.write_text(json.dumps(data));os.chmod(temp,0o600);os.replace(temp,f)
    print('QUEUED',args.job_id,args.action)
else:
    f=P/'results'/(args.job_id+'.json')
    if not f.exists():print('RUNNING_OR_QUEUED',args.job_id)
    else:
        data=json.loads(f.read_text());print(json.dumps(data))
        if args.tail and data.get('log'):
            log=P/data['log'];print('\n'.join(log.read_text(errors='replace').splitlines()[-min(args.tail,100):]))
