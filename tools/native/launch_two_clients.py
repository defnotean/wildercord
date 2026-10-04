"""Isolated two-client launcher proposal; no invocation has occurred.
Run only after root's serialized source/descriptor compilation and exact launch export.
"""
import argparse,hashlib,json,os,subprocess,time,uuid,shutil
from pathlib import Path

def java_quote(arg):
    return '"'+str(arg).replace('\\','\\\\').replace('"','\\"').replace('\n','\\n').replace('\r','\\r')+'"'

def replace_flag(args,name,value):
    result=[];i=0
    while i<len(args):
        if args[i]==name:
            if i+1>=len(args):raise ValueError('Incomplete launch option '+name)
            i+=2
        else:result.append(args[i]);i+=1
    return result+[name,str(value)]

def main():
    ap=argparse.ArgumentParser();ap.add_argument('launch',type=Path);ap.add_argument('--output',type=Path,required=True);ap.add_argument('--timeout',type=int,default=360);ap.add_argument('--accepted-eula',type=Path);ap.add_argument('--require-unity',action='store_true');ap.add_argument('--require-camp',action='store_true');ns=ap.parse_args()
    launch=json.loads(ns.launch.read_text(encoding='utf-8-sig'));base=ns.output.resolve();base.mkdir(parents=True,exist_ok=False)
    nonce=str(uuid.uuid4());ipc=base/'ipc';ipc.mkdir();jobs=[];logs=[];started=time.monotonic()
    if ns.accepted_eula:
        lines=ns.accepted_eula.read_text(encoding='utf-8-sig').splitlines()
        if not any(line.strip().lower()=='eula=true' for line in lines):raise ValueError('Source is not an existing accepted EULA file')
    (base/'launch-proof.json').write_text(json.dumps({'nonce':nonce,'launcher_pid':os.getpid(),'source_launch_sha256':hashlib.sha256(ns.launch.read_bytes()).hexdigest(),'status':'started'},indent=2))
    try:
        for role,name in [('host','WCAuraHost'),('peer','WCAuraPeer')]:
            game=base/role;game.mkdir()
            if role=='host' and ns.accepted_eula:shutil.copyfile(ns.accepted_eula,game/'eula.txt')
            args=list(launch['args']);args=replace_flag(args,'--gameDir',game);args=replace_flag(args,'--username',name);args=replace_flag(args,'--accessToken','0')
            offline=bytearray(hashlib.md5(('OfflinePlayer:'+name).encode()).digest());offline[6]=(offline[6]&15)|48;offline[8]=(offline[8]&63)|128;args=replace_flag(args,'--uuid',uuid.UUID(bytes=bytes(offline)))
            jvm=[a for a in launch['jvm'] if not a.startswith('-Dwildercord.mp.')]
            jvm+=['-Dwildercord.mp.role='+role,'-Dwildercord.mp.directory='+str(ipc),'-Dwildercord.mp.nonce='+nonce]
            # Never interpolate paths or game arguments into a shell command.
            argv=jvm+['-cp',os.pathsep.join(launch['classpath']),launch['main']]+args
            argfile=game/'java.args';argfile.write_text('\n'.join(java_quote(a) for a in argv)+'\n',encoding='utf-8')
            env=os.environ.copy();env.update({k:str(v) for k,v in launch.get('environment',{}).items() if v is not None})
            logfile=(base/(role+'.log')).open('wb');logs.append(logfile)
            proc=subprocess.Popen([launch['java'],'@'+str(argfile)],cwd=game,env=env,stdout=logfile,stderr=subprocess.STDOUT,shell=False);jobs.append((role,proc))
        while any(p.poll() is None for _,p in jobs):
            failed=[(r,p.returncode) for r,p in jobs if p.poll() not in (None,0)]
            if failed:raise RuntimeError('Actual client process failed: '+repr(failed))
            if time.monotonic()-started>ns.timeout:raise TimeoutError('Two actual clients exceeded finite supervisor deadline')
            time.sleep(.25)
        if any(p.returncode!=0 for _,p in jobs):raise RuntimeError('A client did not exit successfully')
        if not (ipc/'host-passed.properties').is_file() or not (ipc/'peer-saw-host.properties').is_file():raise RuntimeError('Missing native bidirectional TCP acceptance witnesses')
        if ns.require_camp and not (ipc/'host-camp-passed.properties').is_file():raise RuntimeError('Missing actual Camp Concord multiplayer acceptance witness')
        if ns.require_unity and not (ipc/'host-unity-passed.properties').is_file():raise RuntimeError('Missing actual Unity multiplayer acceptance witness')
        (base/'result.json').write_text(json.dumps({'nonce':nonce,'status':'passed','processes':{r:{'pid':p.pid,'exit':p.returncode} for r,p in jobs},'seconds':round(time.monotonic()-started,2)},indent=2))
    finally:
        for _,p in jobs:
            if p.poll() is None:p.terminate()
        for _,p in jobs:
            try:p.wait(timeout=10)
            except subprocess.TimeoutExpired:p.kill();p.wait(timeout=10)
        for f in logs:f.close()

if __name__=='__main__':main()
