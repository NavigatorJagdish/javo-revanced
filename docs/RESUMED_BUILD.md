# Resumed build — 2026-11-03

The owner approved creation of a private `NavigatorJagdish/javo-revanced` repository, uploading the prepared source, and running GitHub Actions. The owner signed in directly to GitHub; no password or personal access token was supplied in chat.

Repository: https://github.com/NavigatorJagdish/javo-revanced (verified private).

The source-only archive uploaded successfully and was checksum-verified by the source-import job. It contains 7,088 files, excludes APKs and private signing keys, and has SHA-256 `83ccad4a8b5120e8d96519de6330d5f0c841728af881848fe79b59fb0c004479`. The import job committed the normal project directory structure to the private repository.

## Changes since the initial development package

- Corrected Manager's app-specific version override to `0.1.0-alpha.1`.
- Restricted the GitHub Maven repository to ReVanced artifacts and made credential configuration conditional, allowing the public build configuration to proceed. The local retry compiled the Manager API module but failed fetching Patcher Android 22.0.2-dev.1 and Library Android 4.0.0 with HTTP 401.
- Prevented a previous APK's emulator evidence from being attached to a different APK. Seven regression tests passed; the host total is now 55 checks.
- Added an emulator smoke helper that confirms the installed APK's hash, package process and crash observation. Manager and Core receive separate screenshot and report filenames.
- Added a private source-import workflow using a temporary repository token. Only the import job requests repository-content write access; Android jobs request package read access.
- Added Android lint to Core's hosted checks.
- The first hosted source-import and web/security jobs passed. Android jobs failed before compilation because the setup action requested the removed SDK package `tools`. Corrected the action to install `platform-tools` and restarted the build.

Current source-restoration build: https://github.com/NavigatorJagdish/javo-revanced/actions/runs/41620062898

The restarted Android jobs are in progress as this note is written. No new Manager APK or patch-execution success is claimed. The initial Core APK and initial package remain available as the earlier verified snapshot. Account authentication, playback and proprietary source-APK testing still require real authorized inputs.

## Hosted build corrections
The first authenticated Manager build compiled an APK; lint found 405 missing regional translations and four locale/resource access errors. Compose reads now use observable configuration/resources. MissingTranslation is explicitly a warning because upstream regional translations are incomplete and Android uses English fallback; no translations are claimed complete. Other lint errors still fail the build.

The source packager incorrectly excluded every directory called tools/build, including 46 legitimate Java/Kotlin sources. Filtering is now root-aware and preserves source packages. Regression checks cover helper inclusion and cache/key exclusion. Hosted pipelines use pipefail and preserve raw APKs even on validation failure. The branding compiler uses the same experimental features as its pinned Patcher dependency.

## Additional verification prepared
57 host checks passed after adding package completeness regressions. All original source files from the five pinned upstream archives are present in the corrected package selection (evidence/upstream-source-completeness.json).

The next build includes a first-party Android integration fixture for each branding target. The instrumentation uses the production bundle loader, Session resource rebuild, and APK signer, then checks package/label/signature metadata. The host harness extracts the signed outputs, compares exact hashes, installs and launches them on an emulator. These fixtures contain no YouTube/Music code and are not distributed as working Video/Music applications. A separate diagnostic attempts Android metadata loading of the signature-inspected official catalog; it does not execute official patches.

The Manager dashboard now displays a real loaded-patch count and a reference-based workspace card. Patching displays circular progress derived from completed production steps, retaining stage logs and cancellation/install actions. These changes remain subject to the next compile/lint check.

## Source-restoration build result
Run 41620062898 at commit d0a5dcb8a53c1e721b6c53996a16066f2f21302f: Manager assemble/lint and branding JAR/DEX conversion passed. Manager lint reported 0 errors and 685 warnings (including regional translations). Manager installed and launched on Android API35 x86_64, with the installed hash matching 78a607a56c00aed994d46b06eb88d83b443e8b170791de4863cfd86eef6cd5fd; process alive and no observed crash. This run did not yet contain the first-party patch/sign integration test or newer dashboard/progress additions.

Core compiled and passed lint in the same run. Its hosted emulator failed before testing: 6956.75MB available versus the default 7372.80MB userdata requirement. The next workflow explicitly requests a 2048MB userdata partition and grants the runner user access to its existing KVM device. The packaging job correctly remained skipped. Raw APK artifacts were retained and their downloaded ZIP hashes match GitHub's artifact digests.

## Android XML compatibility fix
A standalone DEX diagnostic on Android API29 reproduced ParserConfigurationException for the desktop Xerces disallow-doctype feature. BrandingResources now strictly decodes and size-bounds the actual UTF-8 text, rejects DTD/entity declarations and NULs before parsing, and supplies a rejecting entity resolver. It performs only an identity transformation of the guarded DOM. Both real transformation and external-entity rejection passed on Android (evidence/android-xml-probe.log). Host branding coverage increased from four to six checks; the host total is 59.

The next workflow retires source bootstrapping and its repository write permission. The repository is now ordinary complete source. Root-only Git ignore rules preserve legitimate nested tools/build packages.


## Successful delivery run

Run 41637751030 completed all five jobs at source commit 9c8cdc283dc64a95a6fd9bf62d45a71babd14f46. Removing the build-only NDKs after compilation freed the temporary runner disk for its emulator. Manager and Core passed launch checks. Manager processed both first-party fixtures and loaded 294 entries from the official catalog. See FINAL_REPORT.md and the packaged evidence for the verification boundaries.
