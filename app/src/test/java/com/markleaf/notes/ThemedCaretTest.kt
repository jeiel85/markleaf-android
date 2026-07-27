package com.markleaf.notes

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fails if a `BasicTextField` in `src/main` never says what colour its caret is.
 *
 * Compose defaults `cursorBrush` to `SolidColor(Color.Black)`, a literal that no
 * theme touches. The note editor is the app's only raw foundation text field —
 * every Material field takes its cursor colour from the theme — so it drew a
 * black caret on a dark background, all but invisible in both Markleaf Green and
 * Material You, while the drag handle right beside it was themed (#283).
 *
 * Nothing else in the suite can see this: the caret blinks, so a Roborazzi
 * golden cannot assert it, `HardcodedStringTest` looks at text rather than
 * colour, and lint has no opinion about a Compose default. What this checks is
 * only that the colour is stated at the call site; whether it contrasts is a
 * judgement the theme makes.
 */
class ThemedCaretTest {

    @Test
    fun everyBasicTextFieldSetsItsCursorBrush() {
        val root = File("src/main/java")
        assertTrue(
            "Expected the app module as the working directory, but ${root.absolutePath} " +
                "does not exist",
            root.isDirectory
        )

        val callSites = mutableListOf<Pair<String, String>>()
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if (line.trimStart().startsWith("//")) return@forEachIndexed
                val opening = CALL.find(line) ?: return@forEachIndexed
                callSites += "$relative:${index + 1}" to callText(lines, index, opening.range.last)
            }
        }

        // A scan that matches nothing passes for the wrong reason — the editor
        // could move to another text field API and take the blind spot with it.
        assertTrue(
            "Expected at least one BasicTextField call site in src/main, found none. " +
                "If the editor no longer uses one, this test needs rewriting rather " +
                "than deleting: the caret still has to come from the theme.",
            callSites.isNotEmpty()
        )

        val offenders = callSites.filterNot { (_, call) -> "cursorBrush" in call }.map { it.first }
        assertTrue(
            buildString {
                appendLine("These BasicTextField call sites do not set cursorBrush, so the caret")
                appendLine("falls back to Compose's opaque black and ignores the theme (#283).")
                appendLine("Pass a brush built from MaterialTheme.colorScheme:")
                offenders.forEach { appendLine("  - $it") }
            },
            offenders.isEmpty()
        )
    }

    /**
     * The source of the call that opens at [column] of line [start], up to its
     * closing parenthesis. Parentheses inside string and character literals are
     * ignored, as are trailing line comments, so a `")"` in an argument cannot
     * end the call early.
     */
    private fun callText(lines: List<String>, start: Int, column: Int): String {
        val text = StringBuilder()
        var depth = 0
        for (index in start until lines.size) {
            val line = lines[index]
            text.appendLine(line)
            val counted = if (index == start) line.substring(column) else line
            for (char in strippedOfLiterals(counted)) {
                when (char) {
                    '(' -> depth++
                    ')' -> depth--
                }
            }
            if (depth <= 0) break
        }
        return text.toString()
    }

    /** Literals go first: a `"https://…"` argument must not read as a comment. */
    private fun strippedOfLiterals(line: String) =
        LITERAL.replace(line, "").substringBefore("//")

    private companion object {
        /** The `(` at the end of the match is where the argument list opens. */
        val CALL = Regex("""\bBasicTextField\s*\(""")

        /** String and character literals, escapes included, so `"\")"` is inert. */
        val LITERAL = Regex(""""(\\.|[^"\\])*"|'(\\.|[^'\\])*'""")
    }
}
