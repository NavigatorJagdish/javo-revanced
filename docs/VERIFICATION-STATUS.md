# Verification status · 11 November 2026

APK distribution is held. This is a development review, not a clean Play Protect verdict.

| Area | Observed evidence and remaining limit |
| --- | --- |
| Manager alpha.11 | Exact signed APK was reported blocked on the owner's phone. VirusTotal showed 66 undetected, two timeouts and seven unsupported results. Google confirmed receipt of the Play Protect appeal. Device clearance remains unverified. |
| Manager alpha.12 | Review candidate increases isolated patcher startup from 10 to 60 seconds. The patch/save deadline remains 20 minutes after preparation. Build and real-input checks are in progress. |
| Core | Wire runtime migration to 6.4.5 compiled and four malformed/valid protobuf regression tests passed. Compatible Compose tooling, location permission handling and an account-settings filter correction are under final validation. Legacy Cast Jackson mapper and address formatter SnakeYAML advisories remain open. |
| Patching | Alpha.11 passed controlled first-party browser/download/patch/sign/install tests. The renewed real YouTube test failed at the 10-second process-start deadline. Alpha.12 must pass its own checks; it cannot inherit another APK's results. Latest live APKMirror and slow physical-phone checks are not confirmed. |
| Video/audio | Controlled MP4/M4A Downloads storage, interrupted audio resume and separate-track mux passed. Actual YouTube playback, account login and real-phone stall reproduction remain unverified. Proprietary source and patched APKs are private test inputs/outputs, not release assets. |
| Source privacy | Current packaging excludes original inputs, reference images, raw logs, local configuration and signing material. Historical public snapshots and old source ZIPs still require cleanup; removing current files does not erase history or clones. |
| Website | Source download shortcut removed; canonical pages, app descriptions, breadcrumbs, sitemap and security status deployed. Search Console ownership verified and homepage indexing requested. Sitemap submission accepted but Google fetch reported failure; live sitemap returns HTTP 200 with 16 URLs. Ranking and indexing are unconfirmed. |
| XERJ invitation | Professional scope clarification sent. Approval is pending confirmation of a four-file allowlist that excludes repository history and private original inputs. No blanket repository indexing approval was given. |

The exact Manager alpha.11 review target is
`86d04a37709528a986c79b09df7ff03a3622b7061b37b20394af3f9737f563a2`.
[VirusTotal report](https://www.virustotal.com/gui/file/86d04a37709528a986c79b09df7ff03a3622b7061b37b20394af3f9737f563a2).
It does not establish that Google Play Protect will allow installation.

No public release is certified solely by a successful compilation, checksum, valid signature,
dependency scan or a settings-screen screenshot. Keep Play Protect enabled.
