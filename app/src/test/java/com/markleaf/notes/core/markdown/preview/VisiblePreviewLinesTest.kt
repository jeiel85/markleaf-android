package com.markleaf.notes.core.markdown.preview

import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers [visiblePreviewLines] — what a `<details>` section (#403) hides from
 * the rendered LazyColumn and from index-based features (the outline, the
 * jump-to-end button) built on top of it. Parses real Markdown via
 * [SimpleMarkdownPreview] so the ids under test are the ones
 * [com.markleaf.notes.core.markdown.CommonMarkPreviewAdapter] actually assigns,
 * the same reason [TocHeadingsTest] and [FootnoteJumpTest] do.
 */
class VisiblePreviewLinesTest {
    private val collapsedByDefault = """
        <details>
        <summary>Toggle me</summary>

        hidden body
        </details>
        after
    """.trimIndent()

    private val openByDefault = """
        <details open>
        <summary>Toggle me</summary>

        shown body
        </details>
        after
    """.trimIndent()

    @Test
    fun `a section with no open attribute hides its body until toggled`() {
        val lines = SimpleMarkdownPreview.parse(collapsedByDefault)

        val collapsed = visiblePreviewLines(lines, toggledSectionIds = emptySet())
        assertEquals(listOf(PreviewLineType.COLLAPSIBLE_SUMMARY, PreviewLineType.BODY), collapsed.map { it.type })
        assertEquals(listOf("Toggle me", "after"), collapsed.map { it.text })

        val id = lines.first().collapsibleId!!
        val expanded = visiblePreviewLines(lines, toggledSectionIds = setOf(id))
        assertEquals(listOf("Toggle me", "hidden body", "after"), expanded.map { it.text })
    }

    @Test
    fun `a details-open section shows its body until toggled closed`() {
        val lines = SimpleMarkdownPreview.parse(openByDefault)

        val shown = visiblePreviewLines(lines, toggledSectionIds = emptySet())
        assertEquals(listOf("Toggle me", "shown body", "after"), shown.map { it.text })

        val id = lines.first().collapsibleId!!
        val closed = visiblePreviewLines(lines, toggledSectionIds = setOf(id))
        assertEquals(listOf("Toggle me", "after"), closed.map { it.text })
    }

    @Test
    fun `a note with no collapsible sections is returned unchanged`() {
        val lines = SimpleMarkdownPreview.parse("just a paragraph")

        assertEquals(lines, visiblePreviewLines(lines, toggledSectionIds = emptySet()))
    }

    @Test
    fun `collapsing an outer section also hides an expanded inner one`() {
        val markdown = """
            <details open>
            <summary>Outer</summary>

            <details open>
            <summary>Inner</summary>

            innermost
            </details>
            </details>
        """.trimIndent()
        val lines = SimpleMarkdownPreview.parse(markdown)
        val outerId = lines.first { it.text == "Outer" }.collapsibleId!!

        // Both default open, so nothing toggled shows everything...
        assertEquals(
            listOf("Outer", "Inner", "innermost"),
            visiblePreviewLines(lines, emptySet()).map { it.text }
        )
        // ...but collapsing the outer one hides the inner section and its body
        // together, even though the inner section was never itself toggled.
        assertEquals(
            listOf("Outer"),
            visiblePreviewLines(lines, setOf(outerId)).map { it.text }
        )
    }
}
