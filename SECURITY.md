# Security policy

Never publish passwords, private signing keys or access tokens in issues, logs or source. For a private report, use an owner-controlled private channel.

If Play Protect blocks an installation, keep Play Protect enabled and stop the installation. Record the exact warning, Android version, app version, APK filename and SHA-256. A locally patched video or music APK differs from the GitHub Manager and Core APKs; include its own SHA-256 and selected patch versions if that is the APK affected. Do not attach third-party APKs or logs with account information to public issues.

The published APK signatures and checksums establish artifact identity. GitHub CI uses disposable test signing identities. The published Manager retains its development certificate for in-place updates; the published Core uses a dedicated offline release certificate. Neither a valid signature nor an emulator test establishes Google's classification. If Google appears to have misclassified a verified release, see [Google's app verification appeal](https://support.google.com/googleplay/android-developer/answer/2992033?hl=en).
