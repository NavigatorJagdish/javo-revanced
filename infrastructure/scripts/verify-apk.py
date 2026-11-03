"""Verify a built APK with official Android tools, then add it to the local artifact inventory."""
from pathlib import Path
import argparse, hashlib, json, re, shutil, subprocess, os, zipfile

ROOT=Path(__file__).resolve().parents[2]
def runtime_for_artifact(runtime, digest, application_id):
    """A previous APK's smoke result must never certify a freshly built APK."""
    if not isinstance(runtime,dict):return None
    if runtime.get('package')!=application_id or runtime.get('installedApkSha256')!=digest:return None
    if runtime.get('installedArtifactMatchesDeliverable') is not True:return None
    if runtime.get('processRunning') is not True or runtime.get('crashObserved') is not False:return None
    return runtime

def run(args):
    # Windows SDK tools are batch files. shell=False requires cmd.exe for .bat only.
    if os.name=='nt' and str(args[0]).endswith('.bat'):
        args=['cmd.exe','/d','/c',subprocess.list2cmdline([str(x) for x in args])]
    result=subprocess.run([str(x) for x in args],capture_output=True,text=True)
    if result.returncode:raise RuntimeError(result.stdout+'\n'+result.stderr)
    return result.stdout+result.stderr

def main():
    p=argparse.ArgumentParser();p.add_argument('apk',type=Path);p.add_argument('--sdk',type=Path,required=True);p.add_argument('--product',choices=['manager','core'],required=True);p.add_argument('--signing',choices=['debug','test','production'],required=True);a=p.parse_args()
    apk=a.apk.resolve();sdk=a.sdk.resolve();bt=sdk/'build-tools/34.0.0'
    expected={'manager':'app.javo.manager','core':'app.revanced.android.gms'}[a.product]
    java=Path(os.environ['JAVA_HOME'])/'bin'/('java.exe' if os.name=='nt' else 'java') if os.getenv('JAVA_HOME') else shutil.which('java')
    if not java:raise RuntimeError('JAVA_HOME or Java on PATH is required to verify Android signatures')
    sig=run([java,'-jar',bt/'lib/apksigner.jar','verify','--verbose','--print-certs',apk])
    badging=run([bt/('aapt2.exe' if os.name=='nt' else 'aapt2'),'dump','badging',apk])
    package=re.search(r"package: name='([^']+)' versionCode='(\d+)' versionName='([^']+)'",badging)
    if not package or package[1]!=expected:raise RuntimeError('APK identity differs from expected Javo identity')
    min_sdk=re.search(r"sdkVersion:'(\d+)'",badging)
    certificate=re.search(r'Signer #1 certificate SHA-256 digest: ([a-fA-F0-9]+)',sig)
    if not certificate or not min_sdk:raise RuntimeError('Missing signing or compatibility metadata')
    label='Javo Manager' if a.product=='manager' else 'Javo Core'
    if a.product=='core' and "application-label:'Javo Core'" not in badging:raise RuntimeError('Javo Core application label not applied')
    if a.signing=='production' and ('Android Debug' in sig or 'androiddebugkey' in sig.lower()):raise RuntimeError('Debug key cannot be labeled production')
    with apk.open('rb') as f:digest=hashlib.file_digest(f,'sha256').hexdigest()
    name=f'javo-{a.product}-{package[3]}-{a.signing}.apk'
    output=ROOT/'deliverables/apks'/name;output.parent.mkdir(parents=True,exist_ok=True)
    if apk!=output.resolve():shutil.copy2(apk,output)
    website=ROOT/'apps/javo-website/dist/downloads/files'/name;website.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(apk,website)
    evidence=ROOT/'evidence';evidence.mkdir(exist_ok=True)
    (evidence/f'{a.product}-apk-signature.txt').write_text(sig,encoding='utf-8')
    (evidence/f'{a.product}-apk-badging.txt').write_text(badging,encoding='utf-8')
    record={'id':a.product,'displayName':label,'applicationId':expected,'version':package[3],'versionCode':int(package[2]),'minSdk':int(min_sdk[1]),'channel':'development','signing':a.signing,'verification':'signature-and-package-verified','sha256':digest,'signerSha256':certificate[1].lower(),'size':apk.stat().st_size,'path':'/downloads/files/'+name,'runtimeVerified':False}
    with zipfile.ZipFile(apk) as z:
        record['architectures']=sorted({n.split('/')[1] for n in z.namelist() if n.startswith('lib/') and n.endswith('.so')}) or ['universal']
    runtime=evidence/f'{a.product}-runtime-smoke.json'
    if runtime.exists():
        matched=runtime_for_artifact(json.loads(runtime.read_text()),digest,expected)
        if matched:record['runtimeSmoke']=matched
    catalog_path=ROOT/'infrastructure/release/catalog.json';catalog=json.loads(catalog_path.read_text());catalog['artifacts']=[x for x in catalog['artifacts'] if x['id']!=a.product]+[record];catalog_path.write_text(json.dumps(catalog,indent=2)+'\n')
    (output.parent/'SHA256SUMS').write_text(''.join(f"{x['sha256']}  {Path(x['path']).name}\n" for x in catalog['artifacts']))
    print(json.dumps(record,indent=2))
if __name__=='__main__':main()
