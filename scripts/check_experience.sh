#!/usr/bin/env bash
set -euo pipefail
collect() {
 mkdir -p ux-screenshots
 adb pull /sdcard/Android/data/dev.breda.tap/files/ux ux-screenshots/ || true
}
trap collect EXIT
./gradlew -PincludeAndroid=true :app:connectedDebugAndroidTest --stacktrace
