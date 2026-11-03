#!/usr/bin/env bash
set -euo pipefail
package="$1"
apk="$2"
mkdir -p evidence/screenshots
adb logcat -c
adb install -r "$apk" | tee evidence/android-install.txt
adb shell monkey -p "$package" -c android.intent.category.LAUNCHER 1 | tee evidence/android-launch.txt
sleep 15
adb shell pidof "$package" | tee evidence/android-process.txt
adb logcat -d -b crash > evidence/android-crashes.txt
if grep -F "Process: $package" evidence/android-crashes.txt; then exit 1; fi
adb exec-out screencap -p > evidence/screenshots/core-emulator.png
echo 'Install and launch smoke check passed. Account authentication, playback and push delivery were not tested.'
