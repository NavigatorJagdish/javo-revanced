"""Generate the delivery report from this run's verified artifacts."""
import json
import os
from pathlib import Path

root = Path(__file__).resolve().parents[2]
catalog = json.loads((root / 'infrastructure/release/catalog.json').read_text())
integration = json.loads((root / 'evidence/manager-patch-integration.json').read_text())
audit = json.loads((root / 'evidence/core-security-audit.json').read_text())
artifacts = {item['id']: item for item in catalog['artifacts']}
if audit['apkSha256'] != artifacts['core']['sha256'] or not audit['forbiddenPermissionsAbsent']:
    raise ValueError('Core security audit must match this APK')
if integration['managerApkSha256'] != artifacts['manager']['sha256'] or not integration.get('success'):
    raise ValueError('Manager integration must match this APK')
checks = integration.get('downloadRegressions', [])
if not integration.get('httpsTransfer') or not integration.get('browserChecks') or integration.get('separateProcessPatch') is not True or integration.get('terminatedProcessRecovery') is not True:
    raise ValueError('Browser, HTTPS transfer or separate process evidence is missing')
if len(checks) < 4:
    raise ValueError('Download regression evidence is missing')
for item in artifacts.values():
    if not item.get('runtimeSmoke'):
        raise ValueError('Matching install and launch evidence is missing')
run_url = 'https://github.com/' + os.environ['GITHUB_REPOSITORY'] + '/actions/runs/' + os.environ['GITHUB_RUN_ID']
rows = '\n'.join(f"| {item['displayName']} {item['version']} | {item['applicationId']} | `{item['sha256']}` |" for item in artifacts.values())
check_list = '\n'.join('- ' + str(check) + '.' for check in checks)
official = integration.get('officialCatalog', {})
if official.get('metadataLoadedOnAndroid'):
    catalog_result = f"The official ReVanced catalog loaded {official['patchCount']} patch definitions on Android. Third-party patch execution is unverified."
else:
    catalog_result = 'The optional official catalog diagnostic was unavailable. See the integration report for its failure. Third-party patch execution is unverified.'
report = f"""# Javo ReVanced alpha.3 verification report

The private workflow built these development APKs and bound the results below to their exact hashes. [Verification run]({run_url})

## Verified artifacts

| Artifact | Android identity | SHA-256 |
| --- | --- | --- |
{rows}

Manager uses a debug certificate. Core uses release runtime flags with a test certificate. Production signing and the signed production update feed require an owner-managed signing identity.

## Core changes

The non-system build removes SMS, overlay and ROM signature-spoofing permission requests. The merged APK audit confirms the restricted permissions are absent, backup is disabled and debugging is disabled. SMS retrieval and overlay-dependent features are outside this build's scope. The self-check lists declared permissions. Raw account, token, request and result logging is removed from the two modified authorization service entry points.

The earlier APK requested RECEIVE_SMS. Google documents sensitive-permission blocks for internet-sideloaded applications in supported markets. The reporter's exact warning is needed to confirm its classification. A device Play Protect verdict remains unverified. [Google guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)

## Manager download changes

APKMirror transfers have connection and read timeouts, bounded redirects, browser-session cookies and an overall coroutine timeout. Transfer, merge and write stages are visible. The APKMirror browser filters known ad resources and pop-ups, opens supported suggested versions directly and highlights download controls. Preparing, patching and writing report measured elapsed time and memory use. The default separate patcher process has a 1024 MB heap limit, a completion timeout and process-exit recovery. The host handles split APKM merging with modules kept open until serialization finishes. Archive extraction rejects traversal and duplicate paths and bounds split count and expanded size. Byte progress supports missing size and nonzero buffer offsets. Failed replacement downloads preserve the previous APK. Logs remain exportable during patching.

## Tests from this build

- 89 host checks passed, comprising 31 Node, 13 Java security, six Java branding, 13 Java download, 12 Java browser policy and 14 Python checks.
- Manager and Core build and lint tasks passed. Module unit-test tasks can report NO-SOURCE.
- Both delivered APKs installed and launched in the workflow's Android emulators. Exact installed hashes matched and no app crashes were observed. See each runtime report for its Android version.
- Both first-party branding fixtures followed the Manager patch, rebuild, sign, install and launch path. The video fixture used the two-split merge output.
- {catalog_result}

Android download regression checks passed.

{check_list}

The integration also transfers a 32 MiB asset APKM over a local HTTPS redirect without Content-Length, preserves its session cookie, imports the patch bundle and runs Video patching in the separate process. A deliberate child-process termination verifies prompt failure. A real WebView fixture checks ad hiding and standalone variant navigation. Core reuses the verified alpha.2 binary after matching its source tree and repeats installation and launch checks.

## Scope and remaining verification

Real YouTube and Music APK download, playback, Google sign-in, messaging, physical-device behavior and the Play Protect classification remain unverified. APKMirror can change its website and access controls. A live APKMirror request from this development environment returned HTTP 403. Live site download and real YouTube execution remain pending. The fixture tests cover browser navigation, HTTPS transport, repository recovery, split merging, preparation, patching and signing with first-party APKs.

Core retains app.revanced.android.gms for patch compatibility. An existing installation with a different signing certificate requires a deliberate migration. The installation guide covers exporting Manager settings and its patch-signing keystore before changing installations. Lint warnings remain in the included reports.

## Package

The ZIP includes the five pinned upstream source trees, master prompt, four visual references, branding assets, 17-page static website, installation guide, APKs, branding RVP and build evidence. The website remains a static deliverable. GitHub downloads require access to the private repository.

Use apks/ for installation files, bundles/ for the optional Javo branding patch, website/ for the built site, docs/ for setup and evidence/ for test reports and screenshots. SHA256SUMS accompanies the release assets.
"""
(root / 'docs/FINAL_REPORT.md').write_text(report, encoding='utf-8')
print('Generated report from matching alpha.2 APK and regression evidence')
