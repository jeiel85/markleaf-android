package com.markleaf.notes

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Catches user-facing text that never became a string resource.
 *
 * [UntranslatedStringTest] and `ResourceParityTest` compare resources against
 * each other, so neither can see a literal that was never one. That blind spot
 * is why the sync relative-time labels — `방금 전`, `3시간 전` — sat in two
 * copies of a private helper and reached every locale, and why the Conflict
 * Center's `Updated:` prefix stayed English everywhere. It was the third
 * instance of the defect after the conflict-copy suffix in #217, and it was
 * found by accident while covering an unrelated screen (#255, #262).
 *
 * Android lint cannot help either: `HardcodedText` inspects XML layouts, and
 * this app's UI is entirely Compose.
 *
 * Two checks, aimed at the two ways the defect showed up:
 *
 *  - [sourceHoldsNoKoreanText] — Korean anywhere in a source literal. The
 *    project writes source in English, so Hangul in `src/main` means text that
 *    belongs in `values-ko/` leaked into code. This is the check that would
 *    have caught `formatRelative`, whose literals sat in a `when` branch and
 *    never went near a UI call.
 *  - [uiCallsTakeNoLiteralText] — a literal handed straight to a Compose call
 *    that renders or announces it.
 *
 * Both allowlists name real exceptions, and [allowlistEntriesAreStillNeeded]
 * fails once an entry stops matching, so neither can rot into a blanket
 * exemption that hides the next regression.
 */
class HardcodedStringTest {

    @Test
    fun sourceHoldsNoKoreanText() {
        val offenders = scan { _, literal -> HANGUL.containsMatchIn(literal) }
            .filterNot { it.allowedBy(KOREAN_IN_SOURCE) }

        assertTrue(
            buildString {
                appendLine("These source literals contain Korean.")
                appendLine("Source is written in English — move the text into strings.xml and")
                appendLine("translate it, or allowlist it here with a reason if it is not prose:")
                offenders.forEach { appendLine("  - $it") }
            },
            offenders.isEmpty()
        )
    }

    @Test
    fun uiCallsTakeNoLiteralText() {
        val offenders = scan { sink, literal -> sink != null && LETTER.containsMatchIn(literal) }
            .filterNot { it.allowedBy(LITERAL_IN_UI) }

        assertTrue(
            buildString {
                appendLine("These literals are passed straight to a Compose call that shows or")
                appendLine("announces them, so they cannot be translated. Use stringResource(),")
                appendLine("or allowlist them here if the text is not prose:")
                offenders.forEach { appendLine("  - $it") }
            },
            offenders.isEmpty()
        )
    }

    @Test
    fun allowlistEntriesAreStillNeeded() {
        val korean = scan { _, literal -> HANGUL.containsMatchIn(literal) }
        val ui = scan { sink, literal -> sink != null && LETTER.containsMatchIn(literal) }
        val stale = (KOREAN_IN_SOURCE + LITERAL_IN_UI).filterNot { entry ->
            (korean + ui).any { it.file == entry.first && it.literal == entry.second }
        }

        assertTrue(
            buildString {
                appendLine("These allowlist entries no longer match anything in the source.")
                appendLine("Remove them, so the list keeps naming real exceptions rather than")
                appendLine("silently exempting text that could regress later:")
                stale.forEach { appendLine("  - ${it.first}  \"${it.second}\"") }
            },
            stale.isEmpty()
        )
    }

    private data class Finding(val file: String, val line: Int, val literal: String) {
        fun allowedBy(allowlist: Set<Pair<String, String>>) = (file to literal) in allowlist
        override fun toString() = "$file:$line  \"$literal\""
    }

    /**
     * Every string literal in `src/main` matching [predicate], which receives the
     * UI call the literal was handed to (or null) and the literal itself.
     *
     * Comment lines are skipped: the codebase explains several Korean-facing
     * behaviours in Korean prose, and those explanations are not shipped text.
     *
     * A literal that starts its own line is matched against the previous line as
     * well: `Text(` and the text it renders are line-broken all over this
     * codebase, and a sink that only ever looked at one line would miss every
     * one of them.
     */
    private fun scan(predicate: (sink: String?, literal: String) -> Boolean): List<Finding> {
        val root = File("src/main/java")
        assertTrue(
            "Expected the app module as the working directory, but ${root.absolutePath} " +
                "does not exist",
            root.isDirectory
        )
        val findings = mutableListOf<Finding>()
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val relative = file.relativeTo(root).invariantSeparatorsPath
            // The last line that could have left a call open. Comment lines never
            // replace it, so a comment between the call and its text is harmless.
            var previous = ""
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trimStart()
                if (COMMENT_STARTS.any { trimmed.startsWith(it) }) return@forEachIndexed
                for (match in LITERAL.findAll(line)) {
                    val literal = match.groupValues[1]
                    // Up to, not including, the opening quote — the sink
                    // patterns anchor on the end of what precedes the literal.
                    // Nothing but indentation there means the call, if any, is
                    // still open from the line above.
                    val beforeLiteral = line.take(match.range.first)
                    val context =
                        if (beforeLiteral.isBlank()) previous + beforeLiteral else beforeLiteral
                    val sink = UI_SINKS.firstOrNull { it.toRegex().containsMatchIn(context) }
                    if (predicate(sink, literal)) {
                        findings += Finding(relative, index + 1, literal)
                    }
                }
                if (trimmed.isNotEmpty()) previous = line
            }
        }
        return findings.sortedBy { it.toString() }
    }

    private companion object {
        val HANGUL = Regex("[가-힣ㄱ-ㅎㅏ-ㅣ]")
        val LETTER = Regex("[A-Za-z가-힣]")

        /** A double-quoted literal with no escaped quotes or nesting to worry about. */
        val LITERAL = Regex("\"([^\"\\\\]*)\"")

        val COMMENT_STARTS = listOf("//", "*", "/*")

        /**
         * Compose calls that render or announce their argument. Deliberately
         * narrow: `label =` is not here, because Compose also uses it to name
         * animations for tooling — `Crossfade(label = "Editor preview mode")` is
         * a debugger string, not something a user ever sees.
         */
        val UI_SINKS = listOf(
            """\bText\(\s*$""",
            """\btext\s*=\s*$""",
            """\bcontentDescription\s*=\s*$"""
        )

        /**
         * The one place Korean belongs in source: the v15→v16 migration matches
         * conflict copies written before the suffix was translated, so it has to
         * spell the exact literal the old code wrote (#217).
         */
        val KOREAN_IN_SOURCE = setOf(
            "com/markleaf/notes/data/local/AppDatabase.kt" to
                "UPDATE `notes` SET `isConflictCopy` = 1 WHERE `title` LIKE '%(다른 기기 사본%'"
        )

        /**
         * Literals that render as-is in every language: Markdown syntax the
         * preview falls back to showing, a tag's `#` form, and the conventional
         * typographic symbol for text size.
         */
        val LITERAL_IN_UI = setOf(
            "com/markleaf/notes/core/markdown/preview/MarkdownPreviewList.kt" to
                "![\${line.text}](\$destination)",
            "com/markleaf/notes/core/markdown/preview/MarkdownPreviewList.kt" to
                "[^\${line.extra}]",
            "com/markleaf/notes/feature/editor/EditorFormattingControls.kt" to "Aa",
            "com/markleaf/notes/feature/editor/EditorSuggestions.kt" to "#\$tag"
        )
    }
}
