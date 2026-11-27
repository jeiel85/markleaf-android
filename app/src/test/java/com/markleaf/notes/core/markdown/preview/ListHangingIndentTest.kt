package com.markleaf.notes.core.markdown.preview

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * [withHangingIndent] — a list item's wrapped lines start under its first
 * letter, not under the bullet (#606).
 */
class ListHangingIndentTest {

    private fun indents(text: AnnotatedString) =
        text.paragraphStyles.map { Triple(it.start, it.end, it.item.textIndent) }

    @Test
    fun wrappedLinesAreIndentedByTheMarkerWidth() {
        val row = AnnotatedString("• A long item that wraps").withHangingIndent(2, 9.sp)

        assertEquals(listOf(Triple(0, row.length, TextIndent(firstLine = 0.sp, restLine = 9.sp))), indents(row))
    }

    @Test
    fun linesAfterABreakInTheItemLineUpWithItsText() {
        // The row arrives with its break as ITEM_BREAK at offset 7 (see InlineMarkdownText);
        // the paragraph boundary right after it is the line break.
        val row = AnnotatedString("• first${ITEM_BREAK}second").withHangingIndent(2, 9.sp, breaks = listOf(7))

        assertEquals(
            listOf(
                Triple(0, 8, TextIndent(firstLine = 0.sp, restLine = 9.sp)),
                Triple(8, row.length, TextIndent(firstLine = 9.sp, restLine = 9.sp))
            ),
            indents(row)
        )
        assertEquals(-1, row.text.indexOf('\n'))
    }

    @Test
    fun aRowWithoutAMarkerIsUnchanged() {
        val text = AnnotatedString("Plain paragraph")

        assertSame(text, text.withHangingIndent(0, 9.sp))
        assertSame(text, text.withHangingIndent(2, TextUnit.Unspecified))
    }

    @Test
    fun copyingAListItemGivesBackItsNewlines() {
        var copied: AnnotatedString? = null
        val clipboard = ItemBreakRestoringClipboard(object : androidx.compose.ui.platform.ClipboardManager {
            override fun getText(): AnnotatedString? = copied
            override fun setText(annotatedString: AnnotatedString) { copied = annotatedString }
        })

        clipboard.setText(AnnotatedString("• first${ITEM_BREAK}second"))

        assertEquals("• first\nsecond", copied?.text)
    }
}
