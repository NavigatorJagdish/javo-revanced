# Verification status · 11 November 2026

APK distribution is held. This is a development review, not a clean Play Protect verdict.

| Area | Observed evidence and remaining limit |
| --- | --- |
| Manager alpha.11 | Exact signed APK was reported blocked on the owner's phone. VirusTotal showed 66 undetected, two timeouts and seven unsupported results. Google confirmed receipt of the Play Protect appeal. Device clearance remains unverified. |
| Manager alpha.12 | Release build and lint passed. The exact offline-signed candidate passed alpha.10 upgrade/launch and controlled browser, download, media-save, patch/sign/install checks on Android 10. Isolated patcher startup is bounded at 60 seconds and patch/save at 20 minutes after preparation. Real-input checks and device Play Protect classification remain pending. |
| Core javo.6 | Source revision `95cfdc6ad93b48a71aa542116c39c4f5f720e6e0` passed the cloud Core build, tests, permission/signature audit, eight compiled-manifest private boundaries and Android 10/15 launch checks using a disposable CI signature. Wire 6.4.5 and the tracked upstream security backports are included. Offline release signing and fresh boundary probes remain pending. Legacy Cast Jackson mapper and address formatter SnakeYAML advisories remain open. |
| Patching | Alpha.12 locally patched, signed and installed the signature-verified YouTube 20.40.45 reference APK in 303 seconds on Android 10. Mandatory Javo branding, settings and the Core-download destination were verified in the output. Local first-party fixtures and terminated-patcher recovery passed. Its cloud Android 10 fixture run failed with a remote-process error and is being investigated. Latest live APKMirror and slow physical-phone checks are not confirmed. |
| Video/audio | Controlled MP4/M4A Downloads storage, interrupted audio resume and separate-track mux passed. Actual YouTube playback, account login and real-phone stall reproduction remain unverified. Proprietary source and patched APKs are private test inputs/outputs, not release assets. |
| Source privacy | Current packaging excludes original inputs, reference images, raw logs, local configuration and signing material. Historical public snapshots and old source ZIPs still require cleanup; removing current files does not erase history or clones. |
| Website | Source download shortcut removed; canonical pages, app descriptions, breadcrumbs, sitemap and security status deployed. Search Console ownership verified and homepage indexing requested. Sitemap submission accepted but Google fetch reported failure; live sitemap returns HTTP 200 with 16 URLs. Ranking and indexing are unconfirmed. |
| XERJ invitation | Professional scope clarification sent. Approval is pending confirmation of a four-file allowlist that excludes repository history and private original inputs. No blanket repository indexing approval was given. |

The exact Manager alpha.11 review target is
`86d04a37709528a986c79b09df7ff03a3622b7061b37b20394af3f9737f563a2`.
[VirusTotal report](https://www.virustotal.com/gui/file/86d04a37709528a986c79b09df7ff03a3622b7061b37b20394af3f9737f563a2).
It does not establish that Google Play Protect will allow installation.

The alpha.12 offline-signed review APK is
`b65eb72cd9b86240ccea0c512dfdb168be6f3d31a475e0e131c730c6ff88d2a4`.
The alpha.11 VirusTotal report and Google appeal do not certify this changed APK.

No public release is certified solely by a successful compilation, checksum, valid signature,
dependency scan or a settings-screen screenshot. Keep Play Protect enabled.
