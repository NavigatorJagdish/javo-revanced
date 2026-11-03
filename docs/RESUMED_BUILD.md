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

Current restarted build: https://github.com/NavigatorJagdish/javo-revanced/actions/runs/41617473242

The restarted Android jobs are in progress as this note is written. No new Manager APK or patch-execution success is claimed. The initial Core APK and initial package remain available as the earlier verified snapshot. Account authentication, playback and proprietary source-APK testing still require real authorized inputs.

## Hosted build corrections
The first authenticated Manager build compiled an APK; lint found 405 missing regional translations and four locale/resource access errors. Compose reads now use observable configuration/resources. MissingTranslation is explicitly a warning because upstream regional translations are incomplete and Android uses English fallback; no translations are claimed complete. Other lint errors still fail the build.

The source packager incorrectly excluded every directory called tools/build, including 46 legitimate Java/Kotlin sources. Filtering is now root-aware and preserves source packages. Regression checks cover helper inclusion and cache/key exclusion. Hosted pipelines use pipefail and preserve raw APKs even on validation failure. The branding compiler uses the same experimental features as its pinned Patcher dependency.
