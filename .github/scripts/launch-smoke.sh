#!/usr/bin/env bash
set -euo pipefail

APK="app/build/outputs/apk/debug/app-debug.apk"

# Read the identity out of the APK rather than hardcoding it. The debug build
# carries an `.debug` applicationIdSuffix (#319), so the installed package is
# not the one in `defaultConfig`, and the launchable activity keeps its original
# package -- which means the `-n <pkg>/.MainActivity` shorthand resolves to the
# wrong class. Both come from the APK so this script cannot drift from the build
# again: hardcoding the id here is what made the suffix unlandable the first
# time it was proposed (#318).
SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK_ROOT" ] || [ ! -d "$SDK_ROOT/build-tools" ]; then
  echo "Cannot locate the Android SDK build-tools (ANDROID_HOME/ANDROID_SDK_ROOT)"
  exit 1
fi
AAPT2="$SDK_ROOT/build-tools/$(ls -1 "$SDK_ROOT/build-tools" | sort -V | tail -n 1)/aapt2"

BADGING="$("$AAPT2" dump badging "$APK")"
PACKAGE="$(printf '%s\n' "$BADGING" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")"
ACTIVITY="$(printf '%s\n' "$BADGING" | sed -n "s/^launchable-activity: name='\([^']*\)'.*/\1/p")"

if [ -z "$PACKAGE" ] || [ -z "$ACTIVITY" ]; then
  echo "Could not read the package name or launchable activity from $APK"
  printf '%s\n' "$BADGING" | head -20
  exit 1
fi
echo "Package:  $PACKAGE"
echo "Activity: $ACTIVITY"

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
  if adb shell pm path "$PACKAGE" 2>/dev/null | tr -d '\r' | grep -q '^package:'; then
    echo "Package indexed."
    break
  fi
  sleep 2
done

if ! adb shell pm path "$PACKAGE" 2>/dev/null | tr -d '\r' | grep -q '^package:'; then
  echo "Package manager never listed $PACKAGE after a successful install"
  exit 1
fi

adb shell am start -W -n "$PACKAGE/$ACTIVITY"
adb shell pidof "$PACKAGE"
