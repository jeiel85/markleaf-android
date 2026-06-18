package com.markleaf.notes.util

import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards against #143: PDF export must not render the note's title twice. A
 * note's title is derived from the first line of its Markdown, so the body
 * already contains it — [ExportPdf.renderDocument] must not inject a synthetic
 * heading on top.
 */
class ExportPdfTest {

    private fun note(content: String) = Note(
        id = "1",
        // Mirror the real save path: title is always extracted from the content.
        title = TitleExtractor.extractTitle(content),
        contentMarkdown = content,
        excerpt = "",
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH
    )

    private fun bodyOf(html: String): String =
        html.substringAfter("<body>").substringBefore("</body>")

    @Test
    fun `heading first line appears once in pdf body`() {
        val html = ExportPdf.renderDocument(note("# My Note\n\nHello world"), "Untitled")
        val body = bodyOf(html)
        // commonmark renders the heading once; no extra <h1> is injected on top.
        assertEquals(1, Regex("<h1>My Note</h1>").findAll(body).count())
    }

    @Test
    fun `plain first line is not forced into a heading`() {
        val html = ExportPdf.renderDocument(note("Shopping list\n\nMilk"), "Untitled")
        val body = bodyOf(html)
        assertFalse(body.contains("<h1>")) // not promoted to a title
        assertEquals(1, Regex("Shopping list").findAll(body).count())
    }

    @Test
    fun `blank note keeps an untitled document title but no body heading`() {
        val html = ExportPdf.renderDocument(note(""), "Untitled")
        // The <title> in <head> still labels the print job / tab.
        assertTrue(html.contains("<title>Untitled</title>"))
        // The visible body has no injected heading.
        assertFalse(bodyOf(html).contains("<h1>"))
    }
}
