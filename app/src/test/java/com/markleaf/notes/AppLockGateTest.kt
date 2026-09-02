package com.markleaf.notes

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every activity a launcher or another app can start, and that can put a note's
 * text on screen, has to sit behind `BiometricLockGate`.
 *
 * `MainActivity` always did. `SingleNoteWidgetConfigureActivity` did not when it
 * was written: the launcher starts it directly when a widget is dropped, so with
 * app lock on it listed every note's title and excerpt without authentication,
 * and choosing one published that note's body to the home screen — a way past
 * the lock that did not involve the lock at all.
 *
 * Nothing else catches this. The gate is a composition, so leaving it out
 * compiles and renders perfectly well; the only signal is that it is missing.
 * Same shape as [PendingIntentFlagsTest] and [HardcodedStringTest], and for the
 * same reason.
 */
class AppLockGateTest {

    @Test
    fun everyExportedNoteShowingActivityIsGated() {
        val ungated = GATED_ACTIVITIES.filterNot { path ->
            File("src/main/java/$path").readText().callsGate()
        }.sorted()

        assertTrue(
            buildString {
                appendLine("These activities can show note content but do not wrap it in $GATE.")
                appendLine("An activity another app can start is a way around app lock unless it")
                appendLine("asks for authentication itself:")
                ungated.forEach { appendLine("  - $it") }
            },
            ungated.isEmpty()
        )
    }

    @Test
    fun theListItselfStillPointsAtRealFiles() {
        val missing = GATED_ACTIVITIES.filterNot { File("src/main/java/$it").isFile }

        assertTrue(
            "Renamed or moved — update GATED_ACTIVITIES: $missing",
            missing.isEmpty()
        )
    }

    /**
     * The gate has to be *called*, not merely imported. Written the obvious way
     * first, this test passed with the call deleted, because the leftover
     * `import …BiometricLockGate` still carried the name — so import lines are
     * dropped before looking, and the open paren is required.
     */
    private fun String.callsGate(): Boolean = lineSequence()
        .filterNot { it.trimStart().startsWith("import ") }
        .any { it.contains("$GATE(") }

    private companion object {
        const val GATE = "BiometricLockGate"

        /**
         * Kept by hand rather than discovered, because "can show note content"
         * is not something a scan can decide. Adding an activity that renders
         * notes means adding it here.
         */
        val GATED_ACTIVITIES = listOf(
            "com/markleaf/notes/MainActivity.kt",
            "com/markleaf/notes/widget/SingleNoteWidgetConfigureActivity.kt"
        )
    }
}
