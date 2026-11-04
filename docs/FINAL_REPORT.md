# Javo ReVanced verification report

**Functional alpha.** Manager and Core are debug-signed development APKs. The source package includes all five pinned upstream source trees, the master prompt, four visual references, branding assets, the 17-page static website, the verified branding bundle, build instructions, and actual test evidence.

## A. Implemented features

Manager retains upstream APK import, patch selection, patch execution, signing, installation and logs. It adds the Javo identity, dark palette, a live patch count, a circular progress display driven by completed steps, and an isolated signed update verifier. Core retains the upstream GmsCore service and package identities with Javo branding. The opt-in branding bundle changes Video and Music launcher names and icons. The website provides documentation and downloads backed by verified local artifacts.

## B. Builds

All five jobs passed in the [private verification run](https://github.com/NavigatorJagdish/javo-revanced/actions/runs/41637751030) at commit `9c8cdc283dc64a95a6fd9bf62d45a71babd14f46`. The package job reverified both APKs and checked the ZIP content hashes. Windows support also includes a Clang driver fix for short executable names and an escaped Android SDK path. The delivered APKs are the Linux CI outputs.

| Artifact | Identity | SHA-256 |
| --- | --- | --- |
| Manager 0.1.0-alpha.1 | app.javo.manager | `96041d7de29aef3fadc35ab11ed5d60793c95123e31a06bf0b6369d87f69659a` |
| Core 0.3.13.2.250932 | app.revanced.android.gms | `07eeaf00757e778e7d155bfad977a20efdb873905051712b91905a9817155449` |
| Javo branding RVP 0.1.0-alpha.1 | Video and Music resources | `629aa5aadf241c980492204e9c6ca067c3ba3605ec703e9c8dca3801dec5ccd9` |

## C. Tests

- 64 host checks passed: 31 Node, 13 Java security, six Java branding and 14 Python checks.
- Manager and Core passed hosted Android lint. Their module unit-test tasks reported NO-SOURCE.
- The exact delivered Manager APK installed and launched on Android API35 x86_64. The exact delivered Core APK installed and launched on API29 x86_64. Installed hashes matched and no app crashes were observed.
- Manager loaded the RVP, applied both branding definitions to first-party test APKs, rebuilt and signed them, and verified both signed fixtures installed and launched. The integration report binds the result to the exact Manager APK and RVP hashes.
- The signature-inspected official ReVanced catalog 6.2.1 loaded 294 patch definitions on Android. This test read catalog metadata. The fixture APKs exercised Javo branding exclusively.

## D. Screenshots

Actual emulator captures and development browser captures are under `evidence/screenshots/`. Fixture screenshots show the project test apps. The four concept images are separately preserved under `assets/references/`.

## E. Dependencies

The archive contains Manager v2.6.0, GmsCore v0.3.13.2.250932, Patcher v22.0.2-dev.1, Library v4.0.0 and the GitLab patch source snapshot declaring 6.1.0. See `evidence/upstream-lock.json` for commit IDs and source archive checksums. The official 6.2.1 binary and GitLab source snapshot have separate provenance.

## F. Branding and website

The references informed the neon palette, vector marks, dashboard and progress treatment. Manager retains the upstream app structure and Core retains the upstream settings layout. The site uses a dependency-free static generator and includes the 16 requested pages plus a 404 page. Earlier browser captures document development layouts. Final routes and download links passed the automated website checks.

## G. Limits

Real YouTube and YouTube Music APKs, playback, Google sign-in, registration, notifications and physical-device behavior remain untested. Production signing and the production update feed are unconfigured. Core shares its package name with ReVanced GmsCore, so Android requires matching signing certificates for in-place updates. Partial translations fall back to English. Lint warnings remain visible in the reports.

## H. External requirements

Real-app integration requires compatible, authorized source APKs and any account credentials entered directly on the test device. Production distribution requires a signing identity, a signed feed, and owner approval for publication.

## I. Package contents

Use `apks/` for the two debug APKs, `bundles/` for the Javo branding RVP, `website/` for the built site, `docs/` for instructions, and `evidence/` for reports and file hashes. The site can be served from any static HTTP server. The final delivery ZIP has a separate SHA-256 checksum.

## J. Readiness

Functional alpha for the tested Manager, Core launch and first-party branding workflow. The repository and CI artifacts are private.
