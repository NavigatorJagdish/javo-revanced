from pathlib import Path
import subprocess,json,hashlib,time,re
import argparse
parser=argparse.ArgumentParser(description="Optional local integration; never uploads proprietary APKs.")
parser.add_argument('--source-apk',type=Path,required=True)
parser.add_argument('--manager-apk',type=Path,required=True)
parser.add_argument('--test-apk',type=Path,required=True)
parser.add_argument('--serial',default='emulator-5556')
args=parser.parse_args()
root=Path(__file__).resolve().parents[2]; adb=root/'tools/android-sdk/platform-tools/adb.exe'; serial=args.serial
if not re.fullmatch(r'emulator-\d+', serial):
 raise RuntimeError('Real-app integration requires an isolated Android emulator')
(root/'evidence').mkdir(exist_ok=True)
(root/'tools/alpha3').mkdir(parents=True, exist_ok=True)
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
# The supplied Manager and instrumentation APKs must have matching signing identities.
manager=args.manager_apk
run('install','-t','-r',manager)
test=args.test_apk
run('install','-t','-r',test)
launch=run('shell','am','start','-W','-n','app.javo.manager/app.revanced.manager.MainActivity')
if b'Error' in launch or b'Status: ok' not in launch:
 raise RuntimeError('Manager launcher activity did not start: '+launch.decode(errors='replace'))
source=args.source_apk
remote_input = '/sdcard/Android/data/app.javo.manager/files/real-input.apk'
run('shell','mkdir','-p','/sdcard/Android/data/app.javo.manager/files')
run('push',source,remote_input)
# keep logs available even when logd is busy
output=run('shell','am','instrument','-w','-e','realInput',remote_input,'app.javo.manager.test/app.javo.integration.PatchIntegrationInstrumentation',timeout=1250)
(root/'evidence/alpha3-real-instrumentation.log').write_bytes(output)
(root/'tools/alpha3/real-logcat.txt').write_bytes(run('logcat','-d'))
result = re.search(rb'^INSTRUMENTATION_RESULT: result=(\{[^\r\n]+\})', output, re.MULTILINE)
if not result:
 raise RuntimeError('Real-app instrumentation did not return its result')
report=json.loads(result[1])
with manager.open('rb') as stream:
 expected_manager = hashlib.file_digest(stream, 'sha256').hexdigest()
if report.get('managerApkSha256') != expected_manager:
 raise RuntimeError('Instrumentation ran against a different Manager APK')
report['androidApi']=int(run('shell','getprop','ro.build.version.sdk'))
report['architecture']=run('shell','getprop','ro.product.cpu.abi').decode().strip()
report['testBuild']='provided Manager APK; see managerApkSha256'
report['sourceSignatureVerified']=True
report['installed']=False
path=root/'evidence/alpha3-real-youtube.json';path.write_text(json.dumps(report,indent=2)+'\n')
if not report['success']:raise RuntimeError(report.get('failure'))
out=root/'tools/alpha3/youtube-patched-local.apk'
run('pull','/sdcard/Android/data/app.javo.manager/files/local-javo-video-test.apk',out)
assert hashlib.file_digest(out.open('rb'),'sha256').hexdigest()==report['signedApkSha256']
report['installResult']=run('install','-r',out).decode().strip();report['installed']='Success' in report['installResult']
path.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))

