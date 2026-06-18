package com.markleaf.notes.util

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import org.commonmark.ext.footnotes.FootnotesExtension
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
        StrikethroughExtension.create(),
        TablesExtension.create(),
        TaskListItemsExtension.create(),
        FootnotesExtension.builder().build()
    )

    private val parser: Parser = Parser.builder().extensions(extensions).build()
    private val renderer: HtmlRenderer = HtmlRenderer.builder().extensions(extensions).build()

    fun export(context: Context, note: Note): Boolean {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return false

        val untitled = context.getString(R.string.untitled)
        val title = note.title.ifBlank { untitled }
        val html = renderDocument(note, untitled)

        // WebView must outlive this call until the print adapter is created.
        // PrintManager keeps a reference to it via the adapter, so a local val
        // is enough — once printing finishes the WebView is GC'd.
        val webView = WebView(context)
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
    internal fun renderDocument(note: Note, untitled: String): String {
        val bodyHtml = renderer.render(parser.parse(note.contentMarkdown))
        val title = note.title.ifBlank { untitled }
        return wrapHtml(title, bodyHtml)
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
