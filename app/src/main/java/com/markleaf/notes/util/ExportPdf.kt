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

        val bodyHtml = renderer.render(parser.parse(note.contentMarkdown))
        val title = note.title.ifBlank { context.getString(R.string.untitled) }
        val html = wrapHtml(title, bodyHtml)

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
              @page { margin: 18mm 16mm; }
              body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
                  "Helvetica Neue", Arial, sans-serif;
                font-size: 11pt;
                line-height: 1.55;
                color: #1f2421;
                max-width: 700px;
                margin: 0 auto;
                padding: 0 4px;
              }
              h1, h2, h3, h4, h5, h6 {
                color: #1f2421;
                line-height: 1.25;
                margin-top: 1.6em;
                margin-bottom: 0.5em;
              }
              h1 { font-size: 1.7em; border-bottom: 1px solid #d6dcd5; padding-bottom: 0.25em; }
              h2 { font-size: 1.35em; }
              h3 { font-size: 1.15em; }
              p { margin: 0.6em 0; }
              code {
                background: #f1f4f1;
                border-radius: 3px;
                padding: 0.1em 0.35em;
                font-family: "JetBrains Mono", "SFMono-Regular", Menlo, Consolas, monospace;
                font-size: 0.9em;
              }
              pre {
                background: #f1f4f1;
                border-radius: 6px;
                padding: 12px 14px;
                overflow-x: auto;
                font-size: 0.9em;
                line-height: 1.45;
              }
              pre code { background: transparent; padding: 0; }
              blockquote {
                margin: 0.8em 0;
                padding: 0.2em 0.9em;
                border-left: 3px solid #4f7d59;
                color: #4a534d;
              }
              ul, ol { padding-left: 1.5em; }
              li { margin: 0.2em 0; }
              table {
                border-collapse: collapse;
                width: 100%;
                margin: 0.8em 0;
                font-size: 0.95em;
              }
              th, td { border: 1px solid #d6dcd5; padding: 6px 9px; text-align: left; }
              th { background: #eef2ee; }
              a { color: #4f7d59; }
              hr { border: none; border-top: 1px solid #d6dcd5; margin: 1.4em 0; }
              img { max-width: 100%; }
            </style>
            </head>
            <body>
            <h1>${escape(title)}</h1>
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
