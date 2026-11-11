# Verification status — 11 November 2026

Manager distribution is held. The owner reports Core javo.6 safe on one phone, not a universal safety verdict.

| Area | Evidence and remaining limit |
| --- | --- |
| Manager alpha.13 | Cloud build, tests, lint and controlled Android 10/15 browser/download/media-save/patch/sign/install checks passed. Truncated framework cache recovered on both APIs. Offline re-signing preserved 634 application ZIP entries; Android 10 alpha.10 update/launch passed. Owner still reports harmful on the phone. |
| Preparing/patching | Validated, locked framework initialization publishes a complete ZIP before parallel decoding; versioned cache paths isolate old files. Startup 60 seconds; preparation and patch/save have separate 8-minute and 20-minute windows. Latest live APKMirror and slow-phone runs unconfirmed. |
| Core javo.6 | Offline signature/permissions/flags, eight private manifest boundaries, five denied external-start probes and Android 10 update/launch passed. Matching Core source passed cloud build, unit tests, lint and Android 10/15 launch with CI signing. Owner reports safe for the copied Core file. Login/push/location/FIDO unverified. |
| Dependencies | Fresh runtime graphs: Manager 385, Core 191 versions; 536 distinct pairs. OSV: no Manager matches, three advisories affecting two legacy Core libraries. See DEPENDENCY-REVIEW.md; no zero-vulnerability claim. |
| Real APK patching | CI-signed alpha.13 patched/signed/installed signature-verified YouTube 20.40.45 in 204 seconds on Android 10. Four selected patches, Javo label/icon/settings, light/dark headers, product logos, built-in download default, cellular 480p default and Javo Core link were verified in the output. This does not certify playback or the offline candidate's Google classification. |
| Media | Controlled MP4/M4A Downloads save, audio resume and separate-track mux passed on Android 10/15 for CI alpha.13. Actual YouTube playback, login, real downloads and physical-phone stalls remain unverified. Proprietary APKs stay private. |
| Google review | Alpha.11 VirusTotal: 66 undetected, two timeouts, seven unsupported; Google appeal receipt confirmed. Neither clears alpha.13. |
| Packaging | Run 42996215236 application/host jobs passed; package job failed on mismatched host-cache paths. Restore list corrected. Local source ZIP requires its own integrity verification. |
| Privacy | Current packages exclude original inputs/references, raw logs, local settings and keys. Historical public ZIPs/snapshots and older commit emails still need cleanup. Clones cannot be erased by deleting current files. |
| Search | Source shortcut removed, metadata/sitemap deployed. Google ownership verified and homepage indexing requested. Sitemap fetch and ranking unconfirmed. |
| Other | XERJ clarification sent; broad approval pending. Private Drive binary recovery archive blocked on readback, so not a verified backup. Local originals intact. |

Offline Manager alpha.13: b189481494d7f92b025afa93beff5f4866e7ba01d63408cfa1bed4455514075a.
Offline Core javo.6: c5bb18a75cc2b70a6729a43aa92054f81f3f959b586e6fb4af588ba685b96311.

[Application verification run](https://github.com/NavigatorJagdish/javo-revanced/actions/runs/42996215236). Keep Play Protect enabled and stop if an APK is blocked.
