package com.markleaf.notes.core.markdown

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import com.markleaf.notes.util.WikilinkExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Obsidian syntax from #608 that Markleaf now reads: `==highlight==`,
 * `%%comment%%` and `[[Note#Heading]]`.
 *
 * Before: the first two were drawn as typed, `==` and `%%` included, and a
 * heading link created a new note titled "Note#Heading".
 */
class ObsidianSyntaxTest {

    private fun parse(markdown: String) = SimpleMarkdownPreview.parse(markdown)

    private fun allText(markdown: String) = parse(markdown).joinToString("\n") { it.text }

    private fun segments(markdown: String) = parse(markdown).flatMap { it.segments }

    // ---- highlight -----------------------------------------------------------

    @Test
    fun highlightedTextIsAHighlightSegmentWithoutItsMarkers() {
        val segs = segments("Read ==this part== first")

        assertEquals("this part", segs.single { it.type == PreviewInlineType.HIGHLIGHT }.text)
        assertEquals("Read this part first", segs.joinToString("") { it.text })
    }

    @Test
    fun twoHighlightsInOneLineStayApart() {
        val highlights = segments("==a== and ==b==").filter { it.type == PreviewInlineType.HIGHLIGHT }

        assertEquals(listOf("a", "b"), highlights.map { it.text })
    }

    @Test
    fun equalsSignsThatAreNotAHighlightStayAsTyped() {
        for (text in listOf("a == b", "x ==  == y", "== spaced ==", "if a==b")) {
            assertTrue(text, segments(text).none { it.type == PreviewInlineType.HIGHLIGHT })
            assertEquals(text, allText(text))
        }
    }

    @Test
    fun aHighlightInsideCodeIsLeftAlone() {
        val segs = segments("`==code==` and\n\n```\n==block==\n```")

        assertTrue(segs.none { it.type == PreviewInlineType.HIGHLIGHT })
        assertTrue(allText("`==code==`").contains("==code=="))
    }

    @Test
    fun aHighlightInAHeadingKeepsItsWords() {
        assertEquals("Plan for today", parse("# Plan ==for== today").single().text)
    }

    // ---- comments ------------------------------------------------------------

    @Test
    fun anInlineCommentIsHidden() {
        assertEquals("Keep this and this", allText("Keep this %%not this%%and this"))
    }

    @Test
    fun aParagraphThatIsOneCommentIsDropped() {
        val lines = parse("Before\n\n%%\nhidden line one\nhidden line two\n%%\n\nAfter")

        assertEquals(listOf("Before", "After"), lines.map { it.text })
    }

    @Test
    fun aCommentInsideCodeIsShown() {
        assertTrue(allText("`%%shown%%`").contains("%%shown%%"))
    }

    @Test
    fun aSinglePercentPairIsNotAComment() {
        assertEquals("50%% off", allText("50%% off"))
    }

    // ---- heading links -------------------------------------------------------

    @Test
    fun aHeadingLinkNamesItsNote() {
        assertEquals("Note", WikilinkExtractor.noteTitle("Note#Heading"))
        assertEquals("Heading", WikilinkExtractor.heading("Note#Heading"))
        assertEquals("", WikilinkExtractor.noteTitle("#Heading"))
        assertNull(WikilinkExtractor.heading("Note#^block-id"))
        assertNull(WikilinkExtractor.heading("Note"))
    }

    @Test
    fun aHeadingLinkCountsAsABacklinkToItsNoteAndKeepsTheWholeTarget() {
        val targets = WikilinkExtractor.extract("[[Note#Heading|see]] and [[C# basics]] and [[Plain]]")

        assertEquals(listOf("Note#Heading", "Note", "C# basics", "C", "Plain"), targets)
    }

    @Test
    fun aSameNoteHeadingLinkAddsNoBacklink() {
        assertEquals(listOf("#Heading"), WikilinkExtractor.extract("[[#Heading]]"))
    }

    // ---- editor --------------------------------------------------------------

    private val colors = MarkdownSyntaxColors(
        heading = Color.Red,
        emphasis = Color.Blue,
        link = Color.Green,
        syntax = Color.Gray,
        checkbox = Color.Magenta,
        code = Color.Cyan,
        codeBlock = Color.Yellow,
        blockquote = Color.LightGray,
        horizontalRule = Color.Black,
        highlightBackground = Color.Yellow
    )

    private fun AnnotatedString.covered(start: Int, end: Int, predicate: (SpanStyle) -> Boolean) =
        spanStyles.any { predicate(it.item) && it.start <= start && it.end >= end }

    @Test
    fun theEditorBandsAHighlightAndGreysAComment() {
        val text = "a ==marked== b %%note%% c"
        val result = MarkdownSyntaxHighlighter.highlight(text, colors)

        val mark = text.indexOf("==marked==")
        assertTrue(result.covered(mark, mark + "==marked==".length) { it.background == Color.Yellow })
        val note = text.indexOf("%%note%%")
        assertTrue(result.covered(note, note + "%%note%%".length) { it.color == Color.Gray })
        assertEquals(text, result.text)
    }

    @Test
    fun theEditorGreysACommentOverSeveralLines() {
        val comment = "%%\nwhole block\n%%"
        val text = "Read %%inline%% end.\n\n$comment\n\nAfter"
        val result = MarkdownSyntaxHighlighter.highlight(text, colors)

        val block = text.indexOf(comment)
        assertTrue(result.covered(block, block + comment.length) { it.color == Color.Gray })
        val after = text.indexOf("After")
        assertFalse(result.covered(after, after + 5) { it.color == Color.Gray })
    }

    @Test
    fun theEditorLeavesAHighlightInCodeUnbanded() {
        val result = MarkdownSyntaxHighlighter.highlight("`==code==`", colors)

        assertFalse(result.spanStyles.any { it.item.background == Color.Yellow })
    }
}
