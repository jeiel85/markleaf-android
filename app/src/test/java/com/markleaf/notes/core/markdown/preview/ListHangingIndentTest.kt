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
        val row = AnnotatedString("• first\nsecond").withHangingIndent(2, 9.sp)

        assertEquals(
            listOf(
                Triple(0, 8, TextIndent(firstLine = 0.sp, restLine = 9.sp)),
                Triple(8, row.length, TextIndent(firstLine = 9.sp, restLine = 9.sp))
            ),
            indents(row)
        )
    }

    @Test
    fun aRowWithoutAMarkerIsUnchanged() {
        val text = AnnotatedString("Plain paragraph")

        assertSame(text, text.withHangingIndent(0, 9.sp))
        assertSame(text, text.withHangingIndent(2, TextUnit.Unspecified))
    }
}
