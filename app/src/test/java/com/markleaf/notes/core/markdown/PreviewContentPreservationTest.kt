package com.markleaf.notes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Nothing written in a note may vanish from its preview.
 *
 * Each case here rendered as fewer rows than the note holds before this was
 * fixed: a fenced block inside a quote or callout rendered as nothing (it has
 * no text node to collect), a `<div>` block was dropped, `<br>` ran two lines
 * together, an image beside text kept only its alt text, and a list inside a
 * quote collapsed to one row `ab`.
 */
class PreviewContentPreservationTest {

    private fun parse(markdown: String) = SimpleMarkdownPreview.parse(markdown)

    private fun PreviewLine.drawn() = segments.joinToString("") { it.text }.ifEmpty { text }

    // ---- quotes ------------------------------------------------------------

    @Test
    fun aFencedBlockInsideAQuoteIsKept() {
        val lines = parse("> ```\n> code line\n> ```")

        assertEquals(1, lines.size)
        assertEquals(PreviewLineType.CODE_BLOCK, lines[0].type)
        assertEquals("code line", lines[0].text)
        assertEquals(1, lines[0].quoteDepth)
    }

    @Test
    fun aListInsideAQuoteKeepsOneRowPerItem() {
        val lines = parse("> - a\n> - b")

        assertEquals(listOf(PreviewLineType.BULLET, PreviewLineType.BULLET), lines.map { it.type })
        assertEquals(listOf("a", "b"), lines.map { it.text })
        assertTrue(lines.all { it.quoteDepth == 1 })
    }

    @Test
    fun aHeadingInsideAQuoteStaysAHeading() {
        val lines = parse("> # Title\n> body")

        assertEquals(PreviewLineType.H1, lines[0].type)
        assertEquals(PreviewLineType.BLOCKQUOTE, lines[1].type)
        assertTrue(lines.all { it.quoteDepth == 1 })
    }

    @Test
    fun aNestedQuoteIsOneLevelDeeper() {
        val lines = parse("> a\n>\n>> b")

        assertEquals(listOf("a", "b"), lines.map { it.text })
        assertEquals(listOf(1, 2), lines.map { it.quoteDepth })
        assertTrue(lines.all { it.type == PreviewLineType.BLOCKQUOTE })
    }

    @Test
    fun aQuoteInsideAListItemStartsAtTheItemsIndent() {
        val lines = parse("- item\n\n  > quoted")

        val quoted = lines.single { it.text == "quoted" }
        assertEquals(1, quoted.depth)
        assertEquals(1, quoted.containerDepth)
        assertEquals(1, quoted.quoteDepth)
    }

    @Test
    fun quotesNestedPastTheCeilingAreCutNotCrashed() {
        val lines = parse(">".repeat(CommonMarkPreviewAdapter.MAX_BLOCK_DEPTH * 2) + " deep")

        assertEquals(CommonMarkPreviewAdapter.DEPTH_CUT_MARKER, lines.last().text)
    }

    // ---- callouts ----------------------------------------------------------

    @Test
    fun aCalloutBodyKeepsItsLinksBoldAndLists() {
        val lines = parse(
            "> [!NOTE]\n> see [link](https://example.com) and **bold**\n>\n> - one\n> - two"
        )

        assertEquals(PreviewLineType.CALLOUT, lines[0].type)
        assertEquals("NOTE", lines[0].extra)
        val body = lines[1]
        assertEquals(PreviewLineType.BODY, body.type)
        assertEquals("https://example.com", body.segments.single { it.type == PreviewInlineType.LINK }.href)
        assertEquals("bold", body.segments.single { it.type == PreviewInlineType.BOLD }.text)
        assertEquals(listOf("one", "two"), lines.drop(2).map { it.text })
        assertTrue(lines.drop(2).all { it.type == PreviewLineType.BULLET })
        assertTrue(lines.all { it.callout == "NOTE" })
        assertTrue(lines.all { it.quoteDepth == 0 })
        assertEquals(listOf(false, false, false, true), lines.map { it.calloutEnd })
    }

    @Test
    fun aFencedBlockInsideACalloutIsKept() {
        val lines = parse("> [!TIP]\n> ```kotlin\n> val x = 1\n> ```")

        assertEquals(listOf(PreviewLineType.CALLOUT, PreviewLineType.CODE_BLOCK), lines.map { it.type })
        assertEquals("val x = 1", lines[1].text)
        assertEquals("kotlin", lines[1].extra)
        assertEquals("TIP", lines[1].callout)
    }

    @Test
    fun aCalloutWithNoBodyIsItsOwnLastRow() {
        val lines = parse("> [!WARNING]")

        assertEquals(1, lines.size)
        assertEquals(PreviewLineType.CALLOUT, lines[0].type)
        assertTrue(lines[0].calloutEnd)
    }

    @Test
    fun anUnknownCalloutTypeIsStillACallout() {
        val lines = parse("> [!info]\n> body")

        assertEquals(PreviewLineType.CALLOUT, lines[0].type)
        assertEquals("info", lines[0].extra)
        assertEquals("body", lines[1].drawn())
    }

    @Test
    fun aQuoteThatOnlyMentionsACalloutLaterIsAPlainQuote() {
        val lines = parse("> text [!NOTE]")

        assertEquals(PreviewLineType.BLOCKQUOTE, lines.single().type)
        assertNull(lines.single().callout)
    }

    // ---- front matter ----------------------------------------------------------

    @Test
    fun anOpeningRuleThatNothingClosesIsARuleNotFrontMatter() {
        // The front-matter extension read everything after an unclosed `---`
        // as metadata and kept only `key: value` lines, so this note rendered
        // empty (review of #491).
        val lines = parse("---\nJust some prose\n\nand more")

        assertEquals(PreviewLineType.HORIZONTAL_RULE, lines[0].type)
        assertEquals(listOf("Just some prose", "and more"), lines.drop(1).map { it.text })
    }

    @Test
    fun aNoteThatIsOnlyARuleIsARule() {
        assertEquals(listOf(PreviewLineType.HORIZONTAL_RULE), parse("---").map { it.type })
    }

    @Test
    fun closedFrontMatterIsStillFrontMatter() {
        val lines = parse("---\ntitle: Hello\n---\n\nbody")

        assertEquals(PreviewLineType.FRONTMATTER, lines[0].type)
        assertEquals("body", lines[1].text)
    }

    @Test
    fun frontMatterClosedByDotsIsStillFrontMatter() {
        assertEquals(PreviewLineType.FRONTMATTER, parse("---\ntitle: Hello\n...\n\nbody")[0].type)
    }

    // ---- HTML --------------------------------------------------------------

    @Test
    fun anHtmlBlockShowsItsText() {
        val lines = parse("<div align=\"center\">\nHello <b>world</b> &amp; more\n</div>")

        assertEquals(listOf("Hello world & more"), lines.map { it.text })
        assertEquals(PreviewLineType.BODY, lines.single().type)
    }

    @Test
    fun htmlLineBreaksAndBlockEndsBreakTheText() {
        val lines = parse("<p>first<br>second</p>\n<p>third</p>")

        assertEquals("first\nsecond\nthird", lines.single().text)
    }

    @Test
    fun anHtmlCommentScriptAndStyleStayHidden() {
        val lines = parse(
            "before\n\n<!-- hidden note -->\n\n<script>alert(1)</script>\n\n<style>p { color: red }</style>\n\nafter"
        )

        assertEquals(listOf("before", "after"), lines.map { it.text })
    }

    @Test
    fun anHtmlImageBlockIsAnImageRow() {
        val lines = parse("<img src=\"attachments/a.png\" alt=\"A cat\" width=\"300\">")

        val image = lines.single()
        assertEquals(PreviewLineType.IMAGE, image.type)
        assertEquals("attachments/a.png", image.extra)
        assertEquals("A cat", image.text)
    }

    @Test
    fun aDataSrcAttributeIsNotTheImageSource() {
        val lines = parse("<img data-src=\"wrong.png\" src=\"right.png\">")

        assertEquals("right.png", lines.single().extra)
    }

    @Test
    fun inlineBrBreaksTheLine() {
        val line = parse("a<br>b").single()

        assertEquals("a\nb", line.drawn())
        assertEquals("a b", line.text)
    }

    @Test
    fun brInsideATableCellBreaksTheCell() {
        val table = parse("| a | b |\n|---|---|\n| l1<br>l2 | c |").single().tableData!!

        assertEquals("l1 l2", table.rows[0][0])
        assertEquals("l1\nl2", table.rowSegments[0][0].joinToString("") { it.text })
    }

    @Test
    fun otherInlineTagsKeepTheirText() {
        val line = parse("press <kbd>Ctrl</kbd> now").single()

        assertEquals("press Ctrl now", line.drawn())
    }

    // ---- images --------------------------------------------------------------

    @Test
    fun anImageBesideTextSplitsTheParagraph() {
        val lines = parse("text ![alt](a.png) more")

        assertEquals(
            listOf(PreviewLineType.BODY, PreviewLineType.IMAGE, PreviewLineType.BODY),
            lines.map { it.type }
        )
        assertEquals(listOf("text", "alt", "more"), lines.map { it.text })
        assertEquals("a.png", lines[1].extra)
    }

    @Test
    fun anImageOnItsOwnLineLeavesNoBlankRows() {
        val lines = parse("intro\n![a](x.png)\noutro")

        assertEquals(listOf("intro", "a", "outro"), lines.map { it.text })
        assertTrue(lines.none { it.drawn().isBlank() && it.type != PreviewLineType.IMAGE })
    }

    @Test
    fun anInlineImgTagSplitsTheParagraphToo() {
        val lines = parse("before <img src=\"p.png\" alt=\"pic\"> after")

        assertEquals(listOf(PreviewLineType.BODY, PreviewLineType.IMAGE, PreviewLineType.BODY), lines.map { it.type })
        assertEquals("p.png", lines[1].extra)
    }

    @Test
    fun anImageAloneInAListItemGoesUnderTheItem() {
        val lines = parse("- ![alt](a.png)")

        assertEquals(listOf(PreviewLineType.BULLET, PreviewLineType.IMAGE), lines.map { it.type })
        assertEquals("", lines[0].text)
        assertEquals(1, lines[1].depth)
    }

    @Test
    fun anImageInsideAListItemKeepsTheItemsText() {
        val lines = parse("- see ![a](x.png) here")

        assertEquals(listOf("see", "a", "here"), lines.map { it.text })
        assertEquals(listOf(0, 1, 1), lines.map { it.depth })
        assertEquals(PreviewLineType.BULLET, lines[0].type)
    }

    @Test
    fun aLinkedImageStaysALink() {
        val line = parse("[![badge](b.png)](https://example.com)").single()

        assertEquals(PreviewLineType.BODY, line.type)
        assertEquals("https://example.com", line.segments.single().href)
    }

    @Test
    fun aParagraphWithoutImagesIsUnchanged() {
        val line = parse("plain **bold** text").single()

        assertEquals("plain bold text", line.text)
        assertEquals(PreviewLineType.BODY, line.type)
    }
}
