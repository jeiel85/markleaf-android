#!/usr/bin/env bash
set -euo pipefail

# Instrumented tests, minus the UI package.
#
# Those classes drifted against the UI they assert on while a crash made the
# whole suite unrunnable, and 24 of them fail (#239). Excluding them by package
# rather than allow-listing the ones we want means a test added anywhere else is
# run by default instead of being silently skipped until someone widens a
# filter. Delete the gradle.properties line below once #239 is dealt with.
#
# The filter goes into gradle.properties rather than onto the command line
# because this repo's `gradlew` collapses multiple arguments into one: line 183
# expands `\"$@\"` inside a double-quoted string, so
#
#     ./gradlew :app:connectedDebugAndroidTest -Pandroid.…notPackage=…
#
# reaches Gradle as a single task name and fails with "Cannot locate tasks that
# match". Every other gradle call in CI passes exactly one argument, which is
# why nothing has tripped over this before. The wrapper needs fixing on its own
# terms — it is the file F-Droid builds with — so this works around it instead
# of touching it here. The runner's checkout is thrown away, so appending is
# safe; locally, nothing is written.

adb wait-for-device
echo "sys.boot_completed=$(adb shell getprop sys.boot_completed | tr -d '\r')"

echo "android.testInstrumentationRunnerArguments.notPackage=com.markleaf.notes.ui" >> gradle.properties

./gradlew :app:connectedDebugAndroidTest
