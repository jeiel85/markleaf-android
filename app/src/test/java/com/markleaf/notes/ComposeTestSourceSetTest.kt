package com.markleaf.notes

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Keeps Compose UI tests out of the variant-agnostic `src/test` source set.
 *
 * Such tests need the `ComponentActivity` entry that `ui-test-manifest`
 * contributes only to the debug manifest, so a Compose test in `src/test`
 * compiles fine and then fails the release and benchmark unit-test variants —
 * a confusing failure far from its cause, which used to be papered over with a
 * hand-maintained exclusion list in `app/build.gradle.kts` (#152).
 *
 * Failing here instead turns that into a fast, local, self-explaining error.
 */
class ComposeTestSourceSetTest {
    @Test
    fun composeUiTestsLiveInTheDebugOnlySourceSet() {
        val sharedTests = File("src/test/java")
        assertTrue(
            "Expected to run with the app module as the working directory, " +
                "but ${sharedTests.absolutePath} does not exist",
            sharedTests.isDirectory
        )

        val offenders = sharedTests.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            // This file names the markers in order to search for them.
            .filterNot { it.name == "$SELF.kt" }
            .filter { file ->
                val source = file.readText()
                COMPOSE_TEST_MARKERS.any { marker -> source.contains(marker) }
            }
            .map { it.invariantSeparatorsPath }
            .sorted()
            .toList()

        assertTrue(
            buildString {
                appendLine("Compose UI tests must live in src/testDebug/java, not src/test/java.")
                appendLine("They need ui-test-manifest's debug-only ComponentActivity, so from")
                appendLine("src/test they break the release and benchmark unit-test variants.")
                appendLine("Move these files to the matching package under src/testDebug/java:")
                offenders.forEach { appendLine("  - $it") }
            },
            offenders.isEmpty()
        )
    }

    private companion object {
        const val SELF = "ComposeTestSourceSetTest"

        val COMPOSE_TEST_MARKERS = listOf(
            "createComposeRule",
            "createAndroidComposeRule",
            "captureRoboImage"
        )
    }
}
