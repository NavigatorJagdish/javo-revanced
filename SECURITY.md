# Security policy

## APK distribution under review

A physical-device report confirms that Google Play Protect showed **Harmful app blocked** for alpha.10 Javo Manager. The APK from that device matches the GitHub release's SHA-256. Play Protect warnings have also been reported for alpha.10 Javo Core and are under review. Keep Play Protect enabled and stop the installation if the block appears. [Google Play Protect warning guide](https://developers.google.com/android/play-protect/warning-dev-guidance)

The same warning was reported for the checksum-verified alpha.11 Manager review build. The owner also reported a harmful indication for the exact offline-signed alpha.13 Manager, SHA-256 `b189481494d7f92b025afa93beff5f4866e7ba01d63408cfa1bed4455514075a`. The owner subsequently requested publication of this exact APK as an experimental review download. Its harmful indication remains unresolved; it is not a recommended or cleared installation.

For offline-signed Core javo.6, SHA-256 `c5bb18a75cc2b70a6729a43aa92054f81f3f959b586e6fb4af588ba685b96311`, the owner reported a safe result on one phone. That is not clearance for every device. Signed integrity, eight private manifest boundaries, five denied external component starts and an Android 10 update/launch passed. Matching Core source passed cloud Android 10/15 checks. Three advisory matches in two legacy Core libraries remain open; see `docs/DEPENDENCY-REVIEW.md`. Login, push and real playback remain unverified.

On November 11, 2026, the exact alpha.11 Manager APK was submitted to VirusTotal and Google Play Protect review. VirusTotal reported 66 undetected results, two timeouts and seven unsupported file-type results. Google's appeal form confirmed receipt. Device-level Play Protect clearance remains pending. [VirusTotal report](https://www.virustotal.com/gui/file/86d04a37709528a986c79b09df7ff03a3622b7061b37b20394af3f9737f563a2)

A recommended release requires signed APK integrity checks, a matching source revision, dependency and permission review, clean install and update tests, application workflow tests, and a verified Play Protect result for the exact final APKs. Record the Android version, install source, artifact SHA-256 and outcome for each file. A successful emulator build or Windows antivirus scan does not answer the Play Protect question.

Publish corresponding licensed source, changelogs, approved screenshots and sanitized test summaries. Keep the project brief, original reference files, raw logs, test account data, local settings, tokens and signing material in private storage. Deleting a file from the current branch does not remove copies in older tags, release archives, forks or clones.

Never publish passwords, private signing keys or access tokens in issues, logs or source. For a private report, use an owner-controlled private channel.

If Play Protect blocks an installation, keep Play Protect enabled and stop the installation. Record the exact warning, Android version, app version, APK filename and SHA-256. A locally patched video or music APK differs from the GitHub Manager and Core APKs; include its own SHA-256 and selected patch versions if that is the APK affected. Do not attach third-party APKs or logs with account information to public issues.

The published APK signatures and checksums establish artifact identity. GitHub CI uses disposable test signing identities. The published Manager retains its development certificate for in-place updates; the published Core uses a dedicated offline release certificate. Neither a valid signature nor an emulator test establishes Google's classification. If Google appears to have misclassified a verified release, see [Google's app verification appeal](https://support.google.com/googleplay/android-developer/answer/2992033?hl=en).
