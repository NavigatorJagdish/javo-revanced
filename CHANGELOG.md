# Changelog

## 0.1.0-alpha.14 — unreleased candidate

- Reject URL-less spoof stream responses so the existing client fallback can run.
- Route the patched app's environment repair/help action to the Javo guide.
- Enforce declared YouTube patch compatibility and validate the extension layout.
- Add Android stream parser regression checks and verified synthetic media fixtures.

See [candidate status](docs/PLAYBACK_ALPHA14_CANDIDATE.md). Real network playback
and the complete new Manager build have not passed yet; no new APK is published.

## 0.1.0-alpha.8

- Save supported MP4 video and M4A audio to Downloads from the Android share menu or Manager.
- Route Javo Video's download control to Manager and start at a lower mobile-data quality when the corresponding patches are included.
- Ship a non-debug Manager with narrower permissions, built-in APKMirror and a pinned default patch bundle.

## 0.1.0-alpha.7

- Apply Javo launcher and interface branding automatically when patching YouTube or YouTube Music.
- Offer a retry for APKMirror server errors and keep late browser messages from interrupting the file handoff.
- Check the reused Core APK's signing identity and signature scheme in the release audit.

## 2026-11-03 — development regeneration

- Recovered the master prompt and four reference images; prior source ZIP unavailable and regeneration authorized.
- Imported pinned complete Manager and GmsCore sources with licenses.
- Applied Javo Manager package/display identity, icon vectors and default dark palette; retained upstream patch/sign/install code.
- Applied Javo Core display/icon branding while retaining service compatibility identity.
- Added isolated signed-update source, host crypto verification tests, opt-in branding resource patches and tested XML transformations.
- Built a responsive 17-page static website with artifact-gated downloads.
- Added APK verification, packaging, CI, provenance and evidence documentation.

See docs/FINAL_REPORT.md for actual build results, tests and limitations. This entry describes a development source snapshot, not a published release.
