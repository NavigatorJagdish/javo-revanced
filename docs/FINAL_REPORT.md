# Javo ReVanced alpha.2 verification report

The private workflow built these development APKs and bound the results below to their exact hashes. [Verification run](https://github.com/NavigatorJagdish/javo-revanced/actions/runs/42006539702)

## Verified artifacts

| Artifact | Android identity | SHA-256 |
| --- | --- | --- |
| Javo Manager 0.1.0-alpha.2 | app.javo.manager | `82f8434081250f1fd98994768903ea3f116e437ef7913b0bceba267e2dd5b28a` |
| Javo Core 0.3.13.2.250932-javo.2 | app.revanced.android.gms | `4316c1a5778e66fadd7d3d28fe689a2d74fb7c601ec36504998622cd78aaa55f` |

Manager uses a debug certificate. Core uses release runtime flags with a test certificate. Production signing and the signed production update feed require an owner-managed signing identity.

## Core changes

The non-system build removes SMS, overlay and ROM signature-spoofing permission requests. The merged APK audit confirms the restricted permissions are absent, backup is disabled and debugging is disabled. SMS retrieval and overlay-dependent features are outside this build's scope. The self-check lists declared permissions. Raw account, token, request and result logging is removed from the two modified authorization service entry points.

The earlier APK requested RECEIVE_SMS. Google documents sensitive-permission blocks for internet-sideloaded applications in supported markets. The reporter's exact warning is needed to confirm its classification. A device Play Protect verdict remains unverified. [Google guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)

## Manager download changes

APKMirror transfers have connection and read timeouts, bounded redirects, browser-session cookies and an overall coroutine timeout. Transfer, merge and write stages are visible. The host handles split APKM merging with modules kept open until serialization finishes. Archive extraction rejects traversal and duplicate paths and bounds split count and expanded size. Byte progress supports missing size and nonzero buffer offsets. Failed replacement downloads preserve the previous APK. Logs remain exportable during patching.

## Tests from this build

- 75 host checks passed, comprising 31 Node, 13 Java security, six Java branding, 11 Java download and 14 Python checks.
- Manager and Core build and lint tasks passed. Module unit-test tasks can report NO-SOURCE.
- Both delivered APKs installed and launched in the workflow's Android emulators. Exact installed hashes matched and no app crashes were observed. See each runtime report for its Android version.
- Both first-party branding fixtures followed the Manager patch, rebuild, sign, install and launch path. The video fixture used the two-split merge output.
- The official ReVanced catalog loaded 294 patch definitions on Android. Third-party patch execution is unverified.

Android download regression checks passed.

- unknown size and nonzero buffer offset complete with exact byte count.
- stalled plugin fails and preserves the previous valid APK.
- two-split APKM serializes before resources close and preserves split resources.
- archive path traversal rejected.

## Scope and remaining verification

Real YouTube and Music APK download, playback, Google sign-in, messaging, physical-device behavior and the Play Protect classification remain unverified. APKMirror can change its website and access controls. The fixture tests cover the transfer primitives, repository recovery and split-merging path with first-party APKs.

Core retains app.revanced.android.gms for patch compatibility. An existing installation with a different signing certificate requires a deliberate migration. The installation guide covers exporting Manager settings and its patch-signing keystore before changing installations. Lint warnings remain in the included reports.

## Package

The ZIP includes the five pinned upstream source trees, master prompt, four visual references, branding assets, 17-page static website, installation guide, APKs, branding RVP and build evidence. The website remains a static deliverable. GitHub downloads require access to the private repository.

Use apks/ for installation files, bundles/ for the optional Javo branding patch, website/ for the built site, docs/ for setup and evidence/ for test reports and screenshots. SHA256SUMS accompanies the release assets.

## Published delivery

The verified build commit is `624a2ae8974c9c9e93af76300e187694e6174eca`. All six workflow jobs passed. [Private alpha.2 release](https://github.com/NavigatorJagdish/javo-revanced/releases/tag/v0.1.0-alpha.2)

The downloaded package matched GitHub asset digests. All 7,206 source inventory entries were rehashed locally. The source ZIP SHA-256 is `258b44552a485fb2792b2f53571781affe27f4a0e89576b4e12a4b23722aca25`. See evidence/release-alpha2.json for the asset receipt.
