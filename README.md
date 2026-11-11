# Javo ReVanced for Android

[**Public Javo website**][website] · [**Security and release status**](SECURITY.md)

**[Download Core javo.6](https://github.com/NavigatorJagdish/javo-revanced/releases/download/core-v0.3.13.2.250932-javo.6/javo-core-0.3.13.2.250932-javo.6-production.apk)** · [Setup guide (Hinglish)](docs/INSTALLATION.md) · [Verification limits](docs/VERIFICATION-STATUS.md)

Manager alpha.13 remains withheld after a harmful Play Protect indication on the owner's phone. The owner reported Core javo.6 safe on that phone; this is not clearance for every device. Keep Play Protect enabled. Three advisory matches in two legacy Core libraries remain open; see the [dependency review](docs/DEPENDENCY-REVIEW.md).

**Patch. Manage. Customize. Control.**

Javo ReVanced is an independent Android APK patching project based on ReVanced Manager and GmsCore. Use Javo Manager to customize compatible YouTube and YouTube Music APKs locally. This is a **functional alpha**. See [the installation guide](docs/INSTALLATION.md) for the current release and [the implementation report](docs/FINAL_REPORT.md) for test scope.

Alpha.13 repairs interrupted framework caches before parallel APK decoding and retains a 20-minute patch/save window after preparation. Core javo.6 includes tracked upstream security backports and updated runtime dependencies. Read the [Core release notes](docs/RELEASE_CORE6.md) and current verification limits before installing.

[website]: https://navigatorjagdish.github.io/javo-revanced/


## Included

- `apps/javo-manager`: complete pinned real ReVanced Manager source, Javo identity, neon dark palette, and isolated signed-update verification source. The upstream patch/sign/install architecture is retained. Private CI compiled and linted Manager. The workflow checks the packaged APK through browser, HTTPS transfer and first-party patch/sign/install integration. See the generated final report for each release's verified outcomes.
- `apps/javo-core`: complete real ReVanced GmsCore source with Javo branding while retaining `app.revanced.android.gms` and service contracts. See evidence for the actual build result.
- `patches/javo-branding`: Video/Music label, icon and interface resource transformations. Javo Manager applies its built-in Javo branding automatically for those targets; the standalone bundle is for external patchers. Both first-party fixtures passed patch/sign/install/launch on Android 10/15.
- `apps/javo-website`: 17 static pages, responsive navigation, persisted theme, honest artifact inventory, documentation and security information. No installation dependencies are required.
- `assets`: scalable Javo logos and Android vector/monochrome icons. Original project references are kept privately and excluded from public source packaging.
- `infrastructure`: real APK verification, release envelope signing, artifact packaging, CI and smoke-test scripts.
- `evidence`: sanitized release summaries and source provenance. Raw private logs, device screenshots and original inputs are excluded from public source packaging.

## Website

Node.js 20+:

```sh
node --test tests/*.test.mjs
node apps/javo-website/build.mjs
node apps/javo-website/server.mjs
```

Open http://127.0.0.1:4173 locally. The source package includes the static build under `website/`; serve that directory with any static HTTP server. No external font, analytics or tracking service is used. A real deployment origin can be supplied as `SITE_ORIGIN` when building to emit an absolute sitemap. The public site is published through GitHub Pages. Its sitemap is available at the website link above.

## Android

Read [BUILDING.md](docs/BUILDING.md), [upstream compatibility](docs/UPSTREAM_COMPATIBILITY.md), and [verification limits](docs/FINAL_REPORT.md). Never label a debug-signed APK as a production release. No proprietary Video or Music APK is included.

GitHub blocks the original patches repository, but the official API serves bundle 6.2.1 and links to a public GitLab source repository. The bundle signature was checked; 294 catalog entries loaded on Android. A separate local Android 10 check applied GmsCore support, Spoof video streams and Javo branding to YouTube 20.40.45 and signed the result. The final report distinguishes this local build from CI fixtures. Playback remains unverified. A pinned GitLab source snapshot is included separately and is not claimed to match that bundle byte-for-byte. The exact packaged Core APK installed and launched without observed crashes on API29 and API35 x86_64 emulators. Google account authentication, streaming playback, notifications and physical-device compatibility remain unverified.

## Licenses and independence

Manager and Javo code: GPL-3.0-only unless a file states otherwise. GmsCore: Apache-2.0 with additional upstream component notices. Full upstream licenses and source are preserved. See [LICENSING.md](docs/LICENSING.md). Javo is not affiliated with Google, YouTube, ReVanced or microG.
