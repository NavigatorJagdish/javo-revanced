# Javo ReVanced 0.1.0-alpha.9 — security update

- **Manager:** APK installs require Android confirmation. The Manager no longer requests permission to update apps without user action or claim update ownership. If a signed update feed is configured, downloading an update and opening the installer are separate actions.
- **Core:** Javo Core 0.3.13.2.250932-javo.3 no longer requests broad legacy storage access. It blocks cleartext connections, including the previous exceptions for certain Wi-Fi location portals. This can affect Wi-Fi location on those networks. Account and notification setup still depend on the device and selected patches.
- **Updates:** The Manager keeps the alpha.8 development certificate. Core now uses a dedicated release certificate. Android cannot install it over the earlier test-signed Core or another GmsCore build with the same package ID. If you choose to switch, plan to set up Core again; uninstalling the old Core removes its local settings and accounts.

Use the files under **Assets** and compare their hashes with `SHA256SUMS`. These changes reduce the apps' permissions and network exposure. Google Play Protect's classification of these APKs has not been verified on a device with Google Play enabled. If it blocks an APK after scanning, stop the installation and record the filename, Android version, and exact warning. See [Google's app verification appeal](https://support.google.com/googleplay/android-developer/answer/2992033?hl=en).

Patched video or music APKs are generated locally and have their own signatures and hashes. A verdict about a locally patched APK must be investigated separately.
