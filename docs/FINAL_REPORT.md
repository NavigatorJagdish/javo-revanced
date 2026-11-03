# Javo ReVanced — implementation and verification report

Date: 2026-11-03. **Production readiness: development prototype.**

The project was regenerated with the owner's permission because the previous ZIP could not be downloaded. The master prompt and all four reference images were recovered. This package includes a real built, signed and emulator-tested Javo Core APK, complete Android source forks, a working website, branding assets, patch adapter source, CI and evidence. It does **not** include a built Javo Manager APK or verified Video/Music patch execution.

## A. Completed features

- Complete pinned ReVanced Manager source imported with licenses. Javo display/package/deep-link identity, vector icons and default dark palette applied. Upstream APK import, metadata selection, patching, managed signing, progress, logs, backup/settings and installation architecture retained in source. These Manager Android changes have not compiled locally.
- Complete real ReVanced GmsCore source imported, branded as Javo Core / Javo Core Settings, with API19-compatible PNG fallbacks and adaptive launcher artwork. Internal app.revanced.android.gms identity and service contracts retained.
- Real Javo Core debug APK built, verified by official Android tools, installed and launched on an API29 x86_64 emulator. The installed APK hash matches the delivered artifact. No process crash was observed in the smoke check.
- Dependency-free responsive website: all 16 requested pages plus 404, mobile navigation, persisted theme, real screenshot filters/lightbox, documentation and downloads generated from verified local artifact metadata. No site has been deployed.
- Four scalable product logos, monochrome variants, 16 transparent PNG icon exports, wordmark, Android adaptive icon sets, Manager splash artwork and shared design tokens.
- Javo Video/Music resource branding adapter source, preserving original and recognized GmsCore-renamed package identities. Actual Java XML transformations tested; Kotlin compilation and Android RVP execution remain unverified.
- Javo Manager update verification source: isolated configurable feed, signed manifest verification, expiry/package/version/ABI checks, file checksum and installed-signing-identity checks before installation. Host crypto primitives tested. No production update feed, key or Android integration verification exists.
- APK verification, signed-manifest tooling, source inventory, packaging verification, build/test scripts and GitHub CI workflows. CI is provided but has not been run remotely.

## B. Build results

| Build/check | Actual result |
| --- | --- |
| Initial Core assembleVtmDefaultDebug | Passed, 27m14s; 1557 actionable tasks |
| Final Core assemble + unit-test task + lint | Passed, 16m57s; 2914 actionable tasks |
| Core lint report | 0 errors, 536 warnings; reports included rather than suppressed |
| Core unit-test task | NO-SOURCE; no Android module unit tests are claimed passed |
| Manager assembleDebug / testDebugUnitTest | Failed before compilation: missing GitHub Packages credentials |
| Exact Patcher POM access | HTTP 401 without credentials |
| Website production generation | Passed: 17 pages and real Core APK download |
| Javo branding Kotlin/RVP build | Not executed successfully; requires the same protected Patcher packages |

Core output: `deliverables/apks/javo-core-0.3.13.2.250932-debug.apk` in the workspace. The main ZIP also contains the actual APK under `apks/` and the static website download directory. This is a debug-signed test artifact, not a production release.

## C. Tests and artifact verification

- 31 Node website/release tests passed.
- 13 host Java security checks passed.
- 4 host Java branding resource checks passed.
- **48 host checks total.** These do not stand in for Manager compilation or end-to-end patching.
- Official apksigner: valid v1 and v2 signature, one Android Debug RSA signer. No production signing claim.
- Package: app.revanced.android.gms. Version: 0.3.13.2.250932. Version code: 250932004. Manifest minimum SDK: 19. Native ABIs present: arm64-v8a, armeabi-v7a, x86, x86_64. Only API29 x86_64 was smoke-tested.
- APK SHA-256: `f589479fc818f7bb24119f67c3f60c7604a3a0427ce37c7b37d811efdece441a`.
- Browser checked at 320, 375, 768, 1024 and 1440 pixels: no horizontal overflow in the recorded measurements. Mobile menu, theme and screenshot gallery are checked separately in the browser evidence.
- Official API patch bundle 6.2.1 downloaded for inspection; detached signature verified against the key supplied over ReVanced HTTPS. Fingerprint: A7835DFCACA14BDA3CD6EDD3633C6920FBE6A2FF. PGPy warns that its self-signature, revocation and key-flag checks are incomplete. No independent out-of-band key ownership verification or patch execution is claimed. Third-party RVP bytes are not redistributed.

## D. Real screenshots and evidence

- evidence/screenshots/core-emulator.png: actual Javo Core settings on Android 10. The visible registration/messaging toggles are upstream settings state, not proof of successful service registration or delivery.
- evidence/screenshots/website-desktop.png: actual browser capture.
- evidence/screenshots/website-mobile-menu.png: actual phone-width browser capture.
- evidence/core-runtime-smoke.json and core-emulator-*.txt/xml: installation, launcher, process and crash observations.
- evidence/core-build.log, core-first-build-summary.txt and core-reports/: build and lint evidence.
- evidence/node-tests.log, java-security-tests.log, java-branding-tests.log: actual host results.

## E. Pinned upstreams

| Source | Commit |
| --- | --- |
| Manager v2.6.0 | aed92b22d3efa91bf46ec6b9d7a8d470b22abeee |
| ReVanced GmsCore v0.3.13.2.250932 | 80bfe22cfaf0de84b6c1e96fe3435877d3b9e08c |
| Patcher 22.0.2-dev.1 | d7f12ab77c205f9223169393714e751c85ad5e76 |
| Library 4.0.0 | 299c63399c77f207291042a1abc3ba55374a2c64 |
| Official temporary GitLab patches source | 015fe10a23ea5b919a3fda6e7da2c176517ae75d |

The GitLab source declares 6.1.0; the official API bundle declares 6.2.1. They are not asserted to be matching reproducible artifacts. GitHub blocks the original patch repository, but it is incorrect to conclude the official patch service is unavailable: its API and GitLab source were recovered. See UPSTREAM_COMPATIBILITY.md and evidence/upstream-lock.json.

## F. Branding fidelity

Manager uses the reference's angular play/ring/wrench geometry and purple/pink/blue identity. Video, Music and Core retain their distinct silhouettes and color families. The website follows the near-black surfaces, neon accents and product-card hierarchy. Artwork is a clean vector interpretation, not a pixel-perfect 3D recreation.

The Manager's complete five-screen reference redesign and a custom Core diagnostic dashboard have not been implemented. Manager retains its upstream layout with a Javo skin. Core retains its real upstream settings layout; the captured emulator uses the system's light appearance. No screenshot is fabricated to conceal these differences. Video/Music UI replacement is not claimed.

## G. Known limitations

- Manager's Javo source cannot be compiled here without GitHub Packages read access. Its new updater and other Android changes are consequently unverified.
- No user-supplied proprietary source APK exists. Video/Music patching, repatching, signing, installation, streaming and account integration have not been exercised.
- No Google account was supplied or entered. Core sign-in, notification delivery, device registration, attestation and real-device battery behavior remain unverified.
- Android 16 / ARM64 operation is unverified, despite an ARM64 library payload being present in the APK.
- Debug signing cannot update an existing differently signed GmsCore. A production signing identity and safe release process remain owner-controlled requirements.
- No hosted release, signed update feed, automatic Core updater, complete dependency vulnerability audit or hosted CI run is claimed.
- Custom branding patches do not automatically resolve conflicts with competing branding patches. Their Android bundle loading remains untested.

## H. External requirements

To finish Manager, configure owner-controlled GitHub Packages read access locally or run the supplied CI in an authorized repository with package access. Do not paste a token into source or this report. Then compile, lint and run Manager, branding bundle and patch execution tests using lawful source APKs. Production publication/signing and account integration require the owner's authorization/identity. No external account has been modified.

## I. Deliverable paths

- `deliverables/javo-revanced-development.zip`: source, website build, verified Core APK, assets, licenses and evidence.
- `deliverables/apks/javo-core-0.3.13.2.250932-debug.apk`: directly installable development APK.
- `deliverables/SHA256SUMS` and `deliverables/apks/SHA256SUMS`: archive/APK checksums.
- `apps/javo-website/dist/`: local generated website; `website/` inside ZIP.
- `evidence/source-inventory.json`: hashes of archived source/build/evidence files. Packaging rechecks ZIP CRC and content hashes.

## J. Readiness decision

**Development prototype.** Core has real build, signature and limited emulator evidence. The complete ecosystem is not finished because Manager, patch execution and account-dependent integration have not met acceptance criteria. The package deliberately does not fabricate those deliverables.
