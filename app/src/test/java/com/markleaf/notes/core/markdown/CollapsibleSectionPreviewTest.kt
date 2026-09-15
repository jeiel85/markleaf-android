package com.markleaf.notes.core.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * #403: `<details>` / `<summary>` collapsible sections. CommonMark already
 * recognises both as block-level HTML tags, so parsing them is not new — what
 * is new is [CommonMarkPreviewAdapter] no longer silently dropping the
 * resulting `HtmlBlock` nodes, and pairing an opening tag with its close
 * across however many sibling rows sit in between.
 */
class CollapsibleSectionPreviewTest {
    @Test
    fun detailsWithBlankLineGetsFullBlockRenderingInside() {
        // GitHub's own documented form: a blank line after <summary> is what
        // lets the body parse as real Markdown blocks instead of raw HTML text.
        val markdown = """
            <details>
            <summary>Click to expand</summary>

            # Heading inside
            Body paragraph.
            </details>
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(
            listOf(
                PreviewLineType.COLLAPSIBLE_SUMMARY,
                PreviewLineType.H1,
                PreviewLineType.BODY
            ),
            lines.map { it.type }
        )
        val summary = lines[0]
        assertEquals("Click to expand", summary.text)
        assertNull("no `open` attribute means it starts collapsed", summary.extra)
        assertEquals(0, summary.collapsibleId)
        // The heading and body both sit inside the section.
        assertEquals(listOf(0), lines[1].collapsibleIds)
        assertEquals(listOf(0), lines[2].collapsibleIds)
        assertEquals("Heading inside", lines[1].text)
        assertEquals("Body paragraph.", lines[2].text)
    }

    @Test
    fun detailsOpenAttributeIsCarriedOnTheSummaryRow() {
        val markdown = "<details open>\n<summary>Shown already</summary>\n\nBody\n</details>"

        val summary = SimpleMarkdownPreview.parse(markdown).first()

        assertEquals(PreviewLineType.COLLAPSIBLE_SUMMARY, summary.type)
        assertEquals(CommonMarkPreviewAdapter.OPEN_MARKER, summary.extra)
    }

    @Test
    fun missingSummaryTagLeavesTheTitleBlankForTheRendererToFillIn() {
        // GitHub falls back to showing the literal word "Details" — this app
        // localizes that fallback in MarkdownPreviewList instead of baking an
        // English string into the parser, so an absent <summary> parses to an
        // empty title rather than the word itself.
        val markdown = "<details>\n\nJust a body, no summary tag.\n</details>"

        val summary = SimpleMarkdownPreview.parse(markdown).first()

        assertEquals(PreviewLineType.COLLAPSIBLE_SUMMARY, summary.type)
        assertTrue(summary.text.isEmpty())
    }

    @Test
    fun selfContainedDetailsWithNoBlankLineStillRendersItsBody() {
        // The exact shape from #403's own report: no blank line anywhere, so
        // commonmark hands the whole run to one HtmlBlock instead of splitting
        // it into separate sibling nodes.
        val markdown = "<details>\n<summary>Title</summary>\nText body\n</details>"

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(listOf(PreviewLineType.COLLAPSIBLE_SUMMARY, PreviewLineType.BODY), lines.map { it.type })
        assertEquals("Title", lines[0].text)
        assertEquals("Text body", lines[1].text)
        assertEquals(listOf(lines[0].collapsibleId), lines[1].collapsibleIds)
    }

    @Test
    fun selfContainedDetailsBodyStillGetsInlineFormatting() {
        val markdown = "<details><summary>T</summary>a **bold** word</details>"

        val body = SimpleMarkdownPreview.parse(markdown)[1]

        assertEquals(PreviewLineType.BODY, body.type)
        assertTrue(body.segments.any { it.type == PreviewInlineType.BOLD && it.text == "bold" })
    }

    @Test
    fun nestedDetailsBothGetDistinctIdsAndTheInnerCarriesBoth() {
        val markdown = """
            <details>
            <summary>Outer</summary>

            <details>
            <summary>Inner</summary>

            Innermost text.
            </details>
            </details>
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        val outer = lines.first { it.text == "Outer" }
        val inner = lines.first { it.text == "Inner" }
        val innermost = lines.first { it.text == "Innermost text." }
        assertTrue("nested sections must not share an id", outer.collapsibleId != inner.collapsibleId)
        // The inner summary row itself sits inside the outer section...
        assertEquals(listOf(outer.collapsibleId), inner.collapsibleIds)
        // ...and its body sits inside both, outermost first.
        assertEquals(listOf(outer.collapsibleId, inner.collapsibleId), innermost.collapsibleIds)
    }

    @Test
    fun adjacentSectionsWithNoBlankLineBetweenThemStayIndependent() {
        // A Codex finding on the PR: without a blank line between the first
        // </details> and the second <details>, commonmark folds the close and
        // the next open into one HtmlBlock. A version of the parser that only
        // asked "is there an opening tag anywhere in here" skipped over that
        // leading close, so the second section ended up nested inside the
        // first instead of standing beside it.
        val markdown = """
            <details>
            <summary>First</summary>

            first body
            </details>
            <details>
            <summary>Second</summary>

            second body
            </details>
        """.trimIndent()

        val lines = SimpleMarkdownPreview.parse(markdown)

        val first = lines.first { it.text == "First" }
        val second = lines.first { it.text == "Second" }
        val firstBody = lines.first { it.text == "first body" }
        val secondBody = lines.first { it.text == "second body" }
        assertTrue("adjacent sections must not share an id", first.collapsibleId != second.collapsibleId)
        assertEquals("Second must not be nested inside First", emptyList<Int>(), second.collapsibleIds)
        assertEquals(listOf(first.collapsibleId), firstBody.collapsibleIds)
        assertEquals(listOf(second.collapsibleId), secondBody.collapsibleIds)
    }

    @Test
    fun unclosedDetailsRunsToEndOfDocumentInsteadOfCrashing() {
        val markdown = "<details>\n<summary>Never closed</summary>\n\nOne\n\nTwo"

        val lines = SimpleMarkdownPreview.parse(markdown)

        val summary = lines.first()
        assertEquals(PreviewLineType.COLLAPSIBLE_SUMMARY, summary.type)
        assertTrue(lines.drop(1).all { it.collapsibleIds == listOf(summary.collapsibleId) })
    }

    @Test
    fun strayClosingTagWithNoOpenIsIgnored() {
        val markdown = "before\n</details>\nafter"

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(listOf("before", "after"), lines.map { it.text })
        assertTrue(lines.all { it.collapsibleIds.isEmpty() })
    }

    @Test
    fun ordinaryHtmlBlockWithoutDetailsIsStillDroppedUnchanged() {
        // Pre-#403 behaviour for any other raw HTML block is untouched.
        val markdown = "before\n\n<div>raw html</div>\n\nafter"

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(listOf("before", "after"), lines.map { it.text })
    }

    @Test
    fun detailsTypedInsideATableCellIsNotSupportedButDoesNotCrash() {
        // A GFM table cell only ever parses inline content (the tables
        // extension never hands a cell block-level children), so `<details>`
        // written inside one arrives as an inline HtmlInline node rather than
        // the HtmlBlock this feature hooks into — the same reason a `#`
        // heading or a fenced code block cannot appear inside a cell either.
        // Getting a block-level feature to nest inside a cell would need a
        // custom table-cell parser, which is a materially bigger change than
        // this issue asked for; what matters here is that typing it in does
        // not crash and does not produce a bogus collapsible section.
        val markdown = "| A | B |\n| - | - |\n| <details><summary>x</summary>y</details> | z |"

        val lines = SimpleMarkdownPreview.parse(markdown)

        assertEquals(listOf(PreviewLineType.TABLE), lines.map { it.type })
        assertTrue(lines.none { it.type == PreviewLineType.COLLAPSIBLE_SUMMARY })
    }
}
