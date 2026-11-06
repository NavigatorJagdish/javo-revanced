"""Restore the private development identity without putting it in source/release packages."""
from pathlib import Path
import hashlib,json,os,subprocess,zipfile,io

root=Path(__file__).resolve().parents[2]
key=Path.home()/'.android/debug.keystore';key.parent.mkdir(parents=True,exist_ok=True)
repo=os.environ['GITHUB_REPOSITORY']
if not key.exists():
    data=json.loads(subprocess.check_output(['gh','api',f'repos/{repo}/actions/artifacts?name=javo-manager-development-identity&per_page=100']))
    artifacts=[a for a in data['artifacts'] if not a['expired'] and a.get('workflow_run',{}).get('head_branch')=='main']
    if artifacts:
        selected=max(artifacts,key=lambda a:a['created_at'])
        blob=subprocess.check_output(['gh','api',f"repos/{repo}/actions/artifacts/{selected['id']}/zip"])
        with zipfile.ZipFile(io.BytesIO(blob)) as archive:
            candidates=[n for n in archive.namelist() if n=='debug.keystore']
            if len(candidates)!=1:raise RuntimeError('Invalid private signing backup')
            content=archive.read(candidates[0])
            if len(content)>1024*1024:raise RuntimeError('Invalid private signing backup size')
            key.write_bytes(content)
        key.chmod(0o600)
        print('Recovered the private development signing identity.')
    else:
        released=subprocess.run(['gh','release','view','v0.1.0-alpha.6','--repo',repo],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        if released.returncode==0:raise RuntimeError('Stable signing identity is missing. Restore the owner backup; refusing to rotate it silently.')
        print('First stable development identity will be created by Android Gradle Plugin.')
receipt=root/'evidence/release-alpha6.json'
if receipt.exists() and key.exists():
    expected=json.loads(receipt.read_text())['managerSignerSha256']
    certificate=subprocess.check_output(['keytool','-exportcert','-alias','androiddebugkey','-keystore',str(key),'-storepass','android'],stderr=subprocess.DEVNULL)
    if hashlib.sha256(certificate).hexdigest()!=expected:raise RuntimeError('Development certificate differs from the pinned alpha.6 identity')
