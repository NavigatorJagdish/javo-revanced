# Continuation state — 11 November 2026

## 13 November playback work — latest local state

Manager alpha.14 is an unreleased source candidate. See
[playback candidate status](PLAYBACK_ALPHA14_CANDIDATE.md). Its guarded stream
fallback and Javo repair/help URI passed ten checks using the real transformed
extension on API 29. Host checks passed 36 Node tests, 24 Python tests and six
Java suites. Full Manager compilation stopped because AGP 8.13.2 is no longer
cached and cannot be downloaded; Comet confirmed DNS_PROBE_FINISHED_BAD_CONFIG.
GitHub connector requests also failed. No alpha.14 APK exists or was published.
The source candidate, portable binary-safe source patch and verification receipt
are stored locally in deliverables/alpha.14-candidate. No proprietary APK, key,
private input or raw account log is included in the candidate source archive.

The owner reports ordinary-video playback stalls on YouTube 20.40.45. A targeted
SABR-only response fallback fix is prepared; the owner's real two-minute playback
problem is not confirmed resolved. New YouTube versions require reviewed bundle
compatibility and actual patch/playback verification. Keep Play Protect enabled.
The existing public alpha.13 review download remains known-blocked by the owner;
none of its test or scan results can be used to certify alpha.14.

Manager alpha.13 and Core javo.6 are the current review sources. Run 42996215236 at bf324721e415562a0ee7910deb703b77babada37 passed both application jobs, build/tests/lint and Android 10/15 checks. Packaging failed on a host cache path mismatch; restore paths now include the framework recovery report.

Offline Manager SHA-256: b189481494d7f92b025afa93beff5f4866e7ba01d63408cfa1bed4455514075a. Its 634 non-signature ZIP entries equal the cloud-tested input. Android 10 update/launch passed. Owner reports harmful on the phone: public distribution held.

Offline Core SHA-256: c5bb18a75cc2b70a6729a43aa92054f81f3f959b586e6fb4af588ba685b96311. Offline signer, eight private manifest boundaries, five denied external-start probes and Android 10 update/launch passed. Owner reports safe on one phone. Original verified Core source 95cfdc6ad93b48a71aa542116c39c4f5f720e6e0 has the same Core tree as bf324721e415562a0ee7910deb703b77babada37.

Fresh runtime scan: Manager 385 versions, Core 191, 536 distinct pairs. No Manager advisory matches; two legacy Core libraries have three open advisories. See DEPENDENCY-REVIEW.md.

Alpha.11 VirusTotal and submitted Google appeal do not cover alpha.13. Physical playback, account login, actual YouTube audio/video downloads and a slow-phone 20-minute run remain unverified. Proprietary APKs remain private.

Google site ownership verified; homepage indexing requested. Sitemap fetch and ranking unconfirmed. Source shortcuts removed; licensing retained. XERJ clarification sent; broad approval pending a four-file scope. Historical public archives and older unmasked commit emails still need cleanup.

Signing material stays local. A private binary recovery archive was blocked during Drive readback, so it is not a verified backup. Preserve local originals. Drive stores backups; GitHub Actions runs builds.

## Latest receipts

The full corrected workflow 43028279298 passed at df2621b2bfcc8c43d6a4fb1412665f56aafa88be. Core javo.6, corresponding source ZIP, checksums and installation guides are published under core-v0.3.13.2.250932-javo.6. Public APK and source ZIP digests match the verified local files.

Alpha.13 VirusTotal completed with 65 undetected, three timeouts (including Google), seven unsupported and no malicious flags observed. Google confirmed alpha.13 appeal receipt. Manager remains held; exact-file review is tracked at https://github.com/NavigatorJagdish/javo-revanced/issues/1.

Source and original-reference private Drive backups were downloaded and checksum-verified. Stopped obsolete emulator disks were removed, freeing 27.77 GiB. Signing keys, current source, APKs, one emulator and dependencies remain local. The separate binary recovery archive is not a verified backup because Drive blocked its download. See the owner's private recovery guide; do not publish its contents or private backup links.

## Approved release-asset cleanup completed

Owner approved permanent removal of eight alpha.1–alpha.8 uploaded source ZIPs and both alpha.10 APKs. All ten were deleted and verified absent using fresh GitHub release metadata. The local backups were rehashed before deletion. New Core javo.6, source ZIP and guide/report assets, and all unselected release assets, retain their original IDs and digests. Historical Git snapshots, auto-generated tag archives, older emails and external copies remain outside this cleanup. See evidence/public-asset-removal.json.

## Explicit review publication request

The owner requested Manager publication after being informed of its unresolved Play Protect indication. The exact tested alpha.13 APK is therefore available only as an experimental review download with prominent warning metadata. This does not establish clearance or resume blocked installation. Keep protection enabled and stop if blocked. See RELEASE_MANAGER13_REVIEW.md and GitHub issue #1.
