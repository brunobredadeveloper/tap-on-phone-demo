#!/usr/bin/env bash
set -euo pipefail
collect() {
 mkdir -p ux-screenshots
 adb pull /data/local/tmp/breda-ux ux-screenshots/ || true
}
trap collect EXIT
./gradlew -PincludeAndroid=true :app:connectedDebugAndroidTest --stacktrace
