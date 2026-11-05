from pathlib import Path
import subprocess,json,hashlib,time,os
import argparse
parser=argparse.ArgumentParser(description="Optional local integration; never uploads proprietary APKs.")
parser.add_argument('--source-apk',type=Path,required=True)
parser.add_argument('--manager-apk',type=Path,required=True)
parser.add_argument('--test-apk',type=Path,required=True)
parser.add_argument('--serial',default='emulator-5556')
args=parser.parse_args()
root=Path(__file__).resolve().parents[2]; adb=root/'tools/android-sdk/platform-tools/adb.exe'; serial=args.serial
assert hashlib.file_digest(args.source_apk.open('rb'),'sha256').hexdigest()=='b7659da492a1ebd8bd7cea2909be4ee1f58e00a2586d65a1c91b2e1e5ec6acd1', 'Use the signature-verified reference APK for this check' 
def run(*args,timeout=150,**kwargs):
 p=subprocess.run([str(adb),'-s',serial,*map(str,args)],capture_output=True,timeout=timeout,**kwargs)
 if p.returncode: raise RuntimeError(p.stdout.decode(errors='replace')+p.stderr.decode(errors='replace'))
 return p.stdout
run('wait-for-device')
for n in range(180):
 if run('shell','getprop','sys.boot_completed').strip()==b'1':break
 time.sleep(1)
else:raise RuntimeError('boot timeout')
# Exact local Manager production build with the same application sources as alpha.3; CI uses a different development certificate.
manager=args.manager_apk
run('install','-t','-r',manager)
test=args.test_apk
run('install','-t','-r',test)
run('shell','am','start','-W','-n','app.javo.manager/app.revanced.manager.ui.activity.MainActivity')
source=args.source_apk
with source.open('rb') as inp:
 run('exec-in','run-as','app.javo.manager','sh','-c','cat > files/real-input.apk',stdin=inp)
run('shell','run-as','app.javo.manager','rm','-f','files/real-integration/result.json')
# keep logs available even when logd is busy
output=run('shell','am','instrument','-w','-e','realInput','/data/user/0/app.javo.manager/files/real-input.apk','app.javo.manager.test/app.javo.integration.PatchIntegrationInstrumentation',timeout=1250)
(root/'evidence/alpha3-real-instrumentation.log').write_bytes(output)
(root/'tools/alpha3/real-logcat.txt').write_bytes(run('logcat','-d'))
report=json.loads(run('exec-out','run-as','app.javo.manager','cat','files/real-integration/result.json'))
report['androidApi']=int(run('shell','getprop','ro.build.version.sdk'))
report['architecture']=run('shell','getprop','ro.product.cpu.abi').decode().strip()
report['testBuild']='provided Manager APK; see managerApkSha256'
report['sourceSignatureVerified']=True
report['installed']=False
path=root/'evidence/alpha3-real-youtube.json';path.write_text(json.dumps(report,indent=2)+'\n')
if not report['success']:raise RuntimeError(report.get('failure'))
out=root/'tools/alpha3/youtube-patched-local.apk'
with out.open('wb') as target:
 p=subprocess.run([str(adb),'-s',serial,'exec-out','run-as','app.javo.manager','cat','files/real-integration/youtube-signed.apk'],stdout=target,check=True,timeout=150)
assert hashlib.file_digest(out.open('rb'),'sha256').hexdigest()==report['signedApkSha256']
report['installResult']=run('install','-r',out).decode().strip();report['installed']='Success' in report['installResult']
path.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))


