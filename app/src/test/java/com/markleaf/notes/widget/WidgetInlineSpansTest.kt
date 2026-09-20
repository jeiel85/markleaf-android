package com.markleaf.notes.widget

import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * [WidgetInlineSpans] is what fixes #438: a home-screen widget row used to show
 * `**bold**` literally. `Spanned` is an Android framework type, so this needs a
 * Robolectric run rather than a plain JVM unit test.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class WidgetInlineSpansTest {

    @Test
    fun `a line with no markdown comes back unchanged`() {
        val line = "just a plain line"

        assertSame(line, WidgetInlineSpans.style(line))
    }

    @Test
    fun `bold reads as bold text with the markers gone`() {
        val styled = WidgetInlineSpans.style("do **not** forget milk") as Spanned

        assertEquals("do not forget milk", styled.toString())
        val spans = styled.getSpans(0, styled.length, StyleSpan::class.java)
        assertEquals(1, spans.size)
        assertEquals(android.graphics.Typeface.BOLD, spans[0].style)
        assertEquals("not", styled.subSequence(styled.getSpanStart(spans[0]), styled.getSpanEnd(spans[0])).toString())
    }

    @Test
    fun `italic and strikethrough both style without leaving markers`() {
        val styled = WidgetInlineSpans.style("*italic* and ~~gone~~") as Spanned

        assertEquals("italic and gone", styled.toString())
        assertEquals(1, styled.getSpans(0, styled.length, StyleSpan::class.java).size)
        assertEquals(1, styled.getSpans(0, styled.length, StrikethroughSpan::class.java).size)
    }

    @Test
    fun `inline code gets a monospace span`() {
        val styled = WidgetInlineSpans.style("run `gradlew test`") as Spanned

        assertEquals("run gradlew test", styled.toString())
        val spans = styled.getSpans(0, styled.length, TypefaceSpan::class.java)
        assertEquals(1, spans.size)
    }

    /**
     * The reason [WidgetInlineSpans.fenceFlags] exists at all: two unrelated
     * `**` occurrences on one code line — Python's `**` operator, used twice —
     * read as a matched bold pair to the same regex the in-app preview uses,
     * which is worse than the literal asterisks it replaced.
     */
    @Test
    fun `a code line with two unrelated double-stars still parses as if it were prose`() {
        val styled = WidgetInlineSpans.style("result = a**2 + b**2") as Spanned

        assertEquals("result = a2 + b2", styled.toString())
        assertEquals(1, styled.getSpans(0, styled.length, StyleSpan::class.java).size)
    }

    // --- fenceFlags ----------------------------------------------------

    @Test
    fun `lines outside any fence are not flagged`() {
        val flags = WidgetInlineSpans.fenceFlags(listOf("one", "**two**", "three"))

        assertEquals(listOf(false, false, false), flags)
    }

    @Test
    fun `both fence delimiters and everything between them are flagged`() {
        val lines = listOf("before", "```", "def f(**kwargs): pass", "```", "after")

        val flags = WidgetInlineSpans.fenceFlags(lines)

        assertEquals(listOf(false, true, true, true, false), flags)
    }

    @Test
    fun `a tilde fence is recognized the same as a backtick one`() {
        val lines = listOf("~~~", "code", "~~~")

        assertEquals(listOf(true, true, true), WidgetInlineSpans.fenceFlags(lines))
    }

    @Test
    fun `an unclosed fence flags every line to the end`() {
        val lines = listOf("before", "```", "still inside")

        assertEquals(listOf(false, true, true), WidgetInlineSpans.fenceFlags(lines))
    }

    @Test
    fun `two separate fenced blocks each open and close independently`() {
        val lines = listOf("```", "a", "```", "prose", "```", "b", "```")

        val flags = WidgetInlineSpans.fenceFlags(lines)

        assertEquals(listOf(true, true, true, false, true, true, true), flags)
    }
}
