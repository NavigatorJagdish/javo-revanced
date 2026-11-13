# Manager alpha.14 playback candidate — not released

The reported problem is normal video playback stopping after about two minutes
on patched YouTube 20.40.45. The exact cause on the owner's device has not yet
been confirmed from playback diagnostics.

## Changes prepared

- When the selected YouTube patches include **Spoof video streams**, reject an
  adaptive-format response that contains neither a direct URL nor a signature
  cipher in any format. The existing upstream parser then closes its input and
  tries the next client instead of accepting an unusable SABR-only response.
- Build the preferred stream-client list privately, then publish the completed
  array with a single volatile write. Concurrent fetches cannot see unfilled
  slots; repeated preferred-client entries do not leave null entries.
- Change the environment check dialog's repair/help action to the Javo
  installation guide. Preserve its warnings, checks and upstream attribution.
  This dialog is separate from Google Play Protect.
- Enforce the patch bundle's declared YouTube version compatibility before
  execution, including when an incompatible patch was selected elsewhere in
  the interface. Reject unknown extension layouts with an actionable message.
- Use three checked-in synthetic media vectors for transfer/mux tests, avoiding
  an external FFmpeg package installation during CI. Reject corrupt vectors.
- Add Android regression checks to the normal integration suite and to the
  private real-YouTube patch test.

The upstream bundle remains the inspected 6.2.1 snapshot, SHA-256
`50f41e4656b7374a1c8fe5579fd107f697f86d7f13fc6cd194cf3821761dc4b4`.
These are Javo additions applied to its output, not an official newer upstream
release. The [official API](https://api.revanced.app/v5/patches) still advertised
6.2.1 when rechecked on 13 November 2026. No additional YouTube version is
certified by this candidate.

## Verification so far

The production Kotlin transformations compiled locally against the pinned
Patcher API. The transformed real extension ran on Android API 29 and passed
12 checks: empty formats, SABR-only rejection, direct URL, signature cipher,
mixed formats, 1,000 unusable formats, a usable last format, parser rejection
and input closure, parser acceptance, atomic client-list publication, repeated
preferred clients, and the Javo repair/help URI. Duplicate
application and an unsupported response-parser layout were rejected on the host.

Host verification passed 36 Node tests, 24 Python tests and the Java security,
branding, download, browser-policy, framework-cache and root-argument suites.

**Final verification update — 13 November:** full workflow 43381854540 passed
Manager compilation, lint, unit tests and Android API 29/35 integration, including
all twelve runtime cases. The exact final CI input patched/signed/installed the
reference YouTube 20.40.45 in 297 seconds. Offline signing preserved all 634
application payload entries. See [current release notes](RELEASE_MANAGER14_REVIEW.md)
for hashes, installation instructions and test limits. Earlier network failures
are historical; final-build sustained playback and Play Protect clearance remain
unverified. Alpha.13's block and appeal are not a verdict on different alpha.14 bytes.

## Installing the fix after a verified release

Updating Manager alone cannot change an already installed Javo Video APK.
Install the verified Manager update with Play Protect enabled, select a clean
supported YouTube APK, include **GmsCore support** and **Spoof video streams**,
then patch, save and install the resulting Javo Video update. Stop if Play
Protect blocks it; do not disable protection or use a bypass for this test.

Use the versions declared compatible by the reviewed bundle. Future YouTube
versions require an updated reviewed bundle and a patch/install/playback test;
changing a version label does not make incompatible bytecode work.

After installing a verified candidate, test several ordinary videos longer
than five minutes, seek forward/back, switch between videos, and check Wi-Fi
and mobile data. Record the selected spoof client and any playback error or
Stats for nerds message without publishing account tokens or private logs.
