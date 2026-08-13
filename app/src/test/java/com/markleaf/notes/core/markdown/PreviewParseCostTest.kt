package com.markleaf.notes.core.markdown

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

    private fun generateVeryLongNote(lineCount: Int): String {
        val sb = StringBuilder(lineCount * 48)
        var line = 0
        while (line < lineCount) {
            when (line % 25) {
                0 -> sb.append("# Heading ").append(line).append('\n')
                1 -> sb.append("## Subheading ").append(line).append('\n')
                2 -> sb.append("- [ ] task item ").append(line).append('\n')
                3 -> sb.append("- [x] done task ").append(line).append('\n')
                4 -> sb.append("- plain bullet ").append(line).append('\n')
                5 -> sb.append("1. ordered item ").append(line).append('\n')
                6 -> sb.append("> blockquote line ").append(line).append('\n')
                7 -> sb.append("```kotlin\nval x = ").append(line).append("\n```\n")
                8 -> sb.append("| a | b |\n|---|---|\n| ").append(line).append(" | x |\n")
                else -> sb.append("A paragraph line with some **bold** and *italic* text ")
                    .append(line).append('\n')
            }
            line++
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