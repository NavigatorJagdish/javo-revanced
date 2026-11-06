# JAVO REVANCED

Get the complete ZIP, Manager APK, Core APK, and branding bundle from the [private GitHub Releases page](https://github.com/NavigatorJagdish/javo-revanced/releases). Sign in with an account that has access to this repository.

Follow the [installation and setup guide (Hinglish)](docs/INSTALLATION.md) to install the APKs, import the branding bundle, and troubleshoot common setup issues.

**Patch. Manage. Customize. Control.**

Independent Android ecosystem regenerated from the owner's master prompt and four design references, with verified alpha.5 Manager update published on 2026-11-06. This is a **functional alpha**. See [the implementation report](docs/FINAL_REPORT.md) for actual build and test outcomes.

Alpha.5 includes APKMirror ad protection, direct suggested-version navigation, browser session/source-page headers, measured preparation progress and patcher-process exit recovery. The Core binary is reused from verified alpha.2 after matching its source tree. The APKMirror toolbar recognizes both first-page and final download.php links, handles download controls in the same window and offers an external-browser fallback. The heap defaults to 700 MB, with an Android 10 cap, after a separate real YouTube patch/sign check. See the evidence for the local build hash and tested patches. Physical-device behavior, playback and account sign-in remain unverified.

## Included

- `apps/javo-manager`: complete pinned real ReVanced Manager source, Javo identity, neon dark palette, and isolated signed-update verification source. The upstream patch/sign/install architecture is retained. Private CI compiled and linted Manager. The workflow checks the packaged APK through browser, HTTPS transfer and first-party patch/sign/install integration. See the generated final report for each release's verified outcomes.
- `apps/javo-core`: complete real ReVanced GmsCore source with Javo branding while retaining `app.revanced.android.gms` and service contracts. See evidence for the actual build result.
- `patches/javo-branding`: opt-in Video/Music label and icon resource patch source. The Kotlin bundle and DEX archive compile. Resource transformations and external-entity rejection were checked on Android; both first-party fixtures passed patching, signing, installation and launch.
- `apps/javo-website`: 17 static pages, responsive navigation, persisted theme, honest artifact inventory, documentation and security information. No installation dependencies are required.
- `assets`: all four original reference images, scalable Javo logos and Android vector/monochrome icons.
- `infrastructure`: real APK verification, release envelope signing, artifact packaging, CI and smoke-test scripts.
- `evidence`: actual logs, test results, screenshots and source provenance. Concept references are never presented as screenshots of a running app.

## Website

Node.js 20+:

```sh
node --test tests/*.test.mjs
node apps/javo-website/build.mjs
node apps/javo-website/server.mjs
```

Open http://127.0.0.1:4173 locally. The source package includes the static build under `website/`; serve that directory with any static HTTP server. No external font, analytics or tracking service is used. A real deployment origin can be supplied as `SITE_ORIGIN` when building to emit an absolute sitemap. No site has been published.

## Android

Read [BUILDING.md](docs/BUILDING.md), [upstream compatibility](docs/UPSTREAM_COMPATIBILITY.md), and [verification limits](docs/FINAL_REPORT.md). Never label a debug-signed APK as a production release. No proprietary Video or Music APK is included.

GitHub blocks the original patches repository, but the official API serves bundle 6.2.1 and links to a public GitLab source repository. The bundle signature was checked; 294 catalog entries loaded on Android. A separate local Android 10 check applied GmsCore support, Spoof video streams and Javo branding to YouTube 20.40.45 and signed the result. The final report distinguishes this local build from CI fixtures. Playback remains unverified. A pinned GitLab source snapshot is included separately and is not claimed to match that bundle byte-for-byte. The exact packaged Core APK installed and launched without observed crashes on API29 and API35 x86_64 emulators. Google account authentication, streaming playback, notifications and physical-device compatibility remain unverified.

## Licenses and independence

Manager and Javo code: GPL-3.0-only unless a file states otherwise. GmsCore: Apache-2.0 with additional upstream component notices. Full upstream licenses and source are preserved. See [LICENSING.md](docs/LICENSING.md). Javo is not affiliated with Google, YouTube, ReVanced or microG.
