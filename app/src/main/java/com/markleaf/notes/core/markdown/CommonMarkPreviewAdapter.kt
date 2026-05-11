package com.markleaf.notes.core.markdown

import org.commonmark.ext.footnotes.FootnoteDefinition
import org.commonmark.ext.footnotes.FootnoteReference
import org.commonmark.ext.footnotes.FootnotesExtension
import org.commonmark.ext.front.matter.YamlFrontMatterBlock
import org.commonmark.ext.front.matter.YamlFrontMatterExtension
import org.commonmark.ext.front.matter.YamlFrontMatterVisitor
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.gfm.tables.TableBlock
import org.commonmark.ext.gfm.tables.TableBody
import org.commonmark.ext.gfm.tables.TableCell
import org.commonmark.ext.gfm.tables.TableHead
import org.commonmark.ext.gfm.tables.TableRow
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.task.list.items.TaskListItemMarker
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.Image
import org.commonmark.node.IndentedCodeBlock
import org.commonmark.node.Link
import org.commonmark.node.ListItem
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.node.ThematicBreak
import org.commonmark.parser.Parser
import com.markleaf.notes.util.WikilinkExtractor

/**
 * Adapts the commonmark-java AST to Markleaf's [PreviewLine] / [PreviewInlineSegment]
 * shape so the existing renderer ([com.markleaf.notes.core.markdown.preview.MarkdownPreviewList])
 * keeps working unchanged.
 *
 * The parser is wired with extensions that match what the hand-rolled
 * [SimpleMarkdownPreview] used to handle:
 *  - YAML front-matter (`---\n...\n---`)
 *  - Strikethrough (`~~text~~`)
 *  - Footnotes (`[^N]` reference + `[^N]: definition`)
 *  - Task list items (`- [ ]`, `- [x]`)
 *
 * GitHub-style callouts (`> [!NOTE]` …) aren't a CommonMark extension —
 * they're recognised after-the-fact by inspecting BlockQuote children.
 */
internal object CommonMarkPreviewAdapter {

    private val parser: Parser = Parser.builder()
        .extensions(
            listOf(
                YamlFrontMatterExtension.create(),
                StrikethroughExtension.create(),
                FootnotesExtension.builder().inlineFootnotes(false).build(),
                TaskListItemsExtension.create(),
                TablesExtension.create()
            )
        )
        .build()

    fun parse(markdown: String): List<PreviewLine> {
        // Special case: a document that is just `---` should render as a
        // horizontal rule, not be eaten by the YAML front-matter extension as
        // an unclosed block.
        if (markdown.trim() == "---") {
            return listOf(PreviewLine(text = "", type = PreviewLineType.HORIZONTAL_RULE))
        }

        val document = parser.parse(markdown) as Document
        val out = mutableListOf<PreviewLine>()
        val frontmatter = collectFrontmatter(document)
        if (frontmatter != null) {
            out += PreviewLine(text = frontmatter, type = PreviewLineType.FRONTMATTER)
        }

        var node: Node? = document.firstChild
        while (node != null) {
            when (node) {
                is YamlFrontMatterBlock -> { /* already consumed by collectFrontmatter */ }
                is Heading -> out += renderHeading(node)
                is Paragraph -> out += renderParagraph(node)
                is BulletList -> renderBulletList(node, out)
                is OrderedList -> renderOrderedList(node, out)
                is BlockQuote -> renderBlockQuote(node, out)
                is FencedCodeBlock -> out += PreviewLine(
                    text = node.literal.trimEnd(),
                    type = PreviewLineType.CODE_BLOCK,
                    extra = node.info?.takeIf { it.isNotEmpty() }
                )
                is IndentedCodeBlock -> out += PreviewLine(
                    text = node.literal.trimEnd(),
                    type = PreviewLineType.CODE_BLOCK
                )
                is ThematicBreak -> out += PreviewLine(
                    text = "",
                    type = PreviewLineType.HORIZONTAL_RULE
                )
                is TableBlock -> out += renderTable(node)
                is FootnoteDefinition -> out += renderFootnoteDefinition(node)
                else -> {
                    // Unknown top-level node: render as body so we don't drop content.
                    val text = collectText(node)
                    if (text.isNotBlank()) {
                        out += PreviewLine(
                            text = text,
                            type = PreviewLineType.BODY,
                            segments = collectInlineSegments(node)
                        )
                    }
                }
            }
            node = node.next
        }
        return out
    }

    private fun collectFrontmatter(document: Document): String? {
        val visitor = YamlFrontMatterVisitor()
        document.accept(visitor)
        if (visitor.data.isEmpty()) return null
        return visitor.data.entries.joinToString("\n") { (k, vList) ->
            "$k: ${vList.joinToString(", ")}"
        }
    }

    private fun renderHeading(node: Heading): PreviewLine {
        val type = when (node.level) {
            1 -> PreviewLineType.H1
            2 -> PreviewLineType.H2
            else -> PreviewLineType.H3
        }
        val text = collectText(node)
        return PreviewLine(text = text, type = type, segments = collectInlineSegments(node))
    }

    private fun renderParagraph(node: Paragraph): PreviewLine {
        // Promote `![alt](path)` to a top-level IMAGE block when it stands
        // alone in the paragraph. We don't try to handle inline images
        // mixed with text — they fall through to BODY where the markdown
        // syntax is rendered as-is (still a fine experience).
        val firstChild = node.firstChild
        if (firstChild is Image && firstChild.next == null) {
            val alt = collectText(firstChild)
            return PreviewLine(
                text = alt,
                type = PreviewLineType.IMAGE,
                extra = firstChild.destination
            )
        }
        val text = collectText(node)
        return PreviewLine(text = text, type = PreviewLineType.BODY, segments = collectInlineSegments(node))
    }

    private fun renderBulletList(node: BulletList, out: MutableList<PreviewLine>) {
        var item: Node? = node.firstChild
        while (item != null) {
            if (item is ListItem) {
                val marker = detectTaskMarker(item)
                val (text, segments) = textAndSegments(item)
                val type = when (marker) {
                    TaskState.DONE -> PreviewLineType.CHECKBOX_DONE
                    TaskState.TODO -> PreviewLineType.CHECKBOX_TODO
                    TaskState.NONE -> PreviewLineType.BULLET
                }
                out += PreviewLine(text = text, type = type, segments = segments)
            }
            item = item.next
        }
    }

    private fun renderOrderedList(node: OrderedList, out: MutableList<PreviewLine>) {
        var item: Node? = node.firstChild
        var index = node.markerStartNumber ?: 1
        while (item != null) {
            if (item is ListItem) {
                val (text, segments) = textAndSegments(item)
                out += PreviewLine(
                    text = text,
                    type = PreviewLineType.ORDERED_LIST,
                    extra = index.toString(),
                    segments = segments
                )
                index++
            }
            item = item.next
        }
    }

    private fun renderBlockQuote(node: BlockQuote, out: MutableList<PreviewLine>) {
        // commonmark-java collapses a multi-line `> ...` blockquote into a
        // single Paragraph child whose text contains all the body lines
        // separated by spaces (because soft breaks). For GitHub-style
        // callouts, the Paragraph's text begins with `[!TYPE]` followed by
        // the body. We detect that prefix instead of looking at the first
        // child node alone.
        val combined = collectBlockQuoteText(node)
        val calloutMatch = CALLOUT_HEAD_PREFIX_REGEX.find(combined)
        if (calloutMatch != null) {
            val type = calloutMatch.groupValues[1]
            val body = combined.substring(calloutMatch.range.last + 1).trim()
            out += PreviewLine(
                text = body,
                type = PreviewLineType.CALLOUT,
                extra = type
            )
        } else {
            // Fall back to one BLOCKQUOTE PreviewLine per child paragraph.
            var child: Node? = node.firstChild
            while (child != null) {
                val text = collectText(child)
                if (text.isNotBlank()) {
                    out += PreviewLine(
                        text = text,
                        type = PreviewLineType.BLOCKQUOTE,
                        segments = collectInlineSegments(child)
                    )
                }
                child = child.next
            }
        }
    }

    /**
     * Collect a blockquote's body as a single string with line breaks preserved
     * across SoftLineBreak nodes — this is what we need to find a callout head
     * `[!NOTE]` that sits on its own source line even though commonmark turned
     * the soft break into a space.
     */
    private fun collectBlockQuoteText(node: BlockQuote): String {
        val sb = StringBuilder()
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                sb.append(text.literal)
            }

            override fun visit(softLineBreak: SoftLineBreak) {
                sb.append('\n')
            }

            override fun visit(hardLineBreak: HardLineBreak) {
                sb.append('\n')
            }

            override fun visit(code: Code) {
                sb.append('`').append(code.literal).append('`')
            }
        })
        return sb.toString()
    }

    private fun renderTable(block: TableBlock): PreviewLine {
        var headers = emptyList<String>()
        var alignments = emptyList<TableAlignment>()
        val rows = mutableListOf<List<String>>()

        var section: Node? = block.firstChild
        while (section != null) {
            when (section) {
                is TableHead -> {
                    val headRow = section.firstChild as? TableRow
                    if (headRow != null) {
                        val cells = collectCells(headRow)
                        headers = cells.map { it.text }
                        alignments = cells.map { it.alignment }
                    }
                }
                is TableBody -> {
                    var bodyRow: Node? = section.firstChild
                    while (bodyRow != null) {
                        if (bodyRow is TableRow) {
                            rows += collectCells(bodyRow).map { it.text }
                        }
                        bodyRow = bodyRow.next
                    }
                }
            }
            section = section.next
        }
        // Pad short body rows to header count so the renderer can use a fixed
        // column layout without per-row null checks.
        val width = headers.size.coerceAtLeast(rows.maxOfOrNull { it.size } ?: 0)
        val paddedRows = rows.map { row -> row + List(width - row.size) { "" } }
        val paddedHeaders = headers + List(width - headers.size) { "" }
        val paddedAlignments = alignments + List(width - alignments.size) { TableAlignment.LEFT }
        return PreviewLine(
            text = "",
            type = PreviewLineType.TABLE,
            tableData = TableData(
                headers = paddedHeaders,
                rows = paddedRows,
                alignments = paddedAlignments
            )
        )
    }

    private data class CellOut(val text: String, val alignment: TableAlignment)

    private fun collectCells(row: TableRow): List<CellOut> {
        val out = mutableListOf<CellOut>()
        var cell: Node? = row.firstChild
        while (cell != null) {
            if (cell is TableCell) {
                val alignment = when (cell.alignment) {
                    TableCell.Alignment.LEFT -> TableAlignment.LEFT
                    TableCell.Alignment.CENTER -> TableAlignment.CENTER
                    TableCell.Alignment.RIGHT -> TableAlignment.RIGHT
                    else -> TableAlignment.LEFT
                }
                out += CellOut(collectText(cell), alignment)
            }
            cell = cell.next
        }
        return out
    }

    private fun renderFootnoteDefinition(node: FootnoteDefinition): PreviewLine {
        val body = collectText(node)
        return PreviewLine(
            text = body,
            type = PreviewLineType.FOOTNOTE_DEF,
            extra = node.label,
            segments = collectInlineSegments(node)
        )
    }

    private fun textAndSegments(node: Node): Pair<String, List<PreviewInlineSegment>> {
        val text = collectText(node)
        val segments = collectInlineSegments(node)
        return text to segments
    }

    private fun detectTaskMarker(item: ListItem): TaskState {
        // commonmark-ext-task-list-items inserts a TaskListItemMarker as the
        // first child of a ListItem when the source had `- [ ]` / `- [x]`.
        val marker = item.firstChild as? TaskListItemMarker ?: return TaskState.NONE
        return if (marker.isChecked) TaskState.DONE else TaskState.TODO
    }

    private fun collectText(node: Node): String {
        val sb = StringBuilder()
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                sb.append(text.literal)
            }

            override fun visit(softLineBreak: SoftLineBreak) {
                sb.append(' ')
            }

            override fun visit(hardLineBreak: HardLineBreak) {
                sb.append('\n')
            }

            override fun visit(code: Code) {
                sb.append(code.literal)
            }
        })
        return sb.toString().trim()
    }

    private fun collectInlineSegments(node: Node): List<PreviewInlineSegment> {
        val out = mutableListOf<PreviewInlineSegment>()
        appendInlineSegments(node, out, default = PreviewInlineType.TEXT)
        return out.coalesce()
    }

    private fun appendInlineSegments(
        node: Node,
        out: MutableList<PreviewInlineSegment>,
        default: PreviewInlineType
    ) {
        var child: Node? = node.firstChild
        while (child != null) {
            when (child) {
                is Text -> appendTextSplittingWikilinks(child.literal, default, out)
                is StrongEmphasis -> appendInlineSegments(child, out, mergeBold(default))
                is Emphasis -> appendInlineSegments(child, out, mergeItalic(default))
                is Strikethrough -> appendInlineSegments(child, out, PreviewInlineType.STRIKETHROUGH)
                is Code -> out += PreviewInlineSegment(child.literal, PreviewInlineType.INLINE_CODE)
                is FootnoteReference -> out += PreviewInlineSegment(child.label, PreviewInlineType.FOOTNOTE_REF)
                is Link -> {
                    // Standard `[label](url)` — emit as a single LINK segment so
                    // the renderer can decorate it (primary color + underline)
                    // and dispatch the URL on tap via ACTION_VIEW. Inner emphasis
                    // (`[**bold**](url)`) collapses to the link's plain text.
                    val link = child
                    val label = collectText(link).ifEmpty { link.destination.orEmpty() }
                    out += PreviewInlineSegment(
                        text = label,
                        type = PreviewInlineType.LINK,
                        href = link.destination
                    )
                }
                is SoftLineBreak -> out += PreviewInlineSegment(" ", default)
                is HardLineBreak -> out += PreviewInlineSegment("\n", default)
                is TaskListItemMarker -> { /* checkbox marker styled at line level */ }
                else -> appendInlineSegments(child, out, default)
            }
            child = child.next
        }
    }

    /**
     * commonmark-java emits raw text nodes that don't know about Bear/Obsidian
     * `[[Title]]` syntax. We split each Text node on the wikilink regex and
     * emit alternating TEXT and WIKILINK segments. The WIKILINK segment's
     * `text` is the link target (e.g. `Title`), which the renderer can use
     * both as label and click handler input.
     */
    private fun appendTextSplittingWikilinks(
        literal: String,
        default: PreviewInlineType,
        out: MutableList<PreviewInlineSegment>
    ) {
        if (!WikilinkExtractor.hasAny(literal)) {
            out += PreviewInlineSegment(literal, default)
            return
        }
        val regex = Regex("""\[\[([^\[\]\n]+?)]]""")
        var cursor = 0
        regex.findAll(literal).forEach { match ->
            if (match.range.first > cursor) {
                out += PreviewInlineSegment(literal.substring(cursor, match.range.first), default)
            }
            out += PreviewInlineSegment(match.groupValues[1].trim(), PreviewInlineType.WIKILINK)
            cursor = match.range.last + 1
        }
        if (cursor < literal.length) {
            out += PreviewInlineSegment(literal.substring(cursor), default)
        }
    }

    private fun mergeBold(current: PreviewInlineType): PreviewInlineType = when (current) {
        PreviewInlineType.ITALIC -> PreviewInlineType.BOLD_ITALIC
        else -> PreviewInlineType.BOLD
    }

    private fun mergeItalic(current: PreviewInlineType): PreviewInlineType = when (current) {
        PreviewInlineType.BOLD -> PreviewInlineType.BOLD_ITALIC
        else -> PreviewInlineType.ITALIC
    }

    /** Merge consecutive segments with identical type so the renderer doesn't draw extra runs. */
    private fun List<PreviewInlineSegment>.coalesce(): List<PreviewInlineSegment> {
        if (isEmpty()) return this
        val result = mutableListOf<PreviewInlineSegment>()
        for (segment in this) {
            val last = result.lastOrNull()
            // Same type AND same href (links to different URLs must not merge).
            if (last != null && last.type == segment.type && last.href == segment.href) {
                result[result.lastIndex] = PreviewInlineSegment(
                    text = last.text + segment.text,
                    type = last.type,
                    href = last.href
                )
            } else {
                result += segment
            }
        }
        return result
    }

    private enum class TaskState { NONE, TODO, DONE }

    private val CALLOUT_HEAD_PREFIX_REGEX = Regex("""^\[!([A-Za-z]+)]""")
}
