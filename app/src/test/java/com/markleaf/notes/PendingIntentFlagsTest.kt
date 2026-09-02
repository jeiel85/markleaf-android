package com.markleaf.notes

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Catches a `PendingIntent` built with both `FLAG_IMMUTABLE` and `FLAG_MUTABLE`.
 *
 * Android 12+ rejects the pair with `IllegalArgumentException`, and the widget's
 * row template carried it from v2.16.0 through v2.35.0 — 44 releases. The throw
 * landed inside `onUpdate`, so the receiver died before the widget was ever
 * populated: the recent-notes list sat on the home screen as an empty card with
 * a dead "+" button, and every launcher refresh crashed the app.
 *
 * Nothing else sees it. The flags are plain ints, so the pair compiles; lint has
 * no rule for it; and a Robolectric test does not help either — `ShadowPendingIntent`
 * records the flags without running the platform's check, so
 * [com.markleaf.notes.widget.QuickNoteWidgetTest] passes with the defect present.
 * That leaves reading the source, which is what this does — the same shape as
 * [HardcodedStringTest], and for the same reason.
 */
class PendingIntentFlagsTest {

    @Test
    fun noPendingIntentAsksForBothMutabilities() {
        val offenders = sourceFiles().mapNotNull { file ->
            val calls = pendingIntentCalls(withoutComments(file.readText()))
            val bad = calls.filter { it.contains(IMMUTABLE) && it.contains(MUTABLE) }
            if (bad.isEmpty()) null else "${file.invariantSeparatorsPath}: ${bad.size} call(s)"
        }.sorted()

        assertTrue(
            buildString {
                appendLine("These PendingIntent calls pass FLAG_IMMUTABLE and FLAG_MUTABLE together.")
                appendLine("Android 12+ throws IllegalArgumentException for the pair — pick one:")
                appendLine("  mutable for a RemoteViews template that a fill-in completes,")
                appendLine("  immutable for everything else.")
                offenders.forEach { appendLine("  - $it") }
            },
            offenders.isEmpty()
        )
    }

    private fun sourceFiles(): List<File> {
        val main = File("src/main/java")
        assertTrue(
            "Expected to run with the app module as the working directory, " +
                "but ${main.absolutePath} does not exist",
            main.isDirectory
        )
        return main.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    /**
     * The argument list of every `PendingIntent.getX(...)` call, so a flag named
     * elsewhere in the same file — a constant, a comment's prose — cannot be read
     * as part of one.
     */
    private fun pendingIntentCalls(source: String): List<String> = buildList {
        var index = source.indexOf(CALL_PREFIX)
        while (index >= 0) {
            val open = source.indexOf('(', index)
            if (open < 0) return@buildList
            var depth = 0
            var cursor = open
            while (cursor < source.length) {
                when (source[cursor]) {
                    '(' -> depth++
                    ')' -> {
                        depth--
                        if (depth == 0) break
                    }
                }
                cursor++
            }
            if (cursor >= source.length) return@buildList
            add(source.substring(open, cursor))
            index = source.indexOf(CALL_PREFIX, cursor)
        }
    }

    /** Line and block comments, so the explanation of the defect is not the defect. */
    private fun withoutComments(source: String): String =
        source.replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

    private companion object {
        const val CALL_PREFIX = "PendingIntent.get"
        const val IMMUTABLE = "FLAG_IMMUTABLE"
        const val MUTABLE = "FLAG_MUTABLE"
        val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("""//[^\n]*""")
    }
}
