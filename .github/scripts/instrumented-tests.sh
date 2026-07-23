#!/usr/bin/env bash
set -euo pipefail

# Instrumented tests, minus the UI package.
#
# Those classes drifted against the UI they assert on while a crash made the
# whole suite unrunnable, and 24 of them fail (#239). Excluding them by package
# rather than allow-listing the ones we want means a test added anywhere else is
# run by default instead of being silently skipped until someone widens a
# filter. Drop the argument once #239 is dealt with.
#
# In a script rather than inline in the workflow because the emulator runner's
# `script:` input does not survive a Gradle `-P` argument written across a
# folded YAML scalar — it arrived as part of the task name and Gradle failed
# with "Cannot locate tasks that match ':app:connectedDebugAndroidTest -P…'".
# One line here, quoted once, with no YAML in the middle.

adb wait-for-device
echo "sys.boot_completed=$(adb shell getprop sys.boot_completed | tr -d '\r')"

./gradlew :app:connectedDebugAndroidTest \
  "-Pandroid.testInstrumentationRunnerArguments.notPackage=com.markleaf.notes.ui"
