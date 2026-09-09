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

# The wait above is necessary and not sufficient. `am start` can still answer
# "Error type 3 / Activity class ... does not exist" after `pm path` has already
# said yes: that query is satisfied once the APK path is registered, which is
# earlier than the package manager's component index being complete. It is the
# same post-install window, one step further along.
#
# Three runs of #376 failed exactly there -- install reported Success, this
# script printed "Package indexed.", and `am start` then said the activity did
# not exist -- on a runner whose emulator took six minutes to boot, while
# `instrumented-tests` installed and drove the same build on the same commit.
# A fourth run passed with no relevant change in between.
#
# Retrying the command that failed is deliberate. Waiting on some other query
# instead would need an assumption about which one is authoritative for
# component resolution, and the wrong choice is how the current guard came to
# report success before `am start` could work. Retrying `am start` needs only
# that this particular error is transient, and it is checked for by name:
# anything else the command says is a real failure and stops immediately, so a
# genuinely broken build still fails on the first attempt rather than after a
# minute of retries.
START_ATTEMPTS=15
for attempt in $(seq 1 "$START_ATTEMPTS"); do
  set +e
  start_output="$(adb shell am start -W -n "$PACKAGE/$ACTIVITY" 2>&1)"
  start_status=$?
  set -e
  printf '%s\n' "$start_output"

  if ! printf '%s\n' "$start_output" | grep -q 'does not exist'; then
    if [ "$start_status" -ne 0 ]; then
      echo "am start failed (exit $start_status) for a reason other than the post-install window"
      exit 1
    fi
    break
  fi

  if [ "$attempt" -eq "$START_ATTEMPTS" ]; then
    echo "The activity was still unresolvable after $START_ATTEMPTS attempts."
    echo "That is longer than the post-install window has ever taken; treat it as a real failure."
    exit 1
  fi

  echo "Activity not resolvable yet -- retrying ($attempt/$START_ATTEMPTS)"
  sleep 2
done

# The assertion the whole job exists for: the process is alive after the launch.
# `set -e` fails the script when pidof prints nothing.
adb shell pidof "$PACKAGE"
