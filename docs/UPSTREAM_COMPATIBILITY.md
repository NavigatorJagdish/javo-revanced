# Official upstream inspection — 2026-11-03

| Component | Observed official state | Selected pin |
| --- | --- | --- |
| ReVanced Manager | Latest stable release API returned v2.6.0; Kotlin/Compose architecture, not Flutter | aed92b22d3efa91bf46ec6b9d7a8d470b22abeee |
| ReVanced Patcher | Latest stable release API returned v22.0.1 | Manager actually requires 22.0.2-dev.1; source d7f12ab77c205f9223169393714e751c85ad5e76 included for API inspection |
| ReVanced Library | Manager version catalog requires 4.0.0 | 299c63399c77f207291042a1abc3ba55374a2c64 source included for inspection |
| ReVanced GmsCore | Latest stable release v0.3.13.2.250932 | 80bfe22cfaf0de84b6c1e96fe3435877d3b9e08c |
| microG GmsCore | Latest release API returned v0.3.17.252432 | Reference only; not substituted for the ReVanced fork |
| ReVanced patches | GitHub returns HTTP 451; official API still serves 6.2.1 and links to GitLab | Public GitLab source pinned at 015fe10a23ea5b919a3fda6e7da2c176517ae75d (declares 6.1.0); not asserted identical to the API bundle |

## Inspected architecture

Manager uses Compose Material 3, Coroutines/Flow, Room, DataStore-backed preferences, WorkManager, Koin and Ackpine installation. Its PatchBundle.Loader calls the actual Patcher loadPatches API; metadata comes from loaded patch objects and JAR manifests. CoroutineRuntime and PatcherProcess apply real patch selections and options. Session calls the real patcher, emits actual progress events, rebuilds resources and bytecode through ReVanced Library, and stops on a patch exception. KeystoreManager and installation workflows are retained rather than replaced with simulations.

Manager's dependency catalog pins Patcher Android 22.0.2-dev.1 and Library Android 4.0.0. The corresponding source uses Kotlin context parameters and multiplatform Android/JVM publications. Patcher resource definitions use resourcePatch { compatibleWith(...); apply { ... } }. ResourcePatchContext exposes get(path) and document(path). Javo branding adapters follow those inspected APIs, but compilation is not claimed verified.

Manager's package identity changed to app.javo.manager while its source namespace remains app.revanced.manager. File-provider authorities use applicationId placeholders. The custom deep-link scheme is javo-manager. Existing downloader plugin contracts remain upstream: compatibility with third-party downloader hosts is untested.

GmsCore retains app.revanced.android.gms, com.google.android.gms namespace, package_id resource, account types, binder interfaces, providers, permissions, services and registration code. Only visual assets/names/palette and source-archive version fallback were modified. This avoids breaking hard-coded patched-client integration expectations. It does not prove Google accepts login or device-registration requests.

## Access findings

The local Manager build actually failed on missing githubPackagesUsername/githubPackagesPassword. Direct access to the exact official Patcher POM returned HTTP 401. Building Patcher/Library source also depends on ReVanced-specific apktool-lib, multidexlib2 and jadb packages hosted through GitHub Packages; their source trees alone do not remove the package-access requirement. No unauthenticated mirror, embedded token or unofficial binary was substituted.

## Official references

- https://github.com/ReVanced/revanced-manager/tree/aed92b22d3efa91bf46ec6b9d7a8d470b22abeee
- https://github.com/ReVanced/revanced-patcher/tree/d7f12ab77c205f9223169393714e751c85ad5e76
- https://github.com/ReVanced/revanced-library/tree/299c63399c77f207291042a1abc3ba55374a2c64
- https://github.com/ReVanced/GmsCore/tree/80bfe22cfaf0de84b6c1e96fe3435877d3b9e08c
- https://github.com/microg/GmsCore/releases
- https://api.github.com/repos/ReVanced/revanced-patches
- https://revanced.app

## Official patch API and GitLab follow-up

The Manager's actual https://api.revanced.app/v5/patches endpoint returned 6.2.1 metadata and https://api.revanced.app/v5/patches.rvp. Its detached OpenPGP signature was checked against the key from https://api.revanced.app/keys; see evidence/official-patches-signature.json for the fingerprint, checksum and verifier warnings. This is signature verification, not patch execution or independent key-owner authentication.

The source URL in the bundle points to the official temporary GitLab repository (project 80488462). The public main source was recovered and pinned. The v6.2.1 tag lookup returned 404; the inspected main snapshot declares version 6.1.0 and Patcher 22.0.0. Therefore the source snapshot and downloaded binary are not treated as the same reproducible release. Downloaded RVP bytes are not redistributed in the delivery ZIP.

In the inspected source, the YouTube GmsCore/custom-branding definitions declare 20.14.43, 20.21.37, 20.26.46, 20.31.42, 20.37.48 and 20.40.45. Music definitions declare 7.29.52, 8.10.52, 8.37.56 and 8.40.54. These are source-declared constraints, not verified Javo compatibility. GmsCore support renames clients to app.revanced.android.youtube and app.revanced.android.apps.youtube.music. Javo's branding transformer preserves those recognized renamed identities when another trusted patch has already applied them.
