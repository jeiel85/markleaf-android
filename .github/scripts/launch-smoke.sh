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

# `adb install` returns when the install session commits, which is before the
# package manager has the app indexed. Starting the activity in that window
# fails with "Error type 3 / Activity class does not exist" -- the app is on
# disk, the launcher just cannot see it yet. That failure looks identical to a
# genuinely broken build, and it is what the job's reputation as a flake was
# partly built on (#247, #252, #262).
#
# Wait for the package manager to answer instead of sleeping a fixed amount:
# a slow CI emulator can take several seconds, a fast one none at all.
echo "Waiting for the package manager to see the app"
for _ in $(seq 1 30); do
  if adb shell pm path com.markleaf.notes 2>/dev/null | tr -d '\r' | grep -q '^package:'; then
    echo "Package indexed."
    break
  fi
  sleep 2
done

if ! adb shell pm path com.markleaf.notes 2>/dev/null | tr -d '\r' | grep -q '^package:'; then
  echo "Package manager never listed com.markleaf.notes after a successful install"
  exit 1
fi

adb shell am start -W -n com.markleaf.notes/.MainActivity
adb shell pidof com.markleaf.notes
