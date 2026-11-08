"""Audit the merged Manager APK and optionally prove signing continuity with alpha.8."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[2]
FORBIDDEN = {
    "READ_SMS", "RECEIVE_SMS", "SEND_SMS", "BIND_ACCESSIBILITY_SERVICE",
    "BIND_NOTIFICATION_LISTENER_SERVICE", "SYSTEM_ALERT_WINDOW", "QUERY_ALL_PACKAGES",
    "MANAGE_EXTERNAL_STORAGE", "READ_EXTERNAL_STORAGE", "WRITE_EXTERNAL_STORAGE",
    "REQUEST_IGNORE_BATTERY_OPTIMIZATIONS",
    "ENFORCE_UPDATE_OWNERSHIP", "UPDATE_PACKAGES_WITHOUT_USER_ACTION",
}


def inspect(apk, sdk):
    build_tools = sdk / "build-tools/34.0.0"
    aapt = build_tools / ("aapt2.exe" if os.name == "nt" else "aapt2")
    java_home = os.getenv("JAVA_HOME")
    java = Path(java_home) / "bin" / ("java.exe" if os.name == "nt" else "java") if java_home else shutil.which("java")
    if not java:
        raise RuntimeError("Java is required to verify the APK")

    def run(*command):
        return subprocess.run([str(x) for x in command], check=True, capture_output=True, text=True).stdout

    signature = run(java, "-jar", build_tools / "lib/apksigner.jar", "verify", "--verbose", "--print-certs", apk)
    manifest = run(aapt, "dump", "xmltree", apk, "--file", "AndroidManifest.xml")
    badging = run(aapt, "dump", "badging", apk)
    if not badging.startswith("package: name='app.javo.manager' "):
        raise RuntimeError("APK is not Javo Manager")
    permissions = sorted(set(re.findall(r"uses-permission[^\n]*name='([^']+)'", run(aapt, "dump", "permissions", apk))))
    if not permissions:
        raise RuntimeError("Could not read requested permissions")

    def flag(name):
        match = re.search(r'android:' + name + r'\([^\n]+?\)=(true|false|\(type 0x12\)0x[0-9a-f]+)', manifest)
        return None if not match else match[1] not in ("false", "(type 0x12)0x0")

    signer = re.search(r"^Signer #1 certificate SHA-256 digest: ([0-9a-f]{64})$", signature, re.M)
    if not signer or "Number of signers: 1" not in signature.splitlines():
        raise RuntimeError("Expected a single verified signer")
    with apk.open("rb") as stream:
        digest = hashlib.file_digest(stream, "sha256").hexdigest()
    with zipfile.ZipFile(apk) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or archive.testzip() is not None:
            raise RuntimeError("APK archive failed integrity checks")
    return {
        "package": "app.javo.manager", "apkSha256": digest, "signerSha256": signer[1],
        "signatureV2Verified": "Verified using v2 scheme (APK Signature Scheme v2): true" in signature.splitlines(),
        "debuggable": "application-debuggable" in badging or flag("debuggable") is True,
        "allowBackup": flag("allowBackup"), "usesCleartextTraffic": flag("usesCleartextTraffic"),
        "requestedPermissions": permissions,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--sdk", type=Path, required=True)
    parser.add_argument("--baseline", type=Path)
    args = parser.parse_args()
    candidate = inspect(args.apk, args.sdk)
    if candidate["debuggable"] or candidate["allowBackup"] is not False or candidate["usesCleartextTraffic"] is not False:
        raise RuntimeError("Manager must disable debugging, backup and cleartext traffic")
    if not candidate["signatureV2Verified"]:
        raise RuntimeError("Manager requires a verified APK Signature Scheme v2 signature")
    forbidden = sorted(set(candidate["requestedPermissions"]) & {"android.permission." + p for p in FORBIDDEN})
    if forbidden:
        raise RuntimeError("Manager requests restricted permissions: " + ", ".join(forbidden))
    candidate["playProtectVerdict"] = "Google classification pending"
    if args.baseline:
        baseline = inspect(args.baseline, args.sdk)
        receipt = json.loads((ROOT / "evidence/release-alpha8.json").read_text(encoding="utf-8"))
        published = next(asset for asset in receipt["assets"] if asset["name"].startswith("javo-manager-"))
        if baseline["apkSha256"] != published["sha256"] or baseline["signerSha256"] != receipt["managerSignerSha256"]:
            raise RuntimeError("The baseline does not match the published alpha.8")
        if candidate["signerSha256"] != baseline["signerSha256"]:
            raise RuntimeError("Manager signing identity changed")
        candidate["publishedBaseline"] = baseline
        candidate["removedPermissions"] = sorted(set(baseline["requestedPermissions"]) - set(candidate["requestedPermissions"]))
    result = json.dumps(candidate, indent=2) + "\n"
    (ROOT / "evidence/manager-security-audit.json").write_text(result, encoding="utf-8")
    print(result)


if __name__ == "__main__":
    main()
