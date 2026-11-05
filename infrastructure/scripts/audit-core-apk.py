"""Enforce the consumer Core APK permission and backup policy."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument('apk', type=Path)
parser.add_argument('--sdk', type=Path, required=True)
args = parser.parse_args()
tool = args.sdk / 'build-tools/34.0.0' / ('aapt2.exe' if __import__('os').name == 'nt' else 'aapt2')
def dump(*parts):
    return subprocess.run([str(tool), 'dump', *parts, str(args.apk)], check=True, capture_output=True, text=True).stdout

permissions = dump('permissions')
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
}
violations = sorted(forbidden.intersection(requested))
if violations:
    raise RuntimeError('Core requests out-of-scope permissions: ' + ', '.join(violations))
for flag in ('debuggable', 'allowBackup'):
    if not re.search(r'android:' + flag + r'\([^\n]+?\)=(?:false|\(type 0x12\)0x0)\b', manifest):
        # Non-debuggable is the default when omitted from a release manifest.
        if flag == 'debuggable' and 'android:debuggable' not in manifest:
            continue
        raise RuntimeError('Core must disable ' + flag)
report = {
    'package': 'app.revanced.android.gms', 'apkSha256': hashlib.sha256(args.apk.read_bytes()).hexdigest(),
    'requestedPermissions': requested,
    'forbiddenPermissionsAbsent': True, 'debuggable': False, 'allowBackup': False,
    'playProtectVerdict': 'requires-device-or-Google-classification-check',
}
root = Path(__file__).resolve().parents[2]
(root / 'evidence/core-security-audit.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
