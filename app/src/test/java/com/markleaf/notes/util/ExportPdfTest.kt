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

    /**
     * #403 hardening: a self-review agent found that a collapsed `&lt;details&gt;`
     * section's content is invisible in the exported PDF with no indication
     * it exists at all -- there is no tap in a static document to reveal it.
     * This pins both halves of the fix: the raw tags still pass through
     * commonmark's renderer unescaped (so there is something for the CSS to
     * apply to), and the stylesheet forces every section's content to print
     * regardless of the `open` attribute.
     */
    @Test
    fun `collapsed details content is forced visible for print`() {
        val markdown = "<details>\n<summary>Appendix</summary>\n\nImportant data\n</details>"
        val html = ExportPdf.renderDocument(note(markdown), "Untitled")
        val body = bodyOf(html)

        assertTrue("the <details> tag must pass through, not be stripped", body.contains("<details>"))
        assertTrue("the <summary> tag must pass through", body.contains("<summary>Appendix</summary>"))
        assertTrue("the body content must pass through", body.contains("Important data"))
        assertTrue(
            "the stylesheet must force non-summary details content visible",
            html.contains("details > :not(summary)") && html.contains("display: block !important")
        )
    }

    /**
     * #474: images were missing from the PDF because the page is loaded with
     * no base URL, so `attachments/…` resolved to nothing. A resolvable image
     * is now inlined as whatever source the resolver returns.
     */
    @Test
    fun `a resolvable image is rendered with the resolved source`() {
        val html = ExportPdf.renderDocument(
            note("# Trip\n\n![Beach](attachments/n1/a.png)"),
            "Untitled"
        ) { destination -> if (destination == "attachments/n1/a.png") "data:image/png;base64,AAAA" else null }
        val body = bodyOf(html)

        assertTrue(body, body.contains("<img src=\"data:image/png;base64,AAAA\" alt=\"Beach\""))
        assertFalse(body.contains("attachments/n1/a.png"))
    }

    /** Unresolvable images read the way the in-app preview shows them, not as a broken icon. */
    @Test
    fun `an unresolvable image becomes the same text the preview shows`() {
        val html = ExportPdf.renderDocument(
            note("Look: ![Diagram](https://example.com/d.png)"),
            "Untitled"
        )
        val body = bodyOf(html)

        assertFalse(body.contains("<img"))
        assertTrue(body, body.contains("![Diagram](https://example.com/d.png)"))
    }

    @Test
    fun frontMatterIsNotPrintedAsAHeading() {
        // Without the front-matter extension the closing `---` turned the YAML
        // above it into a Setext H2, and the opening one into a rule.
        val body = bodyOf(ExportPdf.renderDocument(note("---\ntitle: Hello\ntags: [a]\n---\n\nBody text"), "Untitled"))

        assertFalse(body, body.contains("<h2"))
        assertFalse(body, body.contains("<hr"))
        assertFalse(body, body.contains("title: Hello"))
        assertTrue(body, body.contains("<p>Body text</p>"))
    }

    @Test
    fun anOpeningRuleThatNothingClosesStillPrintsWhatFollows() {
        val body = bodyOf(ExportPdf.renderDocument(note("---\nJust some prose"), "Untitled"))

        assertTrue(body, body.contains("<hr"))
        assertTrue(body, body.contains("<p>Just some prose</p>"))
    }

    @Test
    fun aNoteThatIsOnlyARulePrintsTheRule() {
        val body = bodyOf(ExportPdf.renderDocument(note("---"), "Untitled"))

        assertTrue(body, body.contains("<hr"))
    }

    @Test
    fun aLoneRuleIsStillARule() {
        val body = bodyOf(ExportPdf.renderDocument(note("Above\n\n---\n\nBelow"), "Untitled"))

        assertTrue(body, body.contains("<hr"))
        assertTrue(body, body.contains("<p>Below</p>"))
    }
}
