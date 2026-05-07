#!/usr/bin/env bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"

adb wait-for-device
BOOT_COMPLETED="$(adb shell getprop sys.boot_completed | tr -d '\r')"
echo "sys.boot_completed=${BOOT_COMPLETED}"

# adb install can intermittently fail with broken pipe on CI emulators.
# Retry with adb server restart before failing the job.
for attempt in 1 2 3; do
  echo "Install attempt ${attempt}"
  if adb install -r "$APK"; then
    break
  fi

  if [ "$attempt" -eq 3 ]; then
    echo "adb install failed after retries"
    exit 1
  fi

  adb kill-server || true
  adb start-server
  adb wait-for-device
  sleep 5
done

adb shell am start -W -n com.markleaf.notes/.MainActivity
adb shell pidof com.markleaf.notes
