"""Install and launch an exact development APK on an isolated Android emulator."""
from pathlib import Path
import argparse,hashlib,json,os,re,shlex,shutil,subprocess,tempfile,time

ROOT=Path(__file__).resolve().parents[2]
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument('package',choices=['app.javo.manager','app.revanced.android.gms'])
    parser.add_argument('apk',type=Path)
    parser.add_argument('--serial')
    args=parser.parse_args()
    product='manager' if args.package=='app.javo.manager' else 'core'
    evidence=ROOT/'evidence';shots=evidence/'screenshots';shots.mkdir(parents=True,exist_ok=True)
    adb=shutil.which('adb')
    if not adb:
        sdk=os.environ.get('ANDROID_HOME') or str(ROOT/'tools/android-sdk')
        adb=str(Path(sdk)/'platform-tools'/('adb.exe' if os.name=='nt' else 'adb'))
    def invoke(command):
        result=subprocess.run(command,capture_output=True,timeout=180)
        if result.returncode:raise RuntimeError(result.stdout.decode(errors='replace')+result.stderr.decode(errors='replace'))
        return result.stdout
    if args.serial:
        serial=args.serial
    else:
        devices=invoke([adb,'devices']).decode()
        candidates=re.findall(r'^(emulator-\d+)\s+device\s*$',devices,re.MULTILINE)
        if len(candidates)!=1:raise RuntimeError('Select exactly one running Android emulator with --serial')
        serial=candidates[0]
    if not re.fullmatch(r'emulator-\d+',serial):raise RuntimeError('Smoke testing is restricted to emulator devices')
    def run(*parts):return invoke([adb,'-s',serial,*parts])
    apk=args.apk.resolve()
    with apk.open('rb') as f:expected=hashlib.file_digest(f,'sha256').hexdigest()
    report={'device':serial,'package':args.package,'processRunning':False,'crashObserved':False,
            'installedArtifactMatchesDeliverable':False,'authentication':'not-tested',
            'pushDelivery':'not-tested','playback':'not-tested'}
    try:
        report['androidApi']=int(run('shell','getprop','ro.build.version.sdk').decode().strip())
        report['architecture']=run('shell','getprop','ro.product.cpu.abi').decode().strip()
        report['bootCompleted']=run('shell','getprop','sys.boot_completed').decode().strip()=='1'
        if not report['bootCompleted']:raise RuntimeError('Emulator has not completed boot')
        run('logcat','-c')
        install=run('install','-r',str(apk)).decode()
        (evidence/f'{product}-emulator-install.txt').write_text(install,encoding='utf-8')
        if 'Success' not in install:raise RuntimeError('APK installation did not succeed')
        paths=run('shell','pm','path',args.package).decode().splitlines()
        remote=next(p.removeprefix('package:') for p in paths if p.startswith('package:') and p.endswith('/base.apk'))
        try:
            # Hash the installed APK without transferring it over the emulator connection.
            result=run('shell',f'sha256sum {shlex.quote(remote)}').decode().strip()
            digest=re.fullmatch(r'([a-f0-9]{64})\s+.+',result)
            if not digest:raise RuntimeError('Emulator did not return an APK checksum')
            actual=digest[1]
        except (RuntimeError, subprocess.TimeoutExpired):
            with tempfile.TemporaryDirectory(prefix='javo-installed-apk-') as folder:
                installed=Path(folder)/'installed.apk'
                for attempt in range(3):
                    try:
                        run('pull',remote,str(installed))
                        break
                    except (RuntimeError, subprocess.TimeoutExpired):
                        if attempt==2:raise
                        time.sleep(2)
                with installed.open('rb') as f:actual=hashlib.file_digest(f,'sha256').hexdigest()
        report['installedApkSha256']=actual
        report['installedArtifactMatchesDeliverable']=actual==expected
        if actual!=expected:raise RuntimeError('Installed package differs from the requested APK')
        launch=run('shell','monkey','-p',args.package,'-c','android.intent.category.LAUNCHER','1').decode()
        (evidence/f'{product}-emulator-launch.txt').write_text(launch,encoding='utf-8')
        time.sleep(15)
        report['processRunning']=bool(run('shell','pidof',args.package).strip())
        crash=run('logcat','-d','-b','crash').decode(errors='replace')
        (evidence/f'{product}-emulator-crashes.txt').write_text(crash,encoding='utf-8')
        report['crashObserved']=f'Process: {args.package}' in crash
        png=run('exec-out','screencap','-p')
        if not png.startswith(b'\x89PNG\r\n\x1a\n'):raise RuntimeError('Invalid emulator screenshot')
        (shots/f'{product}-emulator.png').write_bytes(png)
        if not report['processRunning'] or report['crashObserved']:raise RuntimeError('App launch smoke check failed')
    except Exception as error:
        report['failure']=str(error)
        raise
    finally:
        (evidence/f'{product}-runtime-smoke.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,indent=2))
    print('Install/launch smoke check passed; account, message delivery and playback integration were not tested.')

if __name__=='__main__':main()
