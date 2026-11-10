package com.markleaf.notes.util

import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.data.settings.EditorFont
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
        assertEquals(1, Regex("<h1[^>]*>My Note</h1>").findAll(body).count())
    }

    @Test
    fun `plain first line is not forced into a heading`() {
        val html = ExportPdf.renderDocument(note("Shopping list\n\nMilk"), "Untitled")
        val body = bodyOf(html)
        assertFalse(body.contains("<h1")) // not promoted to a title
        assertEquals(1, Regex("Shopping list").findAll(body).count())
    }

    @Test
    fun `blank note keeps an untitled document title but no body heading`() {
        val html = ExportPdf.renderDocument(note(""), "Untitled")
        // The <title> in <head> still labels the print job / tab.
        assertTrue(html.contains("<title>Untitled</title>"))
        // The visible body has no injected heading.
        assertFalse(bodyOf(html).contains("<h1"))
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
    fun aCalloutPrintsAsATitledBox() {
        val body = bodyOf(
            ExportPdf.renderDocument(
                note("> [!WARNING]\n> Be careful"),
                "Untitled",
                calloutLabel = { kind, _ -> "Label:${kind?.name}" }
            )
        )

        assertTrue(body, body.contains("<div class=\"callout callout-warning\">"))
        assertTrue(body, body.contains("<p class=\"callout-title\">Label:WARNING</p>"))
        assertTrue(body, body.contains("Be careful"))
        assertFalse(body, body.contains("[!WARNING]"))
    }

    @Test
    fun aCalloutTitleIsEscapedAndUsed() {
        // `1 < 2` is text, not a tag, so it reaches the title and must be escaped.
        val body = bodyOf(ExportPdf.renderDocument(note("> [!TIP] When 1 < 2 & co\n> body"), "Untitled"))

        assertTrue(body, body.contains("<p class=\"callout-title\">When 1 &lt; 2 &amp; co</p>"))
    }

    @Test
    fun anUnknownCalloutTypeIsLabelledByItsWord() {
        val body = bodyOf(ExportPdf.renderDocument(note("> [!info]\n> body"), "Untitled"))

        assertTrue(body, body.contains("callout-other"))
        assertTrue(body, body.contains(">info</p>"))
    }

    @Test
    fun wikilinksPrintAsTheirLabel() {
        val body = bodyOf(ExportPdf.renderDocument(note("See [[Target]] and [[Other|the other]]. `[[code]]`"), "Untitled"))

        assertTrue(body, body.contains("See Target and the other."))
        assertTrue(body, body.contains("<code>[[code]]</code>"))
    }

    @Test
    fun aTablePastTheCellLimitPrintsAsText() {
        val columns = 1_000
        val row = "|" + "x|".repeat(columns)
        val markdown = buildString {
            append(row).append('\n')
            append("|").append("-|".repeat(columns)).append('\n')
            repeat(1_001) { append(row).append('\n') }
        }

        val body = bodyOf(ExportPdf.renderDocument(note(markdown), "Untitled"))

        assertTrue(body.contains("<pre>"))
        assertFalse(body.contains("<table"))
    }

    @Test
    fun anAddressInsideAWikilinkPrintsAsTheLabel() {
        val body = bodyOf(ExportPdf.renderDocument(note("See [[www.example.com notes]] now"), "Untitled"))

        assertTrue(body, body.contains("See www.example.com notes now"))
        assertFalse(body, body.contains("<a "))
    }

    @Test
    fun aPlainQuoteInsideACalloutIsNotStyledAsTheCallout() {
        val html = ExportPdf.renderDocument(note("> [!NOTE]\n> > inner"), "Untitled")

        assertTrue(html.contains(".callout > blockquote"))
        assertFalse(html.contains(".callout blockquote"))
    }

    @Test
    fun aBareUrlPrintsAsALink() {
        val body = bodyOf(ExportPdf.renderDocument(note("Visit https://example.com today"), "Untitled"))

        assertTrue(body, body.contains("<a href=\"https://example.com\">https://example.com</a>"))
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

    private fun bodyFontFamily(html: String): String =
        Regex("""body \{\s*font-family: ([^;]+);""").find(html)!!.groupValues[1]

    /** #568: the default font prints as it always has. */
    @Test
    fun `sans keeps the original font stack`() {
        val html = ExportPdf.renderDocument(note("# A\n\nText"), "Untitled")

        assertTrue(bodyFontFamily(html).startsWith("-apple-system"))
        assertFalse(html.contains("@font-face"))
    }

    /** #568: the PDF is set in the font chosen in Settings, like the editor. */
    @Test
    fun `serif and monospace print in the system families`() {
        val serif = ExportPdf.renderDocument(note("# A"), "Untitled", bodyFont = EditorFont.SERIF)
        val mono = ExportPdf.renderDocument(note("# A"), "Untitled", bodyFont = EditorFont.MONOSPACE)

        assertEquals("serif", bodyFontFamily(serif))
        assertEquals("monospace", bodyFontFamily(mono))
    }

    @Test
    fun `a custom font is declared and used first, with sans behind it`() {
        val html = ExportPdf.renderDocument(
            note("# A"), "Untitled", bodyFont = EditorFont.CUSTOM, hasCustomFont = true
        )

        assertTrue(html, html.contains("""@font-face { font-family: "Markleaf Custom"; src: url("https://markleaf.invalid/custom-font"); }"""))
        assertTrue(bodyFontFamily(html).startsWith("\"Markleaf Custom\", -apple-system"))
    }

    /** With its file gone, Custom draws as sans in the editor; the PDF does the same. */
    @Test
    fun `a custom font with no file prints in sans`() {
        val html = ExportPdf.renderDocument(note("# A"), "Untitled", bodyFont = EditorFont.CUSTOM)

        assertFalse(html.contains("@font-face"))
        assertTrue(bodyFontFamily(html).startsWith("-apple-system"))
    }

    /** Code keeps its monospace stack whatever the body font is. */
    @Test
    fun `code keeps its own font`() {
        val html = ExportPdf.renderDocument(note("`x`"), "Untitled", bodyFont = EditorFont.SERIF)

        assertTrue(html.contains("""font-family: "JetBrains Mono", "SFMono-Regular", Menlo, Consolas, monospace;"""))
    }

    // #262: `[x](#heading)` links resolve in the preview (v2.60.0); the PDF
    // needs the same ids on its headings for them to go anywhere.
    @Test
    fun headingsCarryTheIdsThePreviewResolvesAnchorsBy() {
        val html = ExportPdf.renderDocument(
            note("# Plan\n\nSee [setup](#set-up-now).\n\n## Set up `now`!\n\n## Plan\n\n### 한글 제목"),
            untitled = "Untitled"
        )
        val body = bodyOf(html)

        assertTrue(body, body.contains("<h1 id=\"plan\">Plan</h1>"))
        assertTrue(body, body.contains("<h2 id=\"set-up-now\">"))
        assertTrue(body, body.contains("<a href=\"#set-up-now\">setup</a>"))
        // A repeat is numbered, as the preview and GitHub number it.
        assertTrue(body, body.contains("<h2 id=\"plan-1\">Plan</h2>"))
        assertTrue(body, body.contains("<h3 id=\"한글-제목\">"))
    }

    @Test
    fun aHeadingWithNoNameableTextGetsNoId() {
        val body = bodyOf(ExportPdf.renderDocument(note("# !!!\n\nbody"), untitled = "Untitled"))

        assertTrue(body, body.contains("<h1>!!!</h1>"))
    }

    @Test
    fun aWikilinkHeadingIsNamedFromItsSourceAsThePreviewNamesIt() {
        val body = bodyOf(ExportPdf.renderDocument(note("## See [[Other note]]"), untitled = "Untitled"))

        assertTrue(body, body.contains("<h2 id=\"see-other-note\">See Other note</h2>"))
    }

    // Codex on #576: an image the export cannot inline becomes `![alt](path)`
    // text, but the preview names the heading from the alt text alone.
    @Test
    fun aHeadingWithAMissingImageIsNamedFromTheAltTextAsThePreviewNamesIt() {
        val body = bodyOf(
            ExportPdf.renderDocument(note("# See ![chart](missing.png)\n\n[go](#see-chart)"), untitled = "Untitled")
        )

        assertTrue(body, body.contains("<h1 id=\"see-chart\">"))
        assertTrue(body, body.contains("![chart](missing.png)"))
    }
}
