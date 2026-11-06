# Javo ReVanced alpha.6 verification report

The private workflow built these development APKs and bound the results below to their exact hashes. [Verification run](https://github.com/NavigatorJagdish/javo-revanced/actions/runs/42152158095)

## Verified artifacts

| Artifact | Android identity | SHA-256 |
| --- | --- | --- |
| Javo Manager 0.1.0-alpha.6 | app.javo.manager | `fcec4cf8210fb1334fb90949f2213a4d2056ecabdda2819678bd5317423d5a13` |
| Javo Core 0.3.13.2.250932-javo.2 | app.revanced.android.gms | `4316c1a5778e66fadd7d3d28fe689a2d74fb7c601ec36504998622cd78aaa55f` |

Manager uses a debug certificate. Core uses release runtime flags with a test certificate. Production signing and the signed production update feed require an owner-managed signing identity.

## Core changes

The non-system build removes SMS, overlay and ROM signature-spoofing permission requests. The merged APK audit confirms the restricted permissions are absent, backup is disabled and debugging is disabled. SMS retrieval and overlay-dependent features are outside this build's scope. The self-check lists declared permissions. Raw account, token, request and result logging is removed from the two modified authorization service entry points.

The earlier APK requested RECEIVE_SMS. Google documents sensitive-permission blocks for internet-sideloaded applications in supported markets. The reporter's exact warning is needed to confirm its classification. A device Play Protect verdict remains unverified. [Google guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)

## Manager download changes

APKMirror transfers have connection and read timeouts, bounded redirects, browser-session cookies and an overall coroutine timeout. Transfer, merge and write stages are visible. The APKMirror browser filters known ad resources and pop-ups, opens supported suggested versions directly and highlights download controls. It recognizes both the initial download control and the final download.php link, uses the same window and reports missing controls instead of silently doing nothing. Persistent native controls resolve the final URL directly instead of waiting for a WebView download callback. A native document picker imports a browser-downloaded APK/APKM back into the same patching session, validates and merges it, and cleans up its private temporary copy. Only imports inside the dedicated private cache directory are accepted. An external-browser fallback is available. Preparing, patching and writing report measured elapsed time and memory use. The native heap-property override returns a terminated property value and older Android versions skip unavailable AppCompat initialization. The default separate patcher process has a 700 MB requested heap limit, capped to one quarter of device RAM and to 700 MB on Android 10 and older, a completion timeout and process-exit recovery. The host handles split APKM merging with modules kept open until serialization finishes. Archive extraction rejects traversal and duplicate paths and bounds split count and expanded size. Byte progress supports missing size and nonzero buffer offsets. Failed replacement downloads preserve the previous APK. Logs remain exportable during patching.

## Tests from this build

- 102 host checks passed, comprising 31 Node, 13 Java security, six Java branding, 13 Java download, 21 Java browser policy and 18 Python checks.
- Manager and Core build and lint tasks passed. Module unit-test tasks can report NO-SOURCE.
- Both delivered APKs installed and launched on API29 and API35 in the workflow's Android emulators. Exact installed hashes matched and no app crashes were observed. See each runtime report for its Android version.
- Both first-party branding fixtures followed the Manager patch, rebuild, sign, install and launch path. The video fixture used the two-split merge output.
- The official ReVanced catalog loaded 294 patch definitions on Android. This CI fixture run does not execute official patches; see the separate local real-APK check below.

Android download regression checks passed.

- unknown size and nonzero buffer offset complete with exact byte count.
- stalled plugin fails and preserves the previous valid APK.
- two-split APKM serializes before resources close and preserves split resources.
- archive path traversal rejected.

The integration also transfers a 32 MiB asset APKM over a local HTTPS redirect without Content-Length, preserves its session cookie, imports the patch bundle and runs Video patching in the separate process. A deliberate child-process termination verifies prompt failure. A real WebView fixture checks ad hiding and standalone variant navigation. Core reuses the verified alpha.2 binary after matching its source tree and repeats installation and launch checks.

## Scope and remaining verification

A separate local Android 10 check patched and signed a Google-signed YouTube 20.40.45 APK using GmsCore support, Spoof video streams and Javo Video branding with a 700 MB heap. See evidence/alpha3-real-youtube.json for its exact local Manager hash and installation result; it is a different development build from this CI APK. This does not verify every official patch. Before the alpha.6 redirect/host fix, a separate Java request to the protected endpoint returned HTTP 403. The alpha.6 local production-screen check subsequently completed the entire live download through the resolved R2 URL; see the final regression section below. The CI fixture tests cover browser navigation, HTTPS transport, repository recovery, split merging, preparation, patching and signing against the exact delivered Manager hash. Playback, Google sign-in, Music patching, physical-device behavior and Play Protect classification remain unverified.

Core retains app.revanced.android.gms for patch compatibility. An existing installation with a different signing certificate requires a deliberate migration. The installation guide covers exporting Manager settings and its patch-signing keystore before changing installations. Lint warnings remain in the included reports.

## Package

The ZIP includes the five pinned upstream source trees, master prompt, four visual references, branding assets, 17-page static website, installation guide, APKs, branding RVP and build evidence. The website remains a static deliverable. GitHub downloads require access to the private repository.

Use apks/ for installation files, bundles/ for the optional Javo branding patch, website/ for the built site, docs/ for setup and evidence/ for test reports and screenshots. SHA256SUMS accompanies the release assets.


## Final download-button regression

The alpha.6 production Manager screen resolved the protected download endpoint, captured its trusted R2 file URL, and imported a document-picker APKM back into the repository on Android 10 and 15. The delivered Manager hash matched the APK installed in both CI environments. The local live test downloaded YouTube 20.40.45 completely: HTTP 200, 169541734 bytes, SHA-256 b7659da492a1ebd8bd7cea2909be4ee1f58e00a2586d65a1c91b2e1e5ec6acd1. See evidence/alpha6-live-mirror.json for its local build hash and scope. All six jobs passed. The complete release ZIP and its source inventory were independently rehashed locally; see evidence/release-alpha6.json and evidence/provenance-alpha6.json.

The separate local real YouTube check used a different development APK, applied three named patches and installed the signed output. Main navigation opened after granting the patched app its declared account-discovery permission and exempting Core from battery optimization. The observed screen is below. Feed playback and account login were not tested.

![Local patched YouTube main navigation on API29](../evidence/screenshots/real-youtube-api29.png)


## Development signing continuity

The release Manager certificate matches the private development-identity backup saved for the owner. Future CI builds restore it from the protected cache or private Actions artifact, compare it with the pinned alpha.6 certificate, and fail rather than silently rotate a missing identity. Signing material is excluded from source ZIPs and release assets. This remains a development certificate; production signing and a hosted signed-update feed are separate owner configuration.
