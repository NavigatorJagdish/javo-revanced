# Javo ReVanced 0.1.0-alpha.6

Reworks APKMirror downloading after the repeated reports that the button still did nothing in alpha.5.

The actual Manager browser now has persistent native Download APK, Open browser and Use downloaded APK controls, page progress and visible status text. The Download button resolves the site's control and returns the resolved CDN file URL to the Manager after the browser visits the protected download endpoint; it no longer depends on Chromium emitting a binary download event. Intermediate pages continue the requested download. WebView compatibility markers are removed from its user agent while the actual engine version is retained. Site verification is not bypassed.

If the site requires a full browser, download there, return to the same Manager source screen and tap Use downloaded APK. The native document picker copies the selected file into a bounded private cache. The existing repository validates package/version, handles APK or APKM merging, continues the patch session and removes the temporary copy. Files outside the dedicated import directory are rejected.

The workflow tests the actual DownloaderActivity and WebViewFragment, native button chaining, document-picker result delivery, host import/merge, temporary cleanup and path rejection on Android 10 and 15. Existing HTTPS transport and patch/sign/install checks remain. There are 102 host checks. FINAL_REPORT.md and release evidence record actual outcomes; controlled tests do not guarantee live site access from every network.

A separate opt-in live APKMirror diagnostic uses the actual Manager screen. See evidence/alpha6-live-mirror.json for its development-environment result when available. No proprietary APK is distributed. Playback/account login remain unverified.

Development signing identity is now restored from a private cache/Actions backup, with certificate pinning after this release and refusal to silently rotate a missing identity. The key is excluded from source ZIPs and release assets. Its private backup is for the owner, not a production signing setup. Moving from alpha.5 may still require exporting settings and the patch-signing keystore, clean-installing Manager and importing the backups. Keep the working Core; its binary and certificate are unchanged.

The live redirect also exposed a missing file host in the old allowlist. The dedicated APKMirror R2 bucket and its downloadprod file prefix are now recognized. Other R2 tenants and non-APK paths remain blocked. Browser requests resolve the protected endpoint before host transfer; binary CDN URLs do not replace the source-page Referer.
