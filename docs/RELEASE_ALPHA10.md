# Javo ReVanced 0.1.0-alpha.10

Manager now allows 20 minutes after preparation for patch execution and APK writing. Preparation still has its own eight-minute limit. The patch worker keeps the CPU awake for the longer operation, subject to Android's background execution limits.

Javo Core 0.3.13.2.250932-javo.4 verifies the complete APK and its pinned signer before loading a legacy DroidGuard module. It rejects unsigned or modified files, unexpected signers and invalid cache keys. The verifier accepts a matching test signer and rejects the negative cases when checking the API 19, 29 and 35 signature rules. Live DroidGuard, account sign-in and Play Protect classification are separate checks.

Manager and Core retain their alpha.9 signing identities. Android can update the matching alpha.9 builds in place. A Core build using a different signing certificate needs a clean install, which removes its local settings and account data.

If Play Protect shows **App Scan Recommended**, let the scan finish. If it blocks installation after the scan, record the warning text, Core version and Android version. The Core APK's SHA-256 and signer are recorded in the release evidence. Google provides an app verification review for suspected misclassifications. [Google Play Protect warning guide](https://developers.google.com/android/play-protect/warning-dev-guidance)

The public Javo site serves verified release links, installation guidance and a crawlable sitemap. [Javo website](https://navigatorjagdish.github.io/javo-revanced/) · [Sitemap](https://navigatorjagdish.github.io/javo-revanced/sitemap.xml)

Both signed release APKs updated alpha.9 in place and launched on Android 10. Manager passed the download, split merge, patch, sign and install fixtures. Core's packaged verifier passed all four signature cases on the same emulator. [Test evidence](../evidence/release-alpha10.json)
