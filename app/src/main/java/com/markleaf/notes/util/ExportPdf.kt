package com.markleaf.notes.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.CalloutHead
import com.markleaf.notes.core.markdown.CalloutKind
import com.markleaf.notes.core.markdown.CommonMarkPreviewAdapter
import com.markleaf.notes.core.markdown.preview.unresolvedImageText
import com.markleaf.notes.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.commonmark.ext.autolink.AutolinkExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.HtmlBlock
import org.commonmark.node.Image
import org.commonmark.node.Text
import org.commonmark.ext.footnotes.FootnotesExtension
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer

/**
 * Renders a [Note] to HTML and hands it to Android's [PrintManager]. The user picks
 * "Save as PDF" from the system print dialog, which writes the file to wherever they
 * want (Drive, Downloads, etc.). We never own the output URI — keeps the no-INTERNET
 * promise intact and avoids storage-permission requests.
 */
object ExportPdf {
    private val extensions = listOf(
        // Front matter is metadata, as it is in the preview (which shows it in
        // its own muted block). Without this extension the PDF parsed it as
        // Markdown: the opening `---` printed as a rule and the YAML under it,
        // closed by the second `---`, as a Setext H2 heading.
        YamlFrontMatterExtension.create(),
        StrikethroughExtension.create(),
        TablesExtension.create(),
        TaskListItemsExtension.create(),
        FootnotesExtension.builder().build(),
        // Bare URLs print as links, as the preview shows them (D082).
        AutolinkExtension.create()
    )

    private val parser: Parser = Parser.builder().extensions(extensions).build()

    // `---` that nothing closes is a rule, not front matter: the extension
    // would swallow the rest of the note. Same rule as the preview
    // (CommonMarkPreviewAdapter.opensUnclosedFrontMatter, review of #491).
    private val parserWithoutFrontMatter: Parser = Parser.builder()
        .extensions(extensions.filterNot { it is YamlFrontMatterExtension })
        .build()

    private val renderer: HtmlRenderer = HtmlRenderer.builder().extensions(extensions).build()

    /**
     * Suspends while the note's images are read and encoded on [Dispatchers.IO];
     * the WebView itself is created back on the caller's (main) thread.
     */
    suspend fun export(context: Context, note: Note): Boolean {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return false

        val untitled = context.getString(R.string.untitled)
        val title = note.title.ifBlank { untitled }
        val html = withContext(Dispatchers.IO) {
            renderDocument(
                note,
                untitled,
                calloutLabel = { kind, raw -> calloutLabel(context, kind, raw) },
                imageSource = PdfImageInliner(context)::dataUri
            )
        }

        // WebView must outlive this call until the print adapter is created.
        // PrintManager keeps a reference to it via the adapter, so a local val
        // is enough — once printing finishes the WebView is GC'd.
        val webView = WebView(context)
        // Images arrive inlined as data: URIs, so the page needs nothing from
        // outside. Blocking the network makes that a guarantee rather than an
        // accident of the store build having no INTERNET permission: in the
        // sideload build, a remote image URL in a note must not be fetched just
        // because the user printed it.
        webView.settings.blockNetworkLoads = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val jobName = context.getString(R.string.export_pdf_job_name, title)
                val adapter = view.createPrintDocumentAdapter(jobName)
                val attributes = PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                    .build()
                printManager.print(jobName, adapter, attributes)
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        return true
    }

    /**
     * Builds the full printable HTML document for [note]. A note's title is the
     * first line of its Markdown — Markleaf has no separate title field, so the
     * body already contains it. We render the Markdown as-is and never inject a
     * synthetic heading on top, otherwise the first line would print twice: once
     * as the injected title and again as part of the body (#143). [untitled] is
     * the fallback used only for the document/tab `<title>` of a blank note.
     */
    internal fun renderDocument(
        note: Note,
        untitled: String,
        // Before imageSource so a trailing lambda still means the image source.
        calloutLabel: (kind: CalloutKind?, raw: String) -> String = { _, raw -> raw },
        imageSource: (destination: String) -> String? = { null }
    ): String {
        val markdown = note.contentMarkdown
        val title = note.title.ifBlank { untitled }
        val document = try {
            (if (CommonMarkPreviewAdapter.opensUnclosedFrontMatter(markdown)) parserWithoutFrontMatter else parser)
                .parse(markdown)
        } catch (tooManyCells: IllegalArgumentException) {
            // commonmark 0.30 aborts a table past a million cells; print the
            // note as its text rather than fail the export (D082).
            return wrapHtml(title, "<pre>${escape(markdown)}</pre>")
        }
        inlineImages(document, imageSource)
        markCallouts(document, calloutLabel)
        flattenWikilinks(document)
        val bodyHtml = renderer.render(document)
        return wrapHtml(title, bodyHtml)
    }

    /**
     * Prints a callout as the preview draws it — a tinted box under its own
     * title — instead of a quote that starts with the literal `[!NOTE]`.
     *
     * The head line is read by [CalloutHead.take], the same code the preview
     * uses, so the two agree on what is a callout and what its title is. The
     * box is a `<div>` around the quote, written as raw HTML blocks beside it,
     * because the renderer is shared and a per-call attribute would have to
     * live outside the tree.
     */
    private fun markCallouts(
        document: org.commonmark.node.Node,
        calloutLabel: (CalloutKind?, String) -> String
    ) {
        val quotes = mutableListOf<BlockQuote>()
        document.accept(object : AbstractVisitor() {
            override fun visit(blockQuote: BlockQuote) {
                quotes += blockQuote
                visitChildren(blockQuote)
            }
        })
        for (quote in quotes) {
            val head = CalloutHead.take(quote) ?: continue
            val kind = CalloutKind.parse(head.type)
            val css = kind?.name?.lowercase() ?: "other"
            val title = head.title.ifBlank { calloutLabel(kind, head.type) }
            quote.insertBefore(htmlBlock("<div class=\"callout callout-$css\">"))
            quote.insertAfter(htmlBlock("</div>"))
            quote.prependChild(htmlBlock("<p class=\"callout-title\">${escape(title)}</p>"))
        }
    }

    private fun htmlBlock(html: String) = HtmlBlock().apply { literal = html }

    /**
     * `[[Target]]` and `[[Target|Label]]` print as the words the preview shows
     * (the label), not the brackets — a PDF has nothing to link them to.
     * Code spans are separate nodes and keep their brackets.
     */
    private fun flattenWikilinks(document: org.commonmark.node.Node) {
        document.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                if (WikilinkExtractor.hasAny(text.literal)) {
                    text.literal = WIKILINK_REGEX.replace(text.literal) { WikilinkExtractor.label(it.groupValues[1]) }
                }
            }
        })
    }

    private val WIKILINK_REGEX = Regex("""\[\[([^\[\]\n]+?)]]""")

    private fun calloutLabel(context: Context, kind: CalloutKind?, raw: String): String = when (kind) {
        CalloutKind.NOTE -> context.getString(R.string.callout_note)
        CalloutKind.TIP -> context.getString(R.string.callout_tip)
        CalloutKind.IMPORTANT -> context.getString(R.string.callout_important)
        CalloutKind.WARNING -> context.getString(R.string.callout_warning)
        CalloutKind.CAUTION -> context.getString(R.string.callout_caution)
        null -> raw
    }

    /**
     * Points every image at what [imageSource] returns for its destination,
     * or — when it returns null — replaces the image with the same
     * `![alt](path)` text the in-app preview shows for an image it can't
     * resolve (#474).
     *
     * Why the images must be rewritten at all: the page is loaded from a
     * string with no base URL, so a relative `attachments/…` path resolves to
     * nothing, and WebView file access is off by default from Android 11. An
     * inlined data: URI needs neither, and keeps the export self-contained.
     */
    private fun inlineImages(document: org.commonmark.node.Node, imageSource: (String) -> String?) {
        val images = mutableListOf<Image>()
        document.accept(object : AbstractVisitor() {
            override fun visit(image: Image) {
                images += image
            }
        })
        for (image in images) {
            val source = imageSource(image.destination)
            if (source != null) {
                image.destination = source
            } else {
                val alt = buildString {
                    var child = image.firstChild
                    while (child != null) {
                        if (child is Text) append(child.literal)
                        child = child.next
                    }
                }
                image.insertAfter(Text(unresolvedImageText(alt, image.destination)))
                image.unlink()
            }
        }
    }

    private fun wrapHtml(title: String, body: String): String {
        // Inline styles only — WebView loads data with no base URL, so external
        // CSS would fail. Keeps the rendered PDF visually consistent with the
        // in-app preview (Markleaf green accent, comfortable line width).
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
            <meta charset="UTF-8">
            <title>${escape(title)}</title>
            <style>
              @page { margin: 20mm 18mm; }
              body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
                  "Helvetica Neue", Arial, sans-serif;
                font-size: 11pt;
                line-height: 1.6;
                color: #2c3531;
                max-width: 720px;
                margin: 0 auto;
                padding: 0;
              }
              h1, h2, h3, h4, h5, h6 {
                color: #1a2521;
                line-height: 1.3;
                margin-top: 1.6em;
                margin-bottom: 0.5em;
                font-weight: 700;
                page-break-after: avoid;
              }
              h1 {
                font-size: 1.8em;
                border-bottom: 2px solid #3d6b49;
                padding-bottom: 0.3em;
                color: #1a2521;
              }
              h2 {
                font-size: 1.4em;
                border-bottom: 1px solid #d6dcd5;
                padding-bottom: 0.2em;
                margin-top: 1.4em;
              }
              h3 { font-size: 1.2em; }
              p { margin: 0.8em 0; }
              a {
                color: #3d6b49;
                text-decoration: none;
                font-weight: 500;
              }
              a:hover {
                text-decoration: underline;
              }
              code {
                background: #f3f6f3;
                border-radius: 4px;
                padding: 0.15em 0.35em;
                font-family: "JetBrains Mono", "SFMono-Regular", Menlo, Consolas, monospace;
                font-size: 0.88em;
                color: #2e5939;
              }
              pre {
                background: #f7faf7;
                border: 1px solid #e1e7e1;
                border-radius: 8px;
                padding: 14px 16px;
                overflow-x: auto;
                font-size: 0.85em;
                line-height: 1.5;
                margin: 1em 0;
                page-break-inside: avoid;
              }
              pre code {
                background: transparent;
                padding: 0;
                color: inherit;
                font-size: inherit;
              }
              blockquote {
                margin: 1.2em 0;
                padding: 0.4em 1.2em;
                border-left: 4px solid #3d6b49;
                background: #f8faf8;
                color: #4a544f;
                border-radius: 0 6px 6px 0;
                page-break-inside: avoid;
              }
              .callout blockquote {
                border-left-color: var(--callout-accent);
                background: var(--callout-fill);
                color: #2c3531;
              }
              .callout-title {
                font-weight: 700;
                color: var(--callout-accent);
                margin: 0.3em 0 0.4em;
              }
              .callout-note { --callout-accent: #3d6b49; --callout-fill: #eef5ef; }
              .callout-tip { --callout-accent: #3f6a85; --callout-fill: #edf3f7; }
              .callout-important { --callout-accent: #6a4f86; --callout-fill: #f3eff7; }
              .callout-warning { --callout-accent: #9a6514; --callout-fill: #fbf4e6; }
              .callout-caution { --callout-accent: #a63a2c; --callout-fill: #fbecea; }
              .callout-other { --callout-accent: #5d6762; --callout-fill: #f2f4f2; }
              details {
                display: block;
                margin: 1.2em 0;
              }
              details > summary {
                font-weight: 600;
                color: #1a2521;
                margin-bottom: 0.5em;
                list-style: none;
              }
              details > summary::-webkit-details-marker {
                display: none;
              }
              /* A PDF has no way to tap anything open, so a collapsible
                 section's content prints regardless of whether the note
                 shows it collapsed in the app -- the reader would otherwise
                 lose real, saved content with no indication it exists. */
              details > :not(summary) {
                display: block !important;
              }
              ul, ol {
                padding-left: 1.8em;
                margin: 0.8em 0;
              }
              li {
                margin: 0.35em 0;
                page-break-inside: avoid;
              }
              li.task-list-item, .task-list-item {
                list-style-type: none;
                margin-left: -1.2em;
                page-break-inside: avoid;
              }
              input[type=checkbox] {
                margin-right: 0.45em;
                vertical-align: middle;
              }
              table {
                border-collapse: collapse;
                width: 100%;
                margin: 1.2em 0;
                font-size: 0.92em;
                page-break-inside: avoid;
              }
              th, td {
                border: 1px solid #d6dcd5;
                padding: 8px 12px;
                text-align: left;
              }
              th {
                background: #eaf0eb;
                color: #2c3531;
                font-weight: 600;
              }
              tr {
                page-break-inside: avoid;
              }
              tr:nth-child(even) {
                background: #fcfdfc;
              }
              hr {
                border: none;
                border-top: 1px solid #d6dcd5;
                margin: 1.8em 0;
              }
              img {
                max-width: 100%;
                height: auto;
                border-radius: 6px;
                page-break-inside: avoid;
              }
            </style>
            </head>
            <body>
            $body
            </body>
            </html>
        """.trimIndent()
    }

    private fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
