"""Run Manager's production patch/sign path on an isolated first-party fixture."""
from pathlib import Path
import hashlib,json,os,re,shutil,subprocess

ROOT=Path(__file__).resolve().parents[2]
def main():
    adb=shutil.which('adb') or str(ROOT/'tools/android-sdk/platform-tools/adb.exe')
    def invoke(*parts):
        p=subprocess.run([adb,*map(str,parts)],capture_output=True,timeout=300)
        if p.returncode:raise RuntimeError(p.stdout.decode(errors='replace')+p.stderr.decode(errors='replace'))
        return p.stdout
    devices=re.findall(r'^(emulator-\d+)\s+device\s*$',invoke('devices').decode(),re.MULTILINE)
    if len(devices)!=1:raise RuntimeError('Exactly one emulator is required')
    def run(*parts):return invoke('-s',devices[0],*parts)
    evidence=ROOT/'evidence';evidence.mkdir(exist_ok=True)
    path=evidence/'manager-patch-integration.json'
    path.write_text(json.dumps({'success':False,'status':'started'})+'\n')
    test_apks=list((ROOT/'apps/javo-manager/app/build/outputs/apk/androidTest/debug').glob('*.apk'))
    if len(test_apks)!=1:raise RuntimeError('Expected one Android instrumentation APK')
    run('install','-t','-r',test_apks[0])
    # Never accept a result left by an earlier instrumentation invocation.
    run('shell','run-as','app.javo.manager','rm','-f','files/integration/result.json')
    output=run('shell','am','instrument','-w','app.javo.manager.test/app.javo.integration.PatchIntegrationInstrumentation')
    (evidence/'manager-patch-instrumentation.log').write_bytes(output)
    report=json.loads(run('exec-out','run-as','app.javo.manager','cat','files/integration/result.json'))
    instrumentation_success=report.get('success') is True
    report['instrumentationSuccess']=instrumentation_success
    report['success']=False
    report['device']=devices[0]
    report['androidApi']=int(run('shell','getprop','ro.build.version.sdk').decode().strip())
    report['architecture']=run('shell','getprop','ro.product.cpu.abi').decode().strip()
    path.write_text(json.dumps(report,indent=2)+'\n')
    if not instrumentation_success:raise RuntimeError(report.get('failure','Instrumentation did not pass'))
    if not re.search(rb'INSTRUMENTATION_CODE:\s*-1\b',output):raise RuntimeError('Instrumentation did not finish successfully')
    manager_apks=list((ROOT/'deliverables/apks').glob('javo-manager-*.apk'))
    if len(manager_apks)!=1:raise RuntimeError('Expected exactly one verified Manager APK')
    with manager_apks[0].open('rb') as stream:manager_digest=hashlib.file_digest(stream,'sha256').hexdigest()
    if report.get('managerApkSha256')!=manager_digest:raise RuntimeError('Instrumentation ran against a different Manager APK')
    for product in report['products']:
        identifier=product['id'];package=product['packageName']
        fixture=ROOT/f'tools/integration-fixture/{identifier}-patched-signed.apk';fixture.parent.mkdir(parents=True,exist_ok=True)
        fixture.write_bytes(run('exec-out','run-as','app.javo.manager','cat',f'files/integration/{identifier}-patched-signed.apk'))
        if hashlib.sha256(fixture.read_bytes()).hexdigest()!=product['signedApkSha256']:raise RuntimeError('Extracted fixture differs from patched/signed output')
        result=run('install','-r',fixture)
        (evidence/f'manager-{identifier}-fixture-install.log').write_bytes(result)
        if b'Success' not in result:raise RuntimeError('Fixture installation failed')
        run('logcat','-c')
        run('shell','am','start','-W','-n',package+'/app.javo.fixture.MainActivity')
        product['patchedFixtureInstalled']=True
        product['fixtureProcessRunning']=bool(run('shell','pidof',package).strip())
        product['fixtureCrashObserved']=('Process: '+package).encode() in run('logcat','-d','-b','crash')
        product['success']=product['fixtureProcessRunning'] and not product['fixtureCrashObserved']
        screenshot=evidence/f'screenshots/manager-{identifier}-patched-fixture.png';screenshot.parent.mkdir(exist_ok=True)
        screenshot.write_bytes(run('exec-out','screencap','-p'))
    report['success']=all(x['success'] for x in report['products'])
    path.write_text(json.dumps(report,indent=2)+'\n')
    if not report['success']:raise RuntimeError('Patched first-party fixture did not launch cleanly')
    print(json.dumps(report,indent=2))
    print('First-party fixture only; real YouTube/Music, playback and account integration remain untested.')
if __name__=='__main__':main()
