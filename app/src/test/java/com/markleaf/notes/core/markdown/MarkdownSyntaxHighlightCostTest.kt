package com.markleaf.notes.core.markdown

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the live editor highlighter's two size guards and the cost of the scan
 * behind them (#437).
 *
 * The report that produced them: pasting ~200,000 characters froze the editor
 * for 8 seconds and left scrolling janky. Asked to retest with the highlighter
 * off, the reporter measured paste time on a *markdown* file growing far faster
 * than the file — 50k→~4 s, 100k→~11 s, 150k→~25 s, 200k→ANR — while a *prose*
 * file of the same lengths pasted in ~2 s at every size, with the setting making
 * no difference at all. Same characters, same 11 regex passes: the only variable
 * is how many matches the passes find, and so how many spans come out.
 *
 * That splits the problem in two, and this test covers the half that lives in
 * this file. The scan is linear and cheap (measured below, and pinned so it
 * stays that way). What is superlinear is the cost of the *result* downstream in
 * text layout, which this file can only avoid by not producing an unbounded
 * number of spans — hence [MarkdownSyntaxHighlighter.MAX_SPAN_COUNT], pinned
 * here on both sides of its boundary so the budget cannot drift or flip its
 * comparison unnoticed.
 *
 * The other half — scroll jank that persists with the highlighter off, on prose
 * with no spans at all — is the non-virtualized `BasicTextField`, and no budget
 * here touches it. It stays on the tracker (#262).
 *
 * `highlight` memoizes its last (text, colors, fontScale), so every repeated
 * measurement below passes a different `fontScale` to force a real recompute.
 * The scale only picks heading sizes; it changes no scanning work.
 */
class MarkdownSyntaxHighlightCostTest {

    private val colors = MarkdownSyntaxColors(
        heading = Color.Red,
        emphasis = Color.Blue,
        link = Color.Green,
        syntax = Color.Gray,
        checkbox = Color.Magenta,
        code = Color.Cyan,
        codeBlock = Color.Yellow,
        blockquote = Color.LightGray,
        horizontalRule = Color.Black
    )

    @Test
    fun anOrdinaryRichNote_isStyledExactlyAsBefore() {
        val note = markdownDoc(4_000)

        val result = MarkdownSyntaxHighlighter.highlight(note, colors)

        assertEquals(note, result.text)
        assertTrue(
            "an ordinary note must stay well inside the budget, not scrape it",
            result.spanStyles.size < MarkdownSyntaxHighlighter.MAX_SPAN_COUNT / 2
        )
        // The styles themselves are covered by MarkdownSyntaxHighlighterTest;
        // what matters here is only that the guard did not swallow them.
        assertTrue("a rich note must still be styled", result.spanStyles.isNotEmpty())
    }

    @Test
    fun atTheSpanBudget_theNoteIsStillStyled() {
        // Each `**bold**` line yields exactly three spans: the bold content and
        // the two muted `**` markers. Nothing else in the passes matches it, so
        // the fixture's span count is arithmetic rather than an estimate, and
        // the boundary can be pinned to the span instead of to a file size.
        val lines = MarkdownSyntaxHighlighter.MAX_SPAN_COUNT / SPANS_PER_BOLD_LINE
        val note = boldLines(lines)

        val result = MarkdownSyntaxHighlighter.highlight(note, colors, fontScale = 1.01f)

        // The most spans that still fit the budget, whatever the budget is set
        // to: the boundary is pinned relative to the constant, not to 1,500.
        assertTrue(
            "the fixture must fit the budget for the boundary to mean anything",
            lines * SPANS_PER_BOLD_LINE <= MarkdownSyntaxHighlighter.MAX_SPAN_COUNT
        )
        assertEquals(lines * SPANS_PER_BOLD_LINE, result.spanStyles.size)
        assertEquals(note, result.text)
    }

    @Test
    fun oneLinePastTheSpanBudget_theNoteIsHandedBackUnstyled() {
        val note = boldLines(MarkdownSyntaxHighlighter.MAX_SPAN_COUNT / SPANS_PER_BOLD_LINE + 1)

        val result = MarkdownSyntaxHighlighter.highlight(note, colors, fontScale = 1.02f)

        assertTrue(
            "past the budget the document carries no spans at all, not a truncated set",
            result.spanStyles.isEmpty()
        )
        // Dropping the styling must never drop a character: the editor edits
        // this text, so a lost character here is lost note content.
        assertEquals(note, result.text)
    }

    @Test
    fun atTheCharacterCap_aLightlyMarkedUpNoteIsStillStyled() {
        val note = paddedBoldNote(MarkdownSyntaxHighlighter.MAX_HIGHLIGHT_CHARS)

        val result = MarkdownSyntaxHighlighter.highlight(note, colors, fontScale = 1.03f)

        assertEquals(MarkdownSyntaxHighlighter.MAX_HIGHLIGHT_CHARS, note.length)
        assertTrue(
            "a long note under the span budget keeps its styling — length alone is not the cost",
            result.spanStyles.isNotEmpty()
        )
    }

    @Test
    fun oneCharacterPastTheCharacterCap_theNoteIsHandedBackUnstyled() {
        // Same note, one character longer: few enough spans to pass the span
        // budget, so only the character cap can be what silences it.
        val note = paddedBoldNote(MarkdownSyntaxHighlighter.MAX_HIGHLIGHT_CHARS + 1)

        val result = MarkdownSyntaxHighlighter.highlight(note, colors, fontScale = 1.04f)

        assertEquals(MarkdownSyntaxHighlighter.MAX_HIGHLIGHT_CHARS + 1, note.length)
        assertTrue(result.spanStyles.isEmpty())
        assertEquals(note, result.text)
    }

    @Test
    fun theScanStaysLinear_onTheLongestNoteItWillEverScan() {
        // The worst case the scan can be asked for: the largest document the
        // character cap still lets through, at the token density that produces
        // the most matches. Everything longer is rejected on length alone.
        val note = markdownDoc(MarkdownSyntaxHighlighter.MAX_HIGHLIGHT_CHARS)

        // Warm up: class loading, regex compilation, JIT.
        MarkdownSyntaxHighlighter.highlight(note, colors, fontScale = 0.9f)

        val bestMs = (1..5).minOf { iteration ->
            val scale = 1.1f + iteration
            val start = System.nanoTime()
            val result = MarkdownSyntaxHighlighter.highlight(note, colors, fontScale = scale)
            val elapsedMs = (System.nanoTime() - start) / 1_000_000.0
            // Keep the result alive so the call cannot be optimised away.
            check(result.text.length == note.length)
            elapsedMs
        }

        println(
            "MarkdownSyntaxHighlightCost: ${note.length}-char markdown scanned in " +
                "%.1f ms (best of 5)".format(bestMs)
        )

        // Generous on purpose. This is not a benchmark of the runner; it is a
        // tripwire for a pass that starts rescanning what it has already read
        // (the measured cost is ~10 ms on an ordinary machine).
        assertTrue(
            "scanning ${note.length} characters took %.1f ms".format(bestMs),
            bestMs < SCAN_BUDGET_MS
        )
    }

    private fun boldLines(count: Int): String =
        (1..count).joinToString(separator = "\n") { "**bold**" }

    /**
     * A note of exactly [length] characters carrying a handful of bold runs —
     * far too few spans to trip the span budget, so it isolates the character
     * cap. The padding is prose with no markdown tokens in it, which is also
     * the shape the reporter's control file had.
     */
    private fun paddedBoldNote(length: Int): String {
        val marked = boldLines(10)
        val padding = StringBuilder(length)
        while (padding.length < length - marked.length - 1) {
            padding.append("plain prose that carries no markdown tokens at all\n")
        }
        val note = StringBuilder(length)
            .append(marked)
            .append('\n')
            .append(padding)
        return note.substring(0, length)
    }

    /**
     * Markdown in the shape the reporter described: every syntax type present,
     * prose between the tokens, repeated to [targetChars].
     */
    private fun markdownDoc(targetChars: Int): String {
        val block = listOf(
            "# Section heading for the document",
            "",
            "Some ordinary prose that carries **bold text** and *italic text* and a",
            "[link to somewhere](https://example.com/page) plus `inline code` in it.",
            "",
            "## Subsection",
            "",
            "- [ ] An unchecked task item with a reasonable amount of text",
            "- [x] A checked task item that is already done",
            "",
            "> A blockquote line that explains something in a couple of clauses.",
            "> A second quoted line continuing the same thought.",
            "",
            "Prose with ~~struck text~~ and _underscore italics_ and more **bold**.",
            "",
            "```kotlin",
            "fun example(): Int = 42",
            "```",
            "",
            "---",
            "",
            "Another paragraph of plain prose so the document is not only syntax.",
            ""
        )
        val sb = StringBuilder(targetChars + 128)
        while (sb.length < targetChars) {
            block.forEach { sb.append(it).append('\n') }
        }
        return sb.substring(0, targetChars)
    }

    private companion object {
        const val SPANS_PER_BOLD_LINE = 3
        const val SCAN_BUDGET_MS = 400.0
    }
}
