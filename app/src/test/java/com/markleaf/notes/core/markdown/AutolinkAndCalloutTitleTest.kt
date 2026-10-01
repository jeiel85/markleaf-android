package com.markleaf.notes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GFM autolinks and callout head lines (M2 of `docs/MARKDOWN_SYNTAX_PLAN.md`).
 *
 * Before: a bare `https://…` was plain text with nothing to tap, the words
 * after `[!TIP]` were pushed into the body, and an Obsidian fold marker
 * leaked into the body as a stray `-`.
 */
class AutolinkAndCalloutTitleTest {

    private fun parse(markdown: String) = SimpleMarkdownPreview.parse(markdown)

    private fun links(markdown: String) = parse(markdown).flatMap { it.segments }
        .filter { it.type == PreviewInlineType.LINK }

    // ---- autolink ------------------------------------------------------------

    @Test
    fun aBareUrlIsALink() {
        val link = links("see https://example.com/path?q=1 now").single()

        assertEquals("https://example.com/path?q=1", link.text)
        assertEquals("https://example.com/path?q=1", link.href)
    }

    @Test
    fun aWwwAddressIsALink() {
        val link = links("visit www.example.com today").single()

        assertEquals("www.example.com", link.text)
        assertEquals("http://www.example.com", link.href)
    }

    @Test
    fun anEmailAddressIsAMailtoLink() {
        assertEquals("mailto:me@example.com", links("mail me@example.com").single().href)
    }

    @Test
    fun trailingPunctuationStaysOutOfTheLink() {
        assertEquals("https://example.com", links("Go to https://example.com.").single().href)
    }

    @Test
    fun aUrlInsideCodeIsNotALink() {
        val segments = parse("run `curl https://example.com` first").single().segments

        assertTrue(segments.none { it.type == PreviewInlineType.LINK })
        assertEquals("curl https://example.com", segments.single { it.type == PreviewInlineType.INLINE_CODE }.text)
    }

    @Test
    fun aWrittenLinkKeepsItsOwnLabel() {
        val link = links("[docs](https://example.com)").single()

        assertEquals("docs", link.text)
    }

    @Test
    fun aBareUrlInATableCellIsALink() {
        val cell = parse("| a |\n|---|\n| https://example.com |").single().tableData!!.rowSegments[0][0]

        assertEquals("https://example.com", cell.single().href)
    }

    // ---- callout heads ---------------------------------------------------------

    @Test
    fun wordsAfterTheHeadAreTheTitle() {
        val lines = parse("> [!TIP] Custom title\n> body")

        assertEquals(PreviewLineType.CALLOUT, lines[0].type)
        assertEquals("TIP", lines[0].extra)
        assertEquals("Custom title", lines[0].text)
        assertEquals("body", lines[1].text)
        assertEquals(2, lines.size)
    }

    @Test
    fun aFormattedTitleIsReadAsItsWords() {
        assertEquals("Big idea", parse("> [!TIP] **Big** idea\n> body")[0].text)
    }

    @Test
    fun aFoldMarkerDoesNotLeakIntoTheBody() {
        val lines = parse("> [!NOTE]-\n> folded body")

        assertEquals("", lines[0].text)
        assertEquals("folded body", lines[1].text)
    }

    @Test
    fun aFoldMarkerBeforeATitleIsDropped() {
        assertEquals("Open title", parse("> [!NOTE]+ Open title\n> body")[0].text)
    }

    @Test
    fun anUntitledCalloutHasNoTitle() {
        assertEquals("", parse("> [!WARNING]\n> careful")[0].text)
    }

    @Test
    fun aTitleOnlyCalloutIsOneRow() {
        val lines = parse("> [!IMPORTANT] Just a title")

        assertEquals(1, lines.size)
        assertEquals("Just a title", lines[0].text)
        assertTrue(lines[0].calloutEnd)
    }

    // ---- limits ----------------------------------------------------------------

    /**
     * commonmark 0.30 aborts a table past a million cells. The preview shows
     * the note's lines instead of failing.
     */
    @Test
    fun aTablePastTheCellLimitDegradesToText() {
        val columns = 1_000
        val row = "|" + "x|".repeat(columns)
        val markdown = buildString {
            append(row).append('\n')
            append("|").append("-|".repeat(columns)).append('\n')
            repeat(1_001) { append(row).append('\n') }
        }

        val lines = parse(markdown)

        assertTrue(lines.isNotEmpty())
        assertTrue(lines.all { it.type == PreviewLineType.BODY })
    }
}
