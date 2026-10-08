#!/bin/sh
# Use adb replacement installs. Gradle connected tests may uninstall the app and erase personal data.
set -eu
cd "$(dirname "$0")/.."
serial=${1:?Usage: test-scanner-device.sh DEVICE_SERIAL [TEST_CLASS]}
test_class=${2:-org.rhythmeta.chunithmd.SongScannerDeviceTest}
(cd android && ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest)
adb -s "$serial" install -r android/app/build/outputs/apk/debug/app-debug.apk
adb -s "$serial" install -r android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
# Never fall back to uninstall, clear, or reinstall without -r after a signing/install error.
result_file=$(mktemp)
trap 'rm -f "$result_file"' EXIT
adb -s "$serial" shell am instrument -w -r -e class "$test_class" \
    org.rhythmeta.chunithmd.test/androidx.test.runner.AndroidJUnitRunner > "$result_file"
cat "$result_file"
# am instrument can return shell status 0 even when the JUnit run failed.
rg -q '^OK \([0-9]+ tests?\)' "$result_file"
