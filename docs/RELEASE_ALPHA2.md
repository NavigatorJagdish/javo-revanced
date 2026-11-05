# Javo ReVanced 0.1.0-alpha.2

This update addresses the reported Core installation block and APKMirror download stalls.

## Core

The non-system companion build removes inherited SMS, overlay and ROM signature-spoofing permission requests. Account-data backup is disabled. Raw account, token, request and result logging is removed from the two modified authorization service entry points. Self-check only lists permissions declared by the installed build. The APK is built with release runtime flags and a test signing certificate. SMS retrieval and overlay-dependent features are outside this build's supported scope.

The earlier APK requested RECEIVE_SMS. Google's guidance identifies this permission as a reason internet-sideloaded apps can be blocked in supported markets. The exact warning on the reporter's device is still needed to confirm the classification. Google controls the device verdict. [Google developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance)

## Manager

APKMirror downloads use a host-managed transfer and split-merging path. Downloaded modules remain open until the APK has been written. The pipeline reports transfer, merge and write stages, applies network timeouts, handles missing Content-Length, carries the browser session to the initial download, rejects unsafe bundle paths, and fixes byte counting for nonzero offsets. Logs can be exported during patching.

The Android regression suite checks missing-size progress, nonzero-offset writes, stalled plugin recovery, two-split APKM merging, and archive traversal rejection. The merged first-party fixture then follows the production patch, sign, install and launch test. See the included evidence for the actual results of the build that produced these assets.

## Installation

Download the APKs and SHA256SUMS from this release. Development certificates can differ from the first alpha. Android may reject an in-place update if the installed certificate differs. Export the Manager patch-signing keystore and settings before changing an installation. Account sign-in, real YouTube playback and the physical-device Play Protect verdict remain unverified.
