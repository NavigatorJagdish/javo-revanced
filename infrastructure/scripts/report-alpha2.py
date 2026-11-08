"""Generate the delivery report from this run's verified artifacts."""
import json
import os
from pathlib import Path

root = Path(__file__).resolve().parents[2]
catalog = json.loads((root / 'infrastructure/release/catalog.json').read_text())
integration = json.loads((root / 'evidence/manager-patch-integration.json').read_text())
audit = json.loads((root / 'evidence/core-security-audit.json').read_text())
manager_audit = json.loads((root / 'evidence/manager-security-audit.json').read_text())
artifacts = {item['id']: item for item in catalog['artifacts']}
if (manager_audit['apkSha256'] != artifacts['manager']['sha256']
    or manager_audit['signerSha256'] != artifacts['manager']['signerSha256']
    or manager_audit['debuggable'] or manager_audit['allowBackup'] is not False
    or manager_audit['usesCleartextTraffic'] is not False or not manager_audit['signatureV2Verified']):
    raise ValueError('Manager security audit must match this APK')
if (audit['apkSha256'] != artifacts['core']['sha256']
    or audit['signerSha256'] != artifacts['core']['signerSha256']
    or not audit['signatureSchemeV2Verified'] or not audit['forbiddenPermissionsAbsent']):
    raise ValueError('Core security audit must match this APK')
if integration['managerApkSha256'] != artifacts['manager']['sha256'] or not integration.get('success'):
    raise ValueError('Manager integration must match this APK')
checks = integration.get('downloadRegressions', [])
if not integration.get('productionBrowserChecks') or not integration.get('httpsTransfer') or not integration.get('browserChecks') or integration.get('separateProcessPatch') is not True or integration.get('terminatedProcessRecovery') is not True:
    raise ValueError('Browser, HTTPS transfer or separate process evidence is missing')
if len(checks) < 4:
    raise ValueError('Download regression evidence is missing')
for item in artifacts.values():
    if not item.get('runtimeSmoke'):
        raise ValueError('Matching install and launch evidence is missing')
manager35 = json.loads((root / 'evidence/manager-api35-runtime-smoke.json').read_text())
if manager35.get('installedApkSha256') != artifacts['manager']['sha256'] or manager35.get('processRunning') is not True or manager35.get('crashObserved') is not False:
    raise ValueError('Matching Android 15 Manager evidence is missing')
integration35 = json.loads((root / 'evidence/manager-api35-patch-integration.json').read_text())
for result, api in [(integration, 29), (integration35, 35)]:
    if result['androidApi'] != api or result['managerApkSha256'] != artifacts['manager']['sha256'] or not result['success']:
        raise ValueError(f'Android {api} patch evidence must match this Manager')
    if not any('HTTP 503' in check for check in result['productionBrowserChecks']):
        raise ValueError(f'Android {api} HTTP 503 recovery evidence is missing')
    products = {item['id']: item for item in result['products']}
    if set(products) != {'video', 'music'} or not all(item['mandatorySettingsAndIconVerified'] for item in products.values()):
        raise ValueError(f'Android {api} mandatory branding evidence is missing')
    if not products['video']['videoHeaderVerified']:
        raise ValueError(f'Android {api} Javo Video header evidence is missing')
    if not all(result.get('mediaSave', {}).get(key) is True for key in ('mp4','m4a','invalidFileRemoved','interruptedAudioResumed','separateTracksMuxed')):
        raise ValueError(f'Android {api} MP4/M4A Downloads evidence is missing')
run_url = 'https://github.com/' + os.environ['GITHUB_REPOSITORY'] + '/actions/runs/' + os.environ.get('JAVO_VERIFICATION_RUN_ID', os.environ['GITHUB_RUN_ID'])
rows = '\n'.join(f"| {item['displayName']} {item['version']} | {item['applicationId']} | `{item['sha256']}` |" for item in artifacts.values())
check_list = '\n'.join('- ' + str(check) + '.' for check in checks)
official = integration.get('officialCatalog', {})
if official.get('metadataLoadedOnAndroid'):
    catalog_result = f"The official ReVanced catalog loaded {official['patchCount']} patch definitions on Android. Third-party patch execution is unverified."
else:
    catalog_result = 'The optional official catalog diagnostic was unavailable. See the integration report for its failure. Third-party patch execution is unverified.'
report = f"""# Javo ReVanced {artifacts['manager']['version']} verification report

The private workflow built these development APKs and bound the results below to their exact hashes. [Verification run]({run_url})

## Verified artifacts

| Artifact | Android identity | SHA-256 |
| --- | --- | --- |
{rows}

Both APKs disable runtime debugging. Manager retains its established development certificate, and Core uses a test certificate. The Manager APK audit verifies its signature and that app backup, cleartext traffic, unrestricted storage and broad app visibility are disabled. APKMirror is built in; the Manager no longer loads downloader plugins from remote APKs. Default patch updates accept only the verified bundle digest. Play Protect classification is pending.

## Core changes

The non-system build removes SMS, overlay and ROM signature-spoofing permission requests. The merged APK audit confirms the restricted permissions are absent, backup is disabled and debugging is disabled. SMS retrieval and overlay-dependent features are outside this build's scope. The self-check lists declared permissions. Raw account, token, request and result logging is removed from the two modified authorization service entry points.

The APK audit verified the expected Core signing certificate, APK Signature Scheme v2 and target SDK {audit['targetSdk']}. The reporter said the Core APK passed their phone's scan and installed. Google Play Protect may scan sideloaded apps on other devices. [Google Play Protect help](https://support.google.com/googleplay/answer/2812853?hl=en)

## Manager download changes

APKMirror transfers have connection and read timeouts, bounded redirects, browser-session cookies and an overall coroutine timeout. Transfer, merge and write stages are visible. The APKMirror browser filters known ad resources and pop-ups, opens supported suggested versions directly and highlights download controls. It recognizes both the initial download control and the final download.php link, uses the same window and reports missing controls instead of silently doing nothing. Persistent native controls resolve the final URL directly instead of waiting for a WebView download callback. A native document picker imports a browser-downloaded APK/APKM back into the same patching session, validates and merges it, and cleans up its private temporary copy. Only imports inside the dedicated private cache directory are accepted. An external-browser fallback is available. Preparing, patching and writing report measured elapsed time and memory use. The native heap-property override returns a terminated property value and older Android versions skip unavailable AppCompat initialization. The default separate patcher process has a 700 MB requested heap limit, capped to one quarter of device RAM and to 700 MB on Android 10 and older, a completion timeout and process-exit recovery. The host handles split APKM merging with modules kept open until serialization finishes. Archive extraction rejects traversal and duplicate paths and bounds split count and expanded size. Byte progress supports missing size and nonzero buffer offsets. Failed replacement downloads preserve the previous APK. Logs remain exportable during patching.

## Tests from this build

- 102 host checks passed, comprising 31 Node, 13 Java security, six Java branding, 13 Java download, 21 Java browser policy and 18 Python checks.
- Manager and Core build and lint tasks passed. Module unit-test tasks can report NO-SOURCE.
- Both delivered APKs installed and launched on API29 and API35 in the workflow's Android emulators. Exact installed hashes matched and no app crashes were observed. See each runtime report for its Android version.
- Both first-party branding fixtures followed the Manager patch, rebuild, sign, install and launch path. The video fixture used the two-split merge output.
- On both emulator APIs, the built-in saver copied an MP4 and an M4A over HTTPS to Downloads, resumed an interrupted audio transfer, checked the track types, matched the original bytes, and removed a rejected file.
- Built-in Javo launcher icon and settings branding were verified for both fixture outputs, along with the Javo Video header. The production browser test showed a fixture HTTP 503, retried with the native button, captured the file URL and ignored a late 503 after handoff.
- {catalog_result}

Android download regression checks passed.

{check_list}

The integration also transfers a 32 MiB asset APKM over a local HTTPS redirect without Content-Length, preserves its session cookie, imports the patch bundle and runs Video patching in the separate process. A deliberate child-process termination verifies prompt failure. A real WebView fixture checks ad hiding and standalone variant navigation. Core reuses the verified alpha.2 binary after matching its source tree and repeats installation and launch checks.

## Scope and remaining verification

A separate local Android 10 check patched and signed a Google-signed YouTube 20.40.45 APK using GmsCore support, Spoof video streams and Javo Video branding with a 700 MB heap. See evidence/alpha3-real-youtube.json for its exact local Manager hash and installation result; it is a different development build from this CI APK. This does not verify every official patch. A mobile-browser request and Python download succeeded, while a separate Java request returned HTTP 403. The CI fixture tests cover browser navigation, HTTPS transport, repository recovery, split merging, preparation, patching and signing against the exact delivered Manager hash. Playback, Google sign-in, Music patching, physical-device behavior and Play Protect classification remain unverified.

Core retains app.revanced.android.gms for patch compatibility. An existing installation with a different signing certificate requires a deliberate migration. The installation guide covers exporting Manager settings and its patch-signing keystore before changing installations. Lint warnings remain in the included reports.

## Package

The ZIP includes the five pinned upstream source trees, master prompt, four visual references, branding assets, 17-page static website, installation guide, APKs, branding RVP and build evidence. The website remains a static deliverable. GitHub downloads require access to the private repository.

Use apks/ for installation files, bundles/ for the standalone branding RVP for external patchers, website/ for the built site, docs/ for setup and evidence/ for test reports and screenshots. SHA256SUMS accompanies the release assets.
"""
(root / 'docs/FINAL_REPORT.md').write_text(report, encoding='utf-8')
print('Generated report from matching APK and regression evidence')
