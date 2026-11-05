# Javo ReVanced 0.1.0-alpha.4

This update retains alpha.3 browser/download/process fixes and adjusts patcher memory startup after a real YouTube APK check.

The requested heap defaults to 700 MB. Android 10 and older cap it at 700 MB as well as one quarter of device RAM. A 985 MB heap failed in ART before patch execution on the Android 10 emulator; 700 MB reached actual YouTube resource preparation and patch execution. Logs record the effective heap. Core remains the exact verified alpha.2 binary and certificate.

The workflow runs 89 host checks, Android 10/15 browser and authenticated HTTPS-transfer fixtures, separate-process preparation/patching, forced process termination recovery, signing, installation and launch. Android 10 now uses a 4 GB guest to exercise the cap that the smaller fixture guest did not reach. Exact build outcomes and APK hashes are in FINAL_REPORT.md and the included evidence.

A separate optional local instrumentation mode accepts an owner-provided YouTube 20.40.45 APK. It tests GmsCore support, Spoof video streams and Javo Video branding, then signs the output. Proprietary input/output APKs are excluded from release assets. See evidence/alpha3-real-youtube.json for the local result and its exact Manager hash; this is not a test of every official patch. Playback and account sign-in remain unverified.

APKMirror navigation was fetched successfully with a mobile browser identity and Referer, and a Google-signed YouTube APK was downloaded using Python. A separate Java request returned HTTP 403. Live source access can still require the site's browser challenge; the real production download pipeline is verified against controlled HTTPS fixtures.

Export Manager settings and its patch-signing keystore before changing installations. Development certificates may differ. APKs, branding RVP, source ZIP and SHA256SUMS are attached to this private prerelease.

