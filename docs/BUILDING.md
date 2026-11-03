# Reproducible build and verification guide

Use the pinned sources included in this archive. Their commit IDs and original archive SHA-256 values are in evidence/upstream-lock.json. Source modifications are listed in evidence/source-modifications.json. Do not replace pinned dependencies with arbitrary newer versions without compatibility testing.

Pinned source and direct versions do not promise byte-for-byte APK reproduction. Transitive upstream dependencies, build timestamps and a locally generated debug signing identity can differ. Private signing keys are not redistributed.

## Toolchain

- Manager: JDK 17, Gradle 9.4.0 (wrapper checksum pinned upstream), AGP 8.13.2, Kotlin 2.3.21, Android API 36.1 compile / 36 target / 26 minimum, CMake 3.22.1. The NDK is resolved by the upstream Android plugin.
- Core: JDK 17, Gradle 8.11.1, AGP 8.2.2, Kotlin 1.9.22, Android API 35 compile / 29 target / 19 minimum, build-tools 34.0.0. The tested target is VTM/default/debug.
- Website and host tests: Node.js 20+; no npm dependencies. Java tests use JDK 17. Artifact scripts require Python 3.11+.

Portable JDKs and the Android SDK were installed inside this workspace's ignored tools directory for the recorded build. They are not redistributed in the source ZIP. Set JAVA_HOME and ANDROID_HOME locally. The SDK manager must have accepted licenses and installed the declared components. Do not commit local.properties.

## Manager

Official Manager dependencies are hosted on GitHub Packages and require read access. Supply `ORG_GRADLE_PROJECT_githubPackagesUsername` and `ORG_GRADLE_PROJECT_githubPackagesPassword` through your local environment or CI secrets. Do not paste credentials into source or logs. The included GitHub workflow uses its temporary GITHUB_TOKEN with packages:read. Repository/package policy may still require explicitly granting package access.

From apps/javo-manager:

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --no-daemon
```

On Windows use gradlew.bat or infrastructure/scripts/build-android.ps1. The package ID is app.javo.manager; namespace remains upstream for source compatibility. The development version is 0.1.0-alpha.1. Default Javo update URL and public key are empty, so no upstream Manager APK can be offered as a Javo update.

## Core

Set `GRADLE_MICROG_VERSION_WITHOUT_GIT=1` to build from the included source archive; its version fallback is pinned to 0.3.13.2.250932 rather than an invented git revision.

From apps/javo-core:

```sh
./gradlew :play-services-core:assembleVtmDefaultDebug --no-daemon --no-configuration-cache --max-workers=2
```

The package ID remains app.revanced.android.gms. The source differs visually from upstream. A debug build cannot update an existing differently signed ReVanced GmsCore. Authentication has not been verified and must not be inferred from a successful build.

## Verify artifacts

```sh
python infrastructure/scripts/verify-apk.py path/to/built.apk --sdk path/to/android-sdk --product core --signing debug
```

The verifier runs official apksigner and aapt2, checks package identity, extracts version/minimum SDK/signing certificate, computes SHA-256 and adds only verified output to the local artifact inventory. It also copies the actual APK into website downloads. This is artifact verification, not runtime certification.

## Tests and packaging

Run infrastructure/scripts/test.ps1 with JAVA_HOME configured on Windows, or run the commands in the CI host-test job. Then build the website and run `python infrastructure/scripts/package.py`. The source archive excludes toolchains, caches, keystores and local settings. It includes source, evidence, website output and any APKs already verified against the inventory. ZIP contents are rehashed and tested before delivery.

CI has been run in the owner-approved private NavigatorJagdish/javo-revanced repository. Build, lint, runtime and packaging results are tracked separately in RESUMED_BUILD.md and the final report. Production signing, release publishing and hosted update keys require an owner-controlled release process and are not configured here.

## Credential-free continuation cache
The private CI exports only resolved app.revanced Maven POM/module/JAR/AAR files and SHA-256 inventory as resolved-revanced-maven-cache. It does not contain tokens. After verifying/extracting that artifact, set JAVO_MAVEN_CACHE to its absolute directory. Settings preserve original Gradle module metadata so multiplatform Android variants resolve correctly. SDK toolchains and this binary cache remain outside the distributed source ZIP.

## First-party Android integration
Build patches/javo-branding, convert its JAR with D8, and run build-integration-fixture.py with --sdk. Then build :app:assembleDebugAndroidTest and install the matching Manager APK on an isolated emulator. android-integration.py loads both resource definitions through the production loader, patches/rebuilds each first-party fixture using Session, signs each APK, verifies labels/package identities, and installs/launches the exact signed outputs. Test results are tied to the Manager and RVP hashes. No proprietary application code or account is needed for this limited check.

The official catalog diagnostic pins the checksum of the previously signature-inspected snapshot; it reports metadata loading separately from patch execution. If that mutable upstream endpoint changes or is unavailable, the diagnostic records the limitation instead of silently trusting new bytes.
