"""Probe private Core components as an external UID on an isolated emulator."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('apk', type=Path)
parser.add_argument('--serial')
args = parser.parse_args()
sdk = Path(os.environ.get('ANDROID_HOME', str(root / 'tools/android-sdk')))
adb = sdk / 'platform-tools' / ('adb.exe' if os.name == 'nt' else 'adb')

def run(*parts):
    return subprocess.run([str(adb), *parts], capture_output=True, text=True, timeout=90)

serial = args.serial
if not serial:
    devices = re.findall(r'^(emulator-\d+)\s+device\s*$', run('devices').stdout, re.M)
    if len(devices) != 1: raise RuntimeError('Select one isolated emulator')
    serial = devices[0]
if not re.fullmatch(r'emulator-\d+', serial): raise RuntimeError('Only emulator testing is allowed')
with args.apk.open('rb') as stream: expected = hashlib.file_digest(stream, 'sha256').hexdigest()
package = 'app.revanced.android.gms'
path = run('-s', serial, 'shell', 'pm', 'path', package).stdout.strip().removeprefix('package:')
if not path.startswith('/data/app/') or '\n' in path: raise RuntimeError('Unexpected installed APK path')
actual = run('-s', serial, 'shell', 'sha256sum', path).stdout.split()[0]
if actual != expected: raise RuntimeError('Boundary probes target a different Core APK')
probes = []
for kind, component in [
    ('activity', 'org.microg.gms.auth.AskPermissionActivity'),
    ('activity', 'org.microg.gms.auth.AskPackageOverrideActivity'),
    ('service', 'org.microg.gms.auth.loginservice.GoogleLoginService'),
    ('service', 'org.microg.gms.location.manager.LocationManagerService'),
    ('service', 'org.microg.gms.location.provider.NetworkLocationProviderService'),
]:
    command = 'start' if kind == 'activity' else 'startservice'
    result = run('-s', serial, 'shell', 'am', command, '--user', '0', '-n', package + '/' + component)
    denied = bool(re.search(r'not exported|Permission Denial', result.stdout + result.stderr, re.I))
    probes.append({'component':component, 'externalStartDenied':denied})
    if not denied: break
report = {'apkSha256':expected, 'device':serial, 'probes':probes,
          'success':len(probes) == 5 and all(p['externalStartDenied'] for p in probes),
          'limit':'External component-start boundaries only; account, network and exploit chains are not fully tested.'}
(root / 'evidence/core-boundary-probes.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
if not report['success']: raise SystemExit('A Core component boundary did not deny the external start')
