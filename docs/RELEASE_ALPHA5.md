# Javo ReVanced 0.1.0-alpha.5

Fixes the reported APKMirror Download button doing nothing on alpha.4.

APKMirror uses a second-page link with id download-link and a /wp-content/themes/APKMirror/download.php destination. The earlier helper recognized only the first-page /download/? control. The browser now recognizes both controls, checks their HTTPS origin/path, handles trusted download controls in the same window, and respects disabled controls. Download APK shows a message if the page has no ready control. Open in browser provides a fallback when the site requires an external browser; download there, return to Manager and select the APK from storage.

The Android regression suite exercises the second-page target=_blank link through a real WebView and requires its native download listener to fire. Existing browser/ad filtering, HTTPS transfer/session-cookie/APKM, process recovery and first-party patch/sign/install tests run on Android 10 and 15. See FINAL_REPORT.md and evidence for actual outcomes of this build.

The 700 MB default heap and Android 10 cap from alpha.4 remain. Core is the exact working alpha.2 APK and certificate. A separate local YouTube 20.40.45 test applied three named patches, signed, installed and opened the main navigation after account-discovery permission and Core battery setup. Its Manager build hash is recorded separately from CI artifacts.

Live source-site access can still depend on browser verification. The controlled download tests do not prove successful live APKMirror transport on every phone. Playback and account sign-in remain unverified. Export Manager settings and its patch-signing keystore before changing development installations; signing certificates can differ.

