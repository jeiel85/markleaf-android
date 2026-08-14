package com.markleaf.notes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Quantifies the preview parse cost on very long notes (#262).
 *
 * The preview re-parses the whole note on every text change while the preview
 * or outline is open (`EditorScreen` derives `previewLines` from
 * `editorState.text`), and the parser accumulates block source spans
 * (`IncludeSourceSpans.BLOCKS`) so a checkbox tap can find its source line.
 * That bookkeeping is fine for ordinary notes but was never quantified for
 * very long ones.
 *
 * This test measures the production parser on a 10,000-line note — far beyond
 * ordinary (most notes are well under 1,000 lines) — and pins a deliberately
 * generous upper bound. The bound exists to catch a quadratic blow-up (a
 * per-line scan over the whole document, say), not to benchmark the machine:
 * best-of-5 timing keeps CI noise from tripping it. The measured numbers are
 * printed so the cost stays visible in the test report.
 *
 * A macrobenchmark cannot isolate this: `MacrobenchmarkRule` measures whole-app
 * frames on a device, and seeding a 10k-line note into the app for it is
 * impractical. The parser is pure JVM, so a JVM test measures exactly the
 * production path and runs in CI.
 */
class PreviewParseCostTest {

    @Test
    fun veryLongNote_parsesWithinBudget() {
        val note = generateVeryLongNote(LINE_COUNT)

        // The reported number is only worth something if the fixture really is
        // that long. An earlier draft counted iterations rather than emitted
        // lines, so the fenced-code and table cases — three lines each — made a
        // "10,000-line" note 11,600 lines.
        assertEquals(
            "the fixture must be exactly the line count it reports",
            LINE_COUNT,
            note.count { it == '\n' }
        )

        // Warm up: class loading, extension init, JIT.
        SimpleMarkdownPreview.parse(note)

        val bestMs = (1..5).minOf {
            val start = System.nanoTime()
            val lines = SimpleMarkdownPreview.parse(note)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
            // Keep the result alive so the parse cannot be elided.
            check(lines.isNotEmpty())
            elapsedMs
        }

        println(
            "PreviewParseCost: $LINE_COUNT-line note parsed in " +
                "%.1f ms (best of 5)".format(bestMs)
        )

        assertTrue(
            "$LINE_COUNT-line preview parse took %.1f ms".format(bestMs),
            bestMs < BUDGET_MS
        )
    }

    /**
     * Emits exactly [lineCount] lines of mixed markdown. The count is of lines,
     * not of iterations: the fenced-code and table cases are three lines each,
     * and they are skipped when fewer than three remain, so the fixture ends on
     * the number it advertises rather than overshooting it.
     */
    private fun generateVeryLongNote(lineCount: Int): String {
        val sb = StringBuilder(lineCount * 48)
        var emitted = 0
        var i = 0
        while (emitted < lineCount) {
            val remaining = lineCount - emitted
            emitted += when {
                i % 25 == 0 -> sb.appendLine("# Heading $i").let { 1 }
                i % 25 == 1 -> sb.appendLine("## Subheading $i").let { 1 }
                i % 25 == 2 -> sb.appendLine("- [ ] task item $i").let { 1 }
                i % 25 == 3 -> sb.appendLine("- [x] done task $i").let { 1 }
                i % 25 == 4 -> sb.appendLine("- plain bullet $i").let { 1 }
                i % 25 == 5 -> sb.appendLine("1. ordered item $i").let { 1 }
                i % 25 == 6 -> sb.appendLine("> blockquote line $i").let { 1 }
                i % 25 == 7 && remaining >= 3 ->
                    sb.append("```kotlin\nval x = ").append(i).append("\n```\n").let { 3 }
                i % 25 == 8 && remaining >= 3 ->
                    sb.append("| a | b |\n|---|---|\n| ").append(i).append(" | x |\n").let { 3 }
                else -> sb.appendLine(
                    "A paragraph line with some **bold** and *italic* text $i"
                ).let { 1 }
            }
            i++
        }
        return sb.toString()
    }

    private companion object {
        const val LINE_COUNT = 10_000
        // Generous: catches quadratic blow-up, not machine speed. A 10k-line
        // note is an extreme; ordinary notes are < 1k lines.
        const val BUDGET_MS = 500.0
    }
}