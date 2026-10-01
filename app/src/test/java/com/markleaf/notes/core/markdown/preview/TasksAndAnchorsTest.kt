package com.markleaf.notes.core.markdown.preview

import com.markleaf.notes.core.markdown.MarkdownEditActions
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * M3 of `docs/MARKDOWN_SYNTAX_PLAN.md`.
 *
 * Numbered tasks (`1. [ ]`) lost their checkbox; tasks inside a quote drew a
 * box a tap could not flip; and `[text](#heading)` was handed to the browser
 * as `https://#heading`.
 */
class TasksAndAnchorsTest {

    private fun parse(markdown: String) = SimpleMarkdownPreview.parse(markdown)

    // ---- numbered and quoted tasks ------------------------------------------

    @Test
    fun aNumberedTaskIsACheckboxThatKeepsItsNumber() {
        val lines = parse("1. [ ] first\n2. [x] second")

        assertEquals(listOf(PreviewLineType.CHECKBOX_TODO, PreviewLineType.CHECKBOX_DONE), lines.map { it.type })
        assertEquals(listOf("1", "2"), lines.map { it.extra })
        assertEquals(listOf("first", "second"), lines.map { it.text })
        assertEquals(listOf(0, 1), lines.map { it.sourceLine })
    }

    @Test
    fun aNumberedListStartingLaterKeepsItsNumbers() {
        assertEquals(listOf("7", "8"), parse("7) [ ] a\n8) [ ] b").map { it.extra })
    }

    @Test
    fun aPlainNumberedItemIsStillAnOrderedItem() {
        val line = parse("1. not a task").single()

        assertEquals(PreviewLineType.ORDERED_LIST, line.type)
        assertNull(line.sourceLine)
    }

    @Test
    fun aBulletTaskCarriesNoNumber() {
        assertNull(parse("- [ ] task").single().extra)
    }

    @Test
    fun aTaskInsideAQuoteKnowsItsLine() {
        val line = parse("intro\n\n> - [ ] quoted").last()

        assertEquals(PreviewLineType.CHECKBOX_TODO, line.type)
        assertEquals(1, line.quoteDepth)
        assertEquals(2, line.sourceLine)
    }

    @Test
    fun togglingANumberedTaskFlipsItsBox() {
        assertEquals("1. [x] a\n2) [ ] b", MarkdownEditActions.toggleTaskAtLine("1. [ ] a\n2) [ ] b", 0))
        assertEquals("1. [ ] a\n2) [ ] b", MarkdownEditActions.toggleTaskAtLine("1. [ ] a\n2) [x] b", 1))
    }

    @Test
    fun togglingAQuotedTaskFlipsItsBox() {
        assertEquals("> - [x] q", MarkdownEditActions.toggleTaskAtLine("> - [ ] q", 0))
        assertEquals(">> 3. [ ] c", MarkdownEditActions.toggleTaskAtLine(">> 3. [x] c", 0))
        assertEquals("  > * [x] d", MarkdownEditActions.toggleTaskAtLine("  > * [ ] d", 0))
    }

    @Test
    fun togglingALineThatIsNotATaskChangesNothing() {
        assertNull(MarkdownEditActions.toggleTaskAtLine("1. plain", 0))
        assertNull(MarkdownEditActions.toggleTaskAtLine("> just a quote [ ]", 0))
        assertNull(MarkdownEditActions.toggleTaskAtLine("[ ] no marker", 0))
    }

    @Test
    fun enterAfterANumberedTaskContinuesWithABox() {
        val before = androidx.compose.ui.text.input.TextFieldValue("1. [ ] milk", selection = androidx.compose.ui.text.TextRange(11))
        val typed = androidx.compose.ui.text.input.TextFieldValue("1. [ ] milk\n", selection = androidx.compose.ui.text.TextRange(12))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed, pendingGuard = null)

        assertEquals("1. [ ] milk\n2. [ ] ", result.value.text)
    }

    @Test
    fun enterOnAnEmptyNumberedTaskEndsTheList() {
        val before = androidx.compose.ui.text.input.TextFieldValue("1. [ ] milk\n2. [ ] ", selection = androidx.compose.ui.text.TextRange(19))
        val typed = androidx.compose.ui.text.input.TextFieldValue("1. [ ] milk\n2. [ ] \n", selection = androidx.compose.ui.text.TextRange(20))

        val result = MarkdownEditActions.applyAutoContinuation(before, typed, pendingGuard = null)

        assertEquals("1. [ ] milk\n\n", result.value.text)
    }

    // ---- anchors -------------------------------------------------------------

    @Test
    fun headingSlugsFollowGitHub() {
        assertEquals("hello-world", headingSlug("Hello World!"))
        assertEquals("api-v20-beta", headingSlug("API v2.0 (beta)"))
        assertEquals("한글-제목", headingSlug("한글 제목"))
        assertEquals("a--b", headingSlug("a  b"))
        assertEquals("snake_case-and-kebab-case", headingSlug("snake_case and kebab-case"))
    }

    @Test
    fun anAnchorFindsItsHeading() {
        val lines = parse("# Intro\n\ntext\n\n## Getting Started\n\nmore")

        assertEquals(lines.indexOfFirst { it.text == "Getting Started" }, findHeadingAnchorIndex(lines, "getting-started"))
    }

    @Test
    fun aRepeatedHeadingIsNumberedInOrder() {
        val lines = parse("## Notes\n\na\n\n## Notes\n\nb\n\n## Notes")
        val headings = lines.withIndex().filter { it.value.type == PreviewLineType.H2 }.map { it.index }

        assertEquals(headings[0], findHeadingAnchorIndex(lines, "notes"))
        assertEquals(headings[1], findHeadingAnchorIndex(lines, "notes-1"))
        assertEquals(headings[2], findHeadingAnchorIndex(lines, "notes-2"))
    }

    @Test
    fun aPercentEncodedAnchorIsDecoded() {
        val lines = parse("# 한글 제목")

        assertEquals(0, findHeadingAnchorIndex(lines, "%ED%95%9C%EA%B8%80-%EC%A0%9C%EB%AA%A9"))
    }

    @Test
    fun anAnchorIsCaseInsensitive() {
        assertEquals(0, findHeadingAnchorIndex(parse("# Setup"), "Setup"))
    }

    @Test
    fun anUnknownAnchorFindsNothing() {
        val lines = parse("# Setup")

        assertEquals(-1, findHeadingAnchorIndex(lines, "missing"))
        assertEquals(-1, findHeadingAnchorIndex(lines, ""))
        assertEquals(-1, findHeadingAnchorIndex(lines, "%zz"))
    }
}
