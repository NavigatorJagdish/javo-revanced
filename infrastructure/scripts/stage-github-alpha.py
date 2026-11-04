"""Stage the exact Android outputs from the successful Javo alpha build."""

import hashlib
import json
from pathlib import Path
import shutil
import sys
import zipfile


EXPECTED_ARCHIVE = "bf35bc03a2ae47cef318276f49603ecb2bb78022482a20be5933315d841f4e69"
EXPECTED_APKS = {
    "javo-manager-0.1.0-alpha.1-debug.apk": "96041d7de29aef3fadc35ab11ed5d60793c95123e31a06bf0b6369d87f69659a",
    "javo-core-0.3.13.2.250932-debug.apk": "07eeaf00757e778e7d155bfad977a20efdb873905051712b91905a9817155449",
}
BUNDLE = "javo-branding-0.1.0-alpha.1.rvp"
EXPECTED_BUNDLE = "629aa5aadf241c980492204e9c6ca067c3ba3605ec703e9c8dca3801dec5ccd9"


def sha256(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def stage(archive_path, root):
    if sha256(archive_path) != EXPECTED_ARCHIVE:
        raise ValueError("The downloaded archive differs from the verified CI output")

    with zipfile.ZipFile(archive_path) as archive:
        def copy(member, destination, expected=None):
            target = root / destination
            target.parent.mkdir(parents=True, exist_ok=True)
            with archive.open(member) as source, target.open("wb") as output:
                shutil.copyfileobj(source, output)
            if expected and sha256(target) != expected:
                raise ValueError(f"Invalid release asset hash for {target.name}")
            return target

        catalog = json.loads(archive.read("infrastructure/release/catalog.json"))
        catalog_hashes = {
            Path(item["path"]).name: item["sha256"] for item in catalog["artifacts"]
        }
        if catalog_hashes != EXPECTED_APKS:
            raise ValueError("Unexpected APKs in the verified catalog")

        for name, digest in EXPECTED_APKS.items():
            apk = copy(f"apks/{name}", f"deliverables/apks/{name}", digest)
            download = root / "apps/javo-website/dist/downloads/files" / name
            download.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(apk, download)

        bundle = copy(f"bundles/{BUNDLE}", f"deliverables/bundles/{BUNDLE}", EXPECTED_BUNDLE)
        build_bundle = root / "patches/javo-branding/build/libs" / BUNDLE
        build_bundle.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(bundle, build_bundle)

        evidence = (
            "manager-runtime-smoke.json", "core-runtime-smoke.json",
            "manager-patch-integration.json", "manager-build.log", "core-build.log",
            "manager-instrumentation-build.log", "manager-patch-instrumentation.log",
            "manager-apk-signature.txt", "core-apk-signature.txt",
            "manager-apk-badging.txt", "core-apk-badging.txt",
            "node-tests.log", "java-security-tests.log", "java-branding-tests.log",
            "runtime-evidence-tests.log",
            "screenshots/manager-emulator.png", "screenshots/core-emulator.png",
            "screenshots/manager-music-patched-fixture.png",
            "screenshots/manager-video-patched-fixture.png",
        )
        for name in evidence:
            copy(f"evidence/{name}", f"evidence/{name}")

    catalog["status"] = "functional-alpha"
    catalog["limitations"] = [
        "Both debug APKs passed signature verification and emulator installation and launch.",
        "Two first-party branding fixtures passed patch, sign, install and launch checks.",
        "Real-app playback, Google account authentication, notifications and physical devices remain unverified.",
        "Production signing and a published Javo update feed are unconfigured.",
    ]
    (root / "infrastructure/release/catalog.json").write_text(
        json.dumps(catalog, indent=2) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    stage(Path(sys.argv[1]), Path(__file__).resolve().parents[2])
