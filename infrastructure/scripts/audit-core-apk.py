"""Enforce the consumer Core APK permission and backup policy."""
from pathlib import Path
import argparse
import hashlib
import json
import os
import re
import shutil
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('apk', type=Path)
parser.add_argument('--sdk', type=Path, required=True)
parser.add_argument('--release', action='store_true', help='Require the pinned offline release signer.')
args = parser.parse_args()
build_tools = args.sdk / 'build-tools/34.0.0'
tool = build_tools / ('aapt2.exe' if os.name == 'nt' else 'aapt2')
java_home = os.environ.get('JAVA_HOME')
java = Path(java_home) / 'bin' / ('java.exe' if os.name == 'nt' else 'java') if java_home else shutil.which('java')
if not java:
    raise RuntimeError('Java is required to verify the Core signer')
signature = subprocess.run(
    [str(java), '-jar', str(build_tools / 'lib/apksigner.jar'), 'verify', '--verbose',
     '--print-certs', str(args.apk)], check=True, capture_output=True, text=True
).stdout
signer = re.search(r'^Signer #1 certificate SHA-256 digest: ([a-f0-9]{64})$', signature, re.MULTILINE)
expected_signer = '47aa6be0fa482463dec325dac7864c771e6a75465c973c29587bc5fe6f4d3873'
if not signer or 'Number of signers: 1' not in signature.splitlines():
    raise RuntimeError('Expected a single verified Core signer')
if args.release and signer[1] != expected_signer:
    raise RuntimeError('Core signing identity differs from the pinned offline release signer')
if 'Verified using v2 scheme (APK Signature Scheme v2): true' not in signature.splitlines():
    raise RuntimeError('Core requires a verified APK Signature Scheme v2 signature')

def dump(*parts):
    return subprocess.run([str(tool), 'dump', *parts, str(args.apk)], check=True, capture_output=True, text=True).stdout

permissions = dump('permissions')
badging = dump('badging')
target_sdk = re.search(r"^targetSdkVersion:'(\d+)'", badging, re.MULTILINE)
if not target_sdk or not badging.startswith("package: name='app.revanced.android.gms' "):
    raise RuntimeError('Could not verify Core package and target SDK')
manifest = subprocess.run(
    [str(tool), 'dump', 'xmltree', str(args.apk), '--file', 'AndroidManifest.xml'],
    check=True, capture_output=True, text=True
).stdout
requested = sorted(set(re.findall(r"uses-permission[^\n]*name='([^']+)'", permissions)))
if not requested:
    raise RuntimeError('Could not inspect APK permissions')
forbidden = {
    'android.permission.RECEIVE_SMS', 'android.permission.READ_SMS',
    'android.permission.SEND_SMS', 'android.permission.SYSTEM_ALERT_WINDOW',
    'android.permission.FAKE_PACKAGE_SIGNATURE', 'android.permission.BIND_ACCESSIBILITY_SERVICE',
    'android.permission.BIND_NOTIFICATION_LISTENER_SERVICE', 'android.permission.REQUEST_INSTALL_PACKAGES',
    'android.permission.READ_EXTERNAL_STORAGE', 'android.permission.WRITE_EXTERNAL_STORAGE',
}
violations = sorted(forbidden.intersection(requested))
if violations:
    raise RuntimeError('Core requests out-of-scope permissions: ' + ', '.join(violations))
for flag in ('debuggable', 'allowBackup', 'usesCleartextTraffic'):
    if not re.search(r'android:' + flag + r'\([^\n]+?\)=(?:false|\(type 0x12\)0x0)\b', manifest):
        # Non-debuggable is the default when omitted from a release manifest.
        if flag == 'debuggable' and 'android:debuggable' not in manifest:
            continue
        raise RuntimeError('Core must disable ' + flag)
network = subprocess.run(
    [str(tool), 'dump', 'xmltree', str(args.apk), '--file', 'res/xml/network_security_config.xml'],
    check=True, capture_output=True, text=True,
).stdout
# A network security config takes precedence over usesCleartextTraffic on Android 7+.
network_flags = re.findall(r'cleartextTrafficPermitted[^\n=]*=(.*)', network)
if network_flags != ['false'] and network_flags != ['(type 0x12)0x0']:
    raise RuntimeError('Core network config must deny cleartext and contain no exceptions')
with zipfile.ZipFile(args.apk) as archive:
    if len(archive.namelist()) != len(set(archive.namelist())) or archive.testzip() is not None:
        raise RuntimeError('Core APK archive failed integrity checks')
report = {
    'package': 'app.revanced.android.gms', 'apkSha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
    'signerSha256': signer[1], 'signatureSchemeV2Verified': True, 'targetSdk': int(target_sdk[1]),
    'requestedPermissions': requested,
    'forbiddenPermissionsAbsent': True, 'debuggable': False, 'allowBackup': False,
    'usesCleartextTraffic': False, 'offlineReleaseSignerVerified': args.release,
    'playProtectVerdict': 'requires-device-or-Google-classification-check',
}
root = Path(__file__).resolve().parents[2]
(root / 'evidence/core-security-audit.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
