from pathlib import Path
import datetime, hashlib, json, os, secrets, socket, subprocess, sys
P=Path(__file__).resolve().parent
sys.path.insert(0,str(P/'vendor'))
import yaml
BASE='35c573d20d4244985e617e8a056f880faaadf1db'
assert str(P).startswith('/home/khiem/P01-T6-16-VA-')
for port in [28443,28090]:
    with socket.socket() as sock:sock.bind(('127.0.0.1',port))
for folder in ['queue','results','logs','private','zapraw','evidence','tls']:
    (P/folder).mkdir(exist_ok=True);os.chmod(P/folder,0o700)
compose=yaml.safe_load((P/'source/docker-compose.yml').read_text())
limits={'postgres':'256m','backend':'640m','frontend':'96m','nginx':'96m'}
for name,service in compose['services'].items():
    service['container_name']='p01-t616-before-'+name
    service['restart']='no';service['mem_limit']=limits[name]
    if 'build' in service:
        service['build']['context']=str(P/'source'/service['build']['context'].removeprefix('./'))
        service['build']['labels']={'org.opencontainers.image.revision':BASE,'p01.task':'T6-16-before'}
        service['image']='p01-t616-before-'+name+':35c573d'
    if name=='postgres':
        service['volumes']=[v if not v.startswith('./database/') else str(P/'source'/v.split(':',1)[0][2:])+':'+v.split(':',1)[1] for v in service['volumes']]
    if name=='nginx':
        service['ports']=['127.0.0.1:28443:443']
        service['volumes']=[str(P/'source/nginx/nginx.conf')+':/etc/nginx/nginx.conf:ro',str(P/'tls')+':/etc/nginx/ssl:ro','nginx_logs:/var/log/nginx']
    service['labels']={'p01.task':'T6-16-before','p01.source':BASE}
env={'POSTGRES_DB':'p01_t616_before','POSTGRES_USER':'p01_lab','POSTGRES_PASSWORD':secrets.token_hex(24),'JWT_SECRET':secrets.token_hex(32),'SPRING_PROFILES_ACTIVE':'prod','SPRING_JPA_HIBERNATE_DDL_AUTO':'validate','JWT_EXPIRATION_MS':'900000','HTTPS_PORT':'28443'}
for file,value in [('before.env',''.join(k+'='+v+'\n' for k,v in env.items())),('runtime.json',json.dumps(env)),('zap.key',secrets.token_hex(24))]:
    path=P/'private'/file;path.write_text(value);os.chmod(path,0o600)
(P/'compose.before.yml').write_text(yaml.safe_dump(compose,allow_unicode=True,sort_keys=False))
with (P/'logs/certificate-generation.log').open('w') as log:
    subprocess.run(['openssl','req','-x509','-newkey','rsa:2048','-nodes','-keyout',str(P/'tls/server.key'),'-out',str(P/'tls/server.crt'),'-days','7','-subj','/CN=localhost','-addext','subjectAltName=DNS:localhost,IP:127.0.0.1'],stdout=log,stderr=log,check=True)
os.chmod(P/'tls/server.key',0o600)
manifest={'task':'T6-16','baseline':BASE,'created_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'project':'p01-t616-before','target':'https://127.0.0.1:28443','zap_api':'http://127.0.0.1:28090','waf':'Absent; baseline nginx:1.27-alpine','runner':'Official ZAP Docker container on personal Ubuntu; not Kali VM','source_compose_sha256':hashlib.sha256((P/'source/docker-compose.yml').read_bytes()).hexdigest(),'openapi_sha256':hashlib.sha256((P/'source/openapi.yaml').read_bytes()).hexdigest(),'source_modified':False,'note':'Deployment-only overrides: isolated names/images/volumes, loopback port, resource limits, separate generated TLS/secrets.'}
(P/'evidence/setup-manifest.json').write_text(json.dumps(manifest,indent=2))
print(json.dumps(manifest,indent=2))
