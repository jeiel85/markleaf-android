package com.markleaf.notes.core.markdown

import org.commonmark.ext.autolink.AutolinkExtension
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
import org.commonmark.node.CustomNode
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlBlock
import org.commonmark.node.HtmlInline
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
import org.commonmark.parser.IncludeSourceSpans
import org.commonmark.parser.Parser

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

    /**
     * How deep the renderer will descend into nested blocks.
     *
     * [renderBlock] recurses once per nesting level, and a note can nest
     * without limit — CommonMark sets no ceiling and neither did this. A list
     * indented two thousand levels parsed fine and then took the renderer's
     * stack down with a `StackOverflowError`, which on a device is the app
     * disappearing the moment the note is opened. Not a crafted-input worry
     * alone: a generated outline or a file from another tool can carry
     * nesting no person would type.
     *
     * 64 rather than something tighter because the preview stops *showing*
     * nesting at `MaxIndentDepth` (6) — every level past that already draws at
     * the same indent — so this sits an order of magnitude beyond the last
     * level a reader can tell apart, and an order of magnitude under the depth
     * that faulted (measured on the JVM test runner: this renderer at 2,000
     * levels, commonmark's own visitors at 3,000; Android's main thread has a
     * larger stack than either). The deeper case is [parse]'s to contain —
     * a ceiling here cannot help when the parser never returns.
     */
    internal const val MAX_BLOCK_DEPTH = 64

    /**
     * The one row that stands in for everything below [MAX_BLOCK_DEPTH].
     *
     * Punctuation, so it needs no translation — the same choice
     * `SingleNoteWidget.TRUNCATION_MARKER` makes for the same reason. Cutting
     * visibly beats both crashing and dropping the subtree in silence.
     */
    internal const val DEPTH_CUT_MARKER = "…"

    /**
     * Open block parsers commonmark may stack while parsing: two per list
     * level (list + item) up to [MAX_BLOCK_DEPTH], with room to spare so the
     * structure reaches the renderer's cut rather than turning to text first.
     */
    internal const val MAX_PARSER_BLOCK_DEPTH = MAX_BLOCK_DEPTH * 2 + 16

    private val parser: Parser = buildParser(frontMatter = true)
    private val parserWithoutFrontMatter: Parser = buildParser(frontMatter = false)

    private fun buildParser(frontMatter: Boolean): Parser = Parser.builder()
        // Block spans give each ListItem the line it started on, which is what
        // lets a preview checkbox tap flip the right `[ ]` in the source (#219).
        // Block-level is enough — we never need to locate an inline node — and
        // it keeps the extra bookkeeping off every piece of text.
        .includeSourceSpans(IncludeSourceSpans.BLOCKS)
        // commonmark 0.30 caps open block parsers at 100 by default, and every
        // list level opens two (the list and its item) — so the default would
        // flatten a list deeper than ~50 levels into text, under this file's
        // own ceiling. Sized so the renderer's MAX_BLOCK_DEPTH stays the one
        // that cuts, visibly; the cap still keeps commonmark's own recursion
        // thousands of levels short of where it overflowed (D082).
        .maxOpenBlockParsers(MAX_PARSER_BLOCK_DEPTH)
        .extensions(
            listOfNotNull(
                YamlFrontMatterExtension.create().takeIf { frontMatter },
                StrikethroughExtension.create(),
                FootnotesExtension.builder().inlineFootnotes(false).build(),
                TaskListItemsExtension.create(),
                TablesExtension.create(),
                // Before autolinks: post-processors run in the order added,
                // and an address inside `[[…]]` is part of the wikilink.
                WikilinkExtension,
                // Bare `https://…`, `www.…` and email addresses become links,
                // as they do on GitHub (D082). Code spans and existing links
                // are left alone by the extension.
                AutolinkExtension.create()
            )
        )
        .build()

    /**
     * True when [markdown] opens with `---` that nothing closes. The
     * front-matter extension reads such a block to the end of the note, keeps
     * only `key: value` lines and drops everything else — so `---` followed by
     * prose rendered nothing, and a note that was only `---` lost its rule.
     * CommonMark reads both as a thematic break, which is what they are; the
     * PDF export asks the same question (review of #491).
     */
    internal fun opensUnclosedFrontMatter(markdown: String): Boolean {
        val lines = markdown.lineSequence().iterator()
        if (!lines.hasNext() || lines.next().trimEnd() != "---") return false
        while (lines.hasNext()) {
            val line = lines.next().trimEnd()
            if (line == "---" || line == "...") return false
        }
        return true
    }

    /**
     * Parses [markdown] into preview rows, degrading to plain text rather than
     * taking the process down.
     *
     * [MAX_BLOCK_DEPTH] bounds *this file's* recursion, which is all it can
     * bound: commonmark walks the same tree with its own recursive visitors —
     * the task-list post-processor faults first, measured at 3,000 nesting
     * levels — and a note deep enough to overflow it never reaches the
     * renderer at all. Since commonmark 0.30 the parser caps open blocks
     * ([MAX_PARSER_BLOCK_DEPTH] here) and inline nesting (100), which keeps its
     * own visitors off that edge; the catch stays because a cap in someone else's
     * library is not a guarantee this file can see from here (D082).
     *
     * `StackOverflowError` is an `Error`, and catching one is normally wrong
     * because the JVM's state after it is not worth trusting. This is the case
     * the rule is written around: the stack is fully unwound by the time the
     * catch runs, the parser is stateless between calls (a fresh document
     * parser per `parse`), and the only value at risk is the local list being
     * built. Weighed against that, the alternative is the app vanishing when
     * someone opens a note — and the note is still theirs to read and edit,
     * which is exactly what the fallback leaves them with.
     */
    fun parse(markdown: String): List<PreviewLine> = try {
        parseStructured(markdown)
    } catch (overflow: StackOverflowError) {
        plainTextRows(markdown)
    }

    /**
     * Every line as its own body row — the preview when the document is too
     * deeply nested to render as structure.
     *
     * No inline segments and no block types: this runs precisely when walking
     * the tree is what failed, so it walks nothing. Blank lines are dropped
     * because a `BODY` row of empty text draws as a gap the source did not ask
     * for.
     */
    internal fun plainTextRows(markdown: String): List<PreviewLine> =
        markdown.lines()
            .filter { it.isNotBlank() }
            .map { PreviewLine(text = it.trimEnd(), type = PreviewLineType.BODY) }

    private fun parseStructured(markdown: String): List<PreviewLine> {

        val document = try {
            (if (opensUnclosedFrontMatter(markdown)) parserWithoutFrontMatter else parser)
                .parse(markdown) as Document
        } catch (tooManyCells: IllegalArgumentException) {
            // commonmark 0.30 aborts a table past a million cells rather than
            // spend memory on it. Only the parse is guarded, so an
            // IllegalArgumentException from this file's own code still fails
            // loudly; the note itself is shown as its lines (D082).
            return plainTextRows(markdown)
        }
        val out = mutableListOf<PreviewLine>()
        val frontmatter = collectFrontmatter(document)
        if (frontmatter != null) {
            // Front matter can only open the note, so its line is known without
            // asking the parser for a span.
            out += PreviewLine(text = frontmatter, type = PreviewLineType.FRONTMATTER, startLine = 0)
        }

        var node: Node? = document.firstChild
        while (node != null) {
            renderBlock(node, out, depth = 0)
            node = node.next
        }
        return applyCollapsibleRanges(out)
    }

    /**
     * Renders one block node into [out] at [depth].
     *
     * [depth] is 0 for a block at document level and grows by one for each list
     * item we descend into, so a nested list — or a continuation paragraph,
     * quote or code block written underneath a list item — arrives as its own
     * row that knows how far in it belongs (#339).
     *
     * [nesting] counts every container descended through — list items *and*
     * quotes — and is what [MAX_BLOCK_DEPTH] bounds. It is separate from
     * [depth] because a quote nests without indenting: `>>>>` keeps [depth] at
     * 0 while recursing just as deeply as a list does.
     */
    private fun renderBlock(node: Node, out: MutableList<PreviewLine>, depth: Int, nesting: Int = depth) {
        if (nesting > MAX_BLOCK_DEPTH) {
            // One marker per cut, not one per sibling: consecutive siblings
            // below the ceiling all mean the same thing to the reader.
            if (out.lastOrNull()?.text != DEPTH_CUT_MARKER) {
                out += PreviewLine(
                    text = DEPTH_CUT_MARKER,
                    type = PreviewLineType.BODY,
                    depth = minOf(depth, MAX_BLOCK_DEPTH)
                )
            }
            return
        }
        val firstRow = out.size
        when (node) {
            is YamlFrontMatterBlock -> { /* already consumed by collectFrontmatter */ }
            is Heading -> out += renderHeading(node)
            is Paragraph -> renderParagraph(node, out, depth, PreviewLineType.BODY)
            is BulletList -> renderBulletList(node, out, depth, nesting)
            is OrderedList -> renderOrderedList(node, out, depth, nesting)
            is BlockQuote -> renderBlockQuote(node, out, depth, nesting)
            is FencedCodeBlock -> out += PreviewLine(
                text = node.literal.trimEnd(),
                type = PreviewLineType.CODE_BLOCK,
                extra = node.info?.takeIf { it.isNotEmpty() },
                depth = depth
            )
            is IndentedCodeBlock -> out += PreviewLine(
                text = node.literal.trimEnd(),
                type = PreviewLineType.CODE_BLOCK,
                depth = depth
            )
            is ThematicBreak -> out += PreviewLine(
                text = "",
                type = PreviewLineType.HORIZONTAL_RULE,
                depth = depth
            )
            is TableBlock -> out += renderTable(node, depth)
            is FootnoteDefinition -> out += renderFootnoteDefinition(node, depth)
            is HtmlBlock -> renderHtmlBlock(node, out, depth)
            else -> {
                // Unknown node: render as body so we don't drop content.
                val text = collectText(node)
                if (text.isNotBlank()) {
                    out += PreviewLine(
                        text = text,
                        type = PreviewLineType.BODY,
                        segments = collectInlineSegments(node),
                        depth = depth
                    )
                }
            }
        }
        // Every row this block just produced starts where the block does (#464)
        // — unless a row already knows better. A list item stamps its own line
        // in renderListItem, and a block nested inside a list item stamped
        // itself when its own renderBlock ran; only what is still unset is
        // left to this block's first line.
        val blockLine = sourceLineOf(node) ?: return
        for (i in firstRow until out.size) {
            if (out[i].startLine == null) out[i] = out[i].copy(startLine = blockLine)
        }
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
            3 -> PreviewLineType.H3
            4 -> PreviewLineType.H4
            5 -> PreviewLineType.H5
            // commonmark caps ATX headings at 6, so `else` is 6 rather than a
            // fallback for anything deeper.
            else -> PreviewLineType.H6
        }
        val text = collectText(node)
        return PreviewLine(
            text = text,
            type = type,
            segments = collectInlineSegments(node),
            // The outline jumps the *editor* to a heading, so it needs the line
            // the heading sits on in the source — the rendered list index only
            // ever located it in the preview (#215). Same block spans the
            // checkbox toggle already relies on.
            sourceLine = sourceLineOf(node)
        )
    }

    /**
     * A paragraph as one [type] row — or, when it carries images, as the text
     * and image rows it reads as in order.
     *
     * An image is drawn as a block of its own (IMAGE), so a paragraph that
     * mixes one with text is split at each image: the text before it, the
     * image, the text after. This used to happen only for a paragraph that was
     * *nothing but* an image; any other image fell through to the inline walk,
     * which kept its alt text and dropped the picture — `see ![chart](a.png)
     * below` read as "see chart below" with no image and no sign one was meant
     * to be there. Same for an `<img>` tag written inline.
     */
    private fun renderParagraph(node: Paragraph, out: MutableList<PreviewLine>, depth: Int, type: PreviewLineType) {
        val pieces = paragraphPieces(node)
        if (pieces.size == 1 && pieces[0] is ParagraphPiece.Inline) {
            // The common case, kept exactly as it was: the flat text comes from
            // the whole paragraph, not from the pieces.
            out += PreviewLine(
                text = collectText(node),
                type = type,
                segments = collectInlineSegments(node),
                depth = depth
            )
            return
        }
        for (piece in pieces) out += piece.toRow(type, depth)
    }

    /** One run of a paragraph between its images, or one of the images. */
    private sealed interface ParagraphPiece {
        fun toRow(textType: PreviewLineType, depth: Int): PreviewLine

        data class Inline(val segments: List<PreviewInlineSegment>) : ParagraphPiece {
            override fun toRow(textType: PreviewLineType, depth: Int) = PreviewLine(
                text = segments.joinToString("") { it.text }.replace('\n', ' ').trim(),
                type = textType,
                segments = segments,
                depth = depth
            )
        }

        data class Picture(val alt: String, val destination: String) : ParagraphPiece {
            override fun toRow(textType: PreviewLineType, depth: Int) = PreviewLine(
                text = alt,
                type = PreviewLineType.IMAGE,
                extra = destination,
                depth = depth
            )
        }
    }

    /**
     * Splits [node]'s inline children at every image. Only images that are
     * direct children split — one nested inside a link (a linked badge) stays
     * part of that link, whose label is what the reader taps. Runs left blank
     * by the split (the line break beside an image on its own line) are
     * dropped; a paragraph with no image comes back as one [ParagraphPiece.Inline].
     */
    private fun paragraphPieces(node: Node): List<ParagraphPiece> {
        val pieces = mutableListOf<ParagraphPiece>()
        var run = mutableListOf<PreviewInlineSegment>()
        fun flush() {
            val trimmed = run.coalesce().trimBlankEdges()
            if (trimmed.isNotEmpty()) pieces += ParagraphPiece.Inline(trimmed)
            run = mutableListOf()
        }
        var child: Node? = node.firstChild
        while (child != null) {
            val picture = when (child) {
                is Image -> ParagraphPiece.Picture(collectText(child), child.destination.orEmpty())
                is HtmlInline -> htmlImage(child.literal)
                else -> null
            }
            if (picture != null) {
                flush()
                pieces += picture
            } else {
                appendInlineNode(child, run, PreviewInlineType.TEXT)
            }
            child = child.next
        }
        flush()
        // A paragraph made only of whitespace still renders the way it did.
        return pieces.ifEmpty { listOf(ParagraphPiece.Inline(collectInlineSegments(node))) }
    }

    /** `<img src="…" alt="…">` as a picture, or null for any other tag or one without a source. */
    private fun htmlImage(tag: String): ParagraphPiece.Picture? {
        if (!HTML_IMG_TAG_REGEX.matches(tag.trim())) return null
        val src = htmlAttribute(tag, "src") ?: return null
        return ParagraphPiece.Picture(alt = htmlAttribute(tag, "alt").orEmpty(), destination = src)
    }

    private fun htmlAttribute(tag: String, name: String): String? =
        Regex("""(?:^|\s)$name\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""", RegexOption.IGNORE_CASE)
            .find(tag)
            ?.groupValues
            ?.drop(1)
            ?.firstOrNull { it.isNotEmpty() }
            ?.let(::decodeHtmlEntities)

    /** Drops whitespace-only runs (line breaks, spaces) from both ends of a run. */
    private fun List<PreviewInlineSegment>.trimBlankEdges(): List<PreviewInlineSegment> {
        var start = 0
        var end = size
        while (start < end && this[start].type == PreviewInlineType.TEXT && this[start].text.isBlank()) start++
        while (end > start && this[end - 1].type == PreviewInlineType.TEXT && this[end - 1].text.isBlank()) end--
        if (start == end) return emptyList()
        val trimmed = subList(start, end).toMutableList()
        trimmed[0].takeIf { it.type == PreviewInlineType.TEXT }?.let {
            trimmed[0] = it.copy(text = it.text.trimStart())
        }
        trimmed[trimmed.lastIndex].takeIf { it.type == PreviewInlineType.TEXT }?.let {
            trimmed[trimmed.lastIndex] = it.copy(text = it.text.trimEnd())
        }
        return trimmed
    }

    private fun renderBulletList(node: BulletList, out: MutableList<PreviewLine>, depth: Int, nesting: Int) {
        val loose = !node.isTight
        var item: Node? = node.firstChild
        while (item != null) {
            if (item is ListItem) {
                val marker = detectTaskMarker(item)
                val type = when (marker) {
                    TaskState.DONE -> PreviewLineType.CHECKBOX_DONE
                    TaskState.TODO -> PreviewLineType.CHECKBOX_TODO
                    TaskState.NONE -> PreviewLineType.BULLET
                }
                renderListItem(
                    item = item,
                    out = out,
                    depth = depth,
                    type = type,
                    extra = null,
                    // Only task items need it, and only they can be tapped.
                    sourceLine = if (marker == TaskState.NONE) null else sourceLineOf(item),
                    looseList = loose,
                    nesting = nesting
                )
            }
            item = item.next
        }
    }

    private fun renderOrderedList(node: OrderedList, out: MutableList<PreviewLine>, depth: Int, nesting: Int) {
        val loose = !node.isTight
        var item: Node? = node.firstChild
        var index = node.markerStartNumber ?: 1
        while (item != null) {
            if (item is ListItem) {
                renderListItem(
                    item = item,
                    out = out,
                    depth = depth,
                    type = PreviewLineType.ORDERED_LIST,
                    extra = index.toString(),
                    sourceLine = null,
                    looseList = loose,
                    nesting = nesting
                )
                index++
            }
            item = item.next
        }
    }

    /**
     * Emits one list row, then whatever else the item carries underneath it.
     *
     * The row's own text is the item's *first* paragraph and nothing more.
     * Every block child after it — a nested list, a continuation paragraph, a
     * quote, a fenced block — becomes its own row one level deeper. Folding the
     * whole subtree into the row instead was the bug behind #339: `- a` with a
     * nested `- b` rendered as the single row `• ab`, and a nested `- [ ] task`
     * lost its checkbox entirely because only the outermost item ever reached
     * the renderer.
     */
    private fun renderListItem(
        item: ListItem,
        out: MutableList<PreviewLine>,
        depth: Int,
        type: PreviewLineType,
        extra: String?,
        sourceLine: Int?,
        looseList: Boolean,
        nesting: Int
    ) {
        // The task-list extension puts its marker ahead of the paragraph, so
        // the item's own text starts one node later for a checklist row.
        val lead = item.firstChild.let { if (it is TaskListItemMarker) it.next else it } as? Paragraph
        // The item's own row takes the lead paragraph's first run of text; an
        // image in it, and whatever follows that image, go underneath like any
        // other block the item carries.
        val leadPieces = lead?.let { paragraphPieces(it) }.orEmpty()
        val plainLead = leadPieces.isEmpty() || (leadPieces.size == 1 && leadPieces[0] is ParagraphPiece.Inline)
        val ownText = leadPieces.firstOrNull() as? ParagraphPiece.Inline
        val itemRow = PreviewLine(
            text = if (plainLead) lead?.let { collectText(it) }.orEmpty() else ownText?.toRow(type, depth)?.text.orEmpty(),
            type = type,
            extra = extra,
            segments = if (plainLead) lead?.let { collectInlineSegments(it) }.orEmpty() else ownText?.segments.orEmpty(),
            sourceLine = sourceLine,
            depth = depth,
            looseList = looseList,
            // The item's own line, not the list's: renderBlock would otherwise
            // hand every item the first item's line (#464).
            startLine = sourceLineOf(item)
        )
        out += itemRow
        if (!plainLead) {
            val rest = if (ownText != null) leadPieces.drop(1) else leadPieces
            for (piece in rest) {
                out += piece.toRow(PreviewLineType.BODY, depth + 1).copy(startLine = itemRow.startLine)
            }
        }
        // An item can open straight into a sublist (`-` on its own line), in
        // which case there is no lead paragraph to walk past. Written long-hand
        // rather than with an elvis: `lead?.next ?: item.firstChild` also falls
        // back when the lead paragraph simply has no sibling, which re-renders
        // that paragraph as a second row.
        var child: Node? = if (lead != null) lead.next else item.firstChild
        while (child != null) {
            if (child !is TaskListItemMarker) {
                renderBlock(child, out, depth + 1, nesting + 1)
            }
            child = child.next
        }
    }

    /**
     * A `>` quote, or a GitHub-style callout (`> [!NOTE]`), as rows.
     *
     * Every block the quote holds is rendered by [renderBlock] like any other,
     * then stamped as belonging to the quote — so a list, a code block or a
     * heading inside one keeps its own shape. This used to flatten each child
     * to its text instead: a list inside a quote became one row `ab`, a fenced
     * block (which has no text node to collect) became nothing at all, and a
     * callout's body lost its links, bold, lists and code the same way.
     */
    private fun renderBlockQuote(node: BlockQuote, out: MutableList<PreviewLine>, depth: Int, nesting: Int) {
        val head = CalloutHead.take(node)
        val calloutType = head?.type
        val first = out.size
        if (head != null) {
            out += PreviewLine(
                // The title, when the head line has one; the renderer labels
                // an untitled callout by its type.
                text = head.title,
                type = PreviewLineType.CALLOUT,
                extra = calloutType,
                depth = depth
            )
        }
        var child: Node? = node.firstChild
        while (child != null) {
            val next = child.next
            when {
                // A plain quote's paragraphs keep their own row type so they
                // read as quoted prose; inside a callout the box says that.
                child is Paragraph && calloutType == null ->
                    renderParagraph(child, out, depth, PreviewLineType.BLOCKQUOTE)
                else -> renderBlock(child, out, depth, nesting + 1)
            }
            child = next
        }
        for (i in first until out.size) {
            val row = out[i]
            out[i] = if (calloutType != null) {
                // Innermost callout wins: a callout nested in this one already
                // stamped its own rows before this loop reached them.
                row.copy(callout = row.callout ?: calloutType, containerDepth = depth)
            } else {
                row.copy(quoteDepth = row.quoteDepth + 1, containerDepth = depth)
            }
        }
        if (calloutType != null) {
            out[out.lastIndex] = out[out.lastIndex].copy(calloutEnd = true)
        }
    }

    private fun renderTable(block: TableBlock, depth: Int): PreviewLine {
        var headerCells = emptyList<CellOut>()
        val bodyRows = mutableListOf<List<CellOut>>()

        var section: Node? = block.firstChild
        while (section != null) {
            when (section) {
                is TableHead -> {
                    val headRow = section.firstChild as? TableRow
                    if (headRow != null) {
                        headerCells = collectCells(headRow)
                    }
                }
                is TableBody -> {
                    var bodyRow: Node? = section.firstChild
                    while (bodyRow != null) {
                        if (bodyRow is TableRow) {
                            bodyRows += collectCells(bodyRow)
                        }
                        bodyRow = bodyRow.next
                    }
                }
            }
            section = section.next
        }
        // Pad short body rows to header count so the renderer can use a fixed
        // column layout without per-row null checks.
        val width = headerCells.size.coerceAtLeast(bodyRows.maxOfOrNull { it.size } ?: 0)
        val emptyCell = CellOut(text = "", alignment = TableAlignment.LEFT, segments = emptyList())
        val paddedHeaderCells = headerCells + List(width - headerCells.size) { emptyCell }
        val paddedBodyRows = bodyRows.map { row -> row + List(width - row.size) { emptyCell } }
        return PreviewLine(
            text = "",
            type = PreviewLineType.TABLE,
            tableData = TableData(
                headers = paddedHeaderCells.map { it.text },
                rows = paddedBodyRows.map { row -> row.map { it.text } },
                alignments = paddedHeaderCells.map { it.alignment },
                headerSegments = paddedHeaderCells.map { it.segments },
                rowSegments = paddedBodyRows.map { row -> row.map { it.segments } }
            ),
            depth = depth
        )
    }

    private data class CellOut(
        val text: String,
        val alignment: TableAlignment,
        val segments: List<PreviewInlineSegment>
    )

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
                out += CellOut(
                    text = collectText(cell),
                    alignment = alignment,
                    segments = collectInlineSegments(cell)
                )
            }
            cell = cell.next
        }
        return out
    }

    private fun renderFootnoteDefinition(node: FootnoteDefinition, depth: Int): PreviewLine {
        val body = collectText(node)
        return PreviewLine(
            text = body,
            type = PreviewLineType.FOOTNOTE_DEF,
            extra = node.label,
            segments = collectInlineSegments(node),
            depth = depth
        )
    }

    /**
     * GitHub-style collapsible sections (#403): `<details>` / `<summary>` is
     * raw HTML, not a CommonMark extension — the spec already recognises
     * `details`/`summary` as block-level tags, so it parses today, just into
     * an [HtmlBlock] this renderer used to drop silently (an [HtmlBlock] has
     * no child nodes to fall through to [collectText]).
     *
     * A `<details>` opening tag and its matching `</details>` arrive as two
     * *separate* sibling [HtmlBlock]s whenever a blank line sits between them
     * — which is also GitHub's own documented way to get real Markdown
     * rendered inside the section — and this leans on [applyCollapsibleRanges]
     * to find the close and bucket whatever rendered in between. Without a
     * blank line, a `</details>` and the *next* tag fold into the same
     * literal instead: a self-contained `<details>…</details>` run (no block
     * structure inside, just inline formatting), a stray close sharing its
     * literal with whatever text follows it, or — the case a Codex review on
     * this PR caught — one section's close immediately followed by the next
     * section's open, which a version of this method that only asked "is
     * there an opening tag anywhere in here" answered by skipping the close
     * entirely and nesting the second section inside the first. So this walks
     * the literal left to right processing tag boundaries in the order they
     * actually appear, rather than branching once on which tag is present.
     */
    private fun renderHtmlBlock(node: HtmlBlock, out: MutableList<PreviewLine>, depth: Int) {
        val literal = node.literal
        var cursor = 0
        while (cursor < literal.length) {
            val openMatch = DETAILS_OPEN_TAG_REGEX.find(literal, cursor)
            val closeMatch = DETAILS_CLOSE_TAG_REGEX.find(literal, cursor)
            val next = earlierMatch(openMatch, closeMatch)
                ?: run {
                    // No <details>/</details> tag anywhere in the rest of this
                    // literal. At cursor == 0 that means none exists at all —
                    // an ordinary HTML block, whose text is shown rather than
                    // dropped (it used to be dropped, so a `<div>` or a
                    // `<p align="center">` simply vanished from the preview).
                    // Past that, a tag was already processed earlier in this
                    // same literal, and what remains is real trailing content
                    // sitting outside any section boundary.
                    if (cursor > 0) {
                        emitPlainHtmlBlockText(literal.substring(cursor), out, depth)
                    } else {
                        emitHtmlBlockContent(literal, out, depth)
                    }
                    return
                }
            if (next === closeMatch) {
                out += PreviewLine(text = "", type = PreviewLineType.COLLAPSIBLE_END, depth = depth)
                cursor = next.range.last + 1
                continue
            }
            // next is an opening <details>. Its own <summary> and close (if
            // either is even in this literal) can only belong to it if a
            // later sibling's opening tag does not sit before them — otherwise
            // they belong to a later section this same loop will reach on its
            // own. Compared against laterOpen specifically, not a boundary
            // position that the close itself could equal: this section's own
            // close is allowed to be exactly the next tag after it.
            val open = next
            val laterOpen = DETAILS_OPEN_TAG_REGEX.find(literal, open.range.last + 1)
            val boundary = laterOpen?.range?.first ?: literal.length
            val isOpenByDefault = DETAILS_OPEN_ATTRIBUTE_REGEX.containsMatchIn(open.groupValues[1])
            val summaryMatch = SUMMARY_TAG_REGEX.find(literal, open.range.last + 1)
                ?.takeIf { it.range.last < boundary }
            // Blank rather than a hard-coded "Details": the parser has no
            // Android Context to localize with, so an absent <summary> is
            // left for MarkdownPreviewList to fill in via stringResource, the
            // same split CalloutBox already uses for callout labels.
            val summaryText = summaryMatch?.groupValues?.get(1)
                ?.let { HTML_TAG_REGEX.replace(it, "") }
                ?.trim()
                .orEmpty()
            out += PreviewLine(
                text = summaryText,
                type = PreviewLineType.COLLAPSIBLE_SUMMARY,
                extra = if (isOpenByDefault) OPEN_MARKER else null,
                depth = depth,
                // The block's own start line, not this open tag's — accurate
                // for the common one-section-per-literal case and merely
                // approximate when several fell into one literal. Nothing
                // reads this for a second, more precise section yet.
                sourceLine = sourceLineOf(node)
            )
            val bodyStart = summaryMatch?.range?.last?.plus(1) ?: (open.range.last + 1)
            val closeMatchForThis = DETAILS_CLOSE_TAG_REGEX.find(literal, bodyStart)
                ?.takeIf { it.range.first < boundary }
            if (closeMatchForThis == null) {
                // Not closed in this literal: either a later sibling HtmlBlock
                // carries the close (applyCollapsibleRanges finds it), or nothing
                // in the note ever does. Either way, resume the scan right after
                // whatever this section's own <summary> ended on — the top of the
                // loop finds the next tag boundary, section or otherwise, itself.
                cursor = bodyStart
                continue
            }
            val innerText = HTML_TAG_REGEX.replace(literal.substring(bodyStart, closeMatchForThis.range.first), "").trim()
            if (innerText.isNotEmpty()) {
                out += PreviewLine(
                    text = innerText,
                    type = PreviewLineType.BODY,
                    segments = collectInlineSegmentsFromPlainText(innerText),
                    depth = depth + 1
                )
            }
            out += PreviewLine(text = "", type = PreviewLineType.COLLAPSIBLE_END, depth = depth)
            cursor = closeMatchForThis.range.last + 1
        }
    }

    /** Whichever of [a] and [b] starts earlier in the source, or the one present when only one is. */
    private fun earlierMatch(a: MatchResult?, b: MatchResult?): MatchResult? = when {
        a == null -> b
        b == null -> a
        else -> if (a.range.first <= b.range.first) a else b
    }

    /**
     * An HTML block that is not a collapsible section, as the text and images
     * it holds. The preview does not render HTML; it reads it the way a reader
     * would — tags removed, `<br>` and the end of a block-level element as line
     * breaks, entities decoded, `<img>` as an image row. A comment, a
     * `<script>` or a `<style>` is not something a reader sees in a browser,
     * so it stays hidden here too. Markdown inside the block is left as
     * written, as CommonMark does for an HTML block.
     */
    private fun emitHtmlBlockContent(literal: String, out: MutableList<PreviewLine>, depth: Int) {
        val html = HTML_HIDDEN_CONTENT_REGEX.replace(literal, "")
        var cursor = 0
        for (img in HTML_IMG_TAG_REGEX.findAll(html)) {
            emitHtmlText(html.substring(cursor, img.range.first), out, depth)
            htmlImage(img.value)?.let { out += it.toRow(PreviewLineType.BODY, depth) }
            cursor = img.range.last + 1
        }
        emitHtmlText(html.substring(cursor), out, depth)
    }

    private fun emitHtmlText(fragment: String, out: MutableList<PreviewLine>, depth: Int) {
        val withBreaks = HTML_LINE_BREAK_TAG_REGEX.replace(fragment, "\n")
        val text = decodeHtmlEntities(HTML_TAG_REGEX.replace(withBreaks, ""))
            .lines()
            .map { it.replace(HORIZONTAL_SPACE_RUN_REGEX, " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        if (text.isNotEmpty()) {
            out += PreviewLine(
                text = text,
                type = PreviewLineType.BODY,
                segments = listOf(PreviewInlineSegment(text, PreviewInlineType.TEXT)),
                depth = depth
            )
        }
    }

    /**
     * The handful of entities hand-written HTML actually uses, plus numeric
     * references. Not a full HTML5 table: anything unknown is left as written,
     * which is still readable.
     */
    private fun decodeHtmlEntities(text: String): String {
        if ('&' !in text) return text
        return HTML_ENTITY_REGEX.replace(text) { match ->
            val body = match.groupValues[1]
            when {
                body.startsWith("#x", ignoreCase = true) ->
                    body.drop(2).toIntOrNull(16)?.let(::codePointString)
                body.startsWith("#") -> body.drop(1).toIntOrNull()?.let(::codePointString)
                else -> NAMED_HTML_ENTITIES[body]
            } ?: match.value
        }
    }

    private fun codePointString(codePoint: Int): String? =
        if (Character.isValidCodePoint(codePoint) && codePoint != 0) String(Character.toChars(codePoint)) else null

    /**
     * Whatever text is left in an [HtmlBlock] literal once every `<details>`
     * / `</details>` tag in it has been consumed: a stray `</details>` can
     * still share its literal with real content that follows it (no blank
     * line means commonmark never gave that content its own paragraph), same
     * as any other raw HTML block that isn't a collapsible-section tag at all.
     */
    private fun emitPlainHtmlBlockText(text: String, out: MutableList<PreviewLine>, depth: Int) {
        val cleaned = HTML_TAG_REGEX.replace(text, "").trim()
        if (cleaned.isNotEmpty()) {
            out += PreviewLine(
                text = cleaned,
                type = PreviewLineType.BODY,
                segments = collectInlineSegmentsFromPlainText(cleaned),
                depth = depth
            )
        }
    }

    /**
     * Pairs each [PreviewLineType.COLLAPSIBLE_SUMMARY] with the
     * [PreviewLineType.COLLAPSIBLE_END] that follows it — a stack rather than
     * simple adjacency so a `<details>` written inside another one nests
     * instead of closing the wrong section — and stamps every row in between
     * with the enclosing section's freshly assigned [PreviewLine.collapsibleId].
     * `COLLAPSIBLE_END` rows exist only to drive this pass and never survive
     * it. A `<details>` with no closing tag anywhere in the note runs to the
     * end of the document, which is what an empty stack at the end already
     * produces without any extra handling.
     */
    private fun applyCollapsibleRanges(lines: List<PreviewLine>): List<PreviewLine> {
        // COLLAPSIBLE_END alone (no summary) happens for a stray `</details>`
        // with no matching open anywhere in the note — still needs stripping,
        // which is what makes this an "either" rather than a summary-only check.
        if (lines.none {
                it.type == PreviewLineType.COLLAPSIBLE_SUMMARY || it.type == PreviewLineType.COLLAPSIBLE_END
            }
        ) {
            return lines
        }
        var nextId = 0
        val openIds = ArrayDeque<Int>()
        val result = mutableListOf<PreviewLine>()
        for (line in lines) {
            if (line.type == PreviewLineType.COLLAPSIBLE_END) {
                if (openIds.isNotEmpty()) openIds.removeLast()
                continue
            }
            val tagged = if (openIds.isEmpty()) {
                line
            } else {
                line.copy(
                    collapsibleIds = openIds.toList(),
                    depth = line.depth + openIds.size,
                    // A quote or callout inside the section moves in with it.
                    containerDepth = if (line.quoteDepth > 0 || line.callout != null) {
                        line.containerDepth + openIds.size
                    } else {
                        line.containerDepth
                    }
                )
            }
            if (line.type == PreviewLineType.COLLAPSIBLE_SUMMARY) {
                val id = nextId++
                result += tagged.copy(collapsibleId = id)
                openIds.addLast(id)
            } else {
                result += tagged
            }
        }
        return result
    }

    /**
     * Inline-only segments for text that was never parsed as its own Markdown
     * block — the self-contained `<details>` body in [renderHtmlBlock]. Reuses
     * [SimpleMarkdownPreview]'s regex-based inline parser (the same one
     * [com.markleaf.notes.core.markdown.preview.MarkdownPreviewList]'s
     * `CalloutBox` leans on for the same reason) rather than commonmark's,
     * because there is no block node here to walk inline children of — just a
     * substring pulled out of a raw HTML literal.
     */
    private fun collectInlineSegmentsFromPlainText(text: String): List<PreviewInlineSegment> =
        SimpleMarkdownPreview.parseInlineSegments(text)

    /**
     * The 0-based source line a block started on, or null when spans are absent
     * (a hand-built node in a test, say). Never guessed — a wrong line here
     * would toggle somebody else's checkbox.
     */
    private fun sourceLineOf(node: Node): Int? =
        node.sourceSpans.firstOrNull()?.lineIndex

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

            // A space here even though the rendered segments now break the line
            // (#394), and deliberately so: this string is the *flat* form of a
            // block. It is what the outline lists a heading by, what a link's
            // label collapses to, and what a table cell holds — all places a
            // newline would either be dropped or break a single-line layout.
            // `PreviewLine.segments` is what the preview draws; `text` is what
            // everything else reads it as.
            override fun visit(softLineBreak: SoftLineBreak) {
                sb.append(' ')
            }

            override fun visit(hardLineBreak: HardLineBreak) {
                sb.append('\n')
            }

            // The flat form of a `<br>`, for the same reason a soft break is a
            // space here: see the comment above.
            override fun visit(htmlInline: HtmlInline) {
                if (HTML_BR_TAG_REGEX.matches(htmlInline.literal.trim())) sb.append(' ')
            }

            // As written, the way the text node it used to be read as was.
            override fun visit(customNode: CustomNode) {
                if (customNode is WikilinkNode) sb.append(customNode.source) else visitChildren(customNode)
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
            appendInlineNode(child, out, default)
            child = child.next
        }
    }

    private fun appendInlineNode(
        child: Node,
        out: MutableList<PreviewInlineSegment>,
        default: PreviewInlineType
    ) {
        when (child) {
            is Text -> out += PreviewInlineSegment(child.literal, default)
            // Found by WikilinkExtension before autolinks run; see Wikilinks.kt.
            is WikilinkNode -> out += PreviewInlineSegment(
                child.label,
                PreviewInlineType.WIKILINK,
                href = child.target
            )
            // Raw inline HTML is not drawn, but `<br>` is a line break the
            // author asked for — most often inside a table cell, where a
            // real newline would end the row. Dropping it ran the two lines
            // together ("l1<br>l2" read as "l1l2"). Any other tag is left
            // out and the text around it stays, as before.
            is HtmlInline -> if (HTML_BR_TAG_REGEX.matches(child.literal.trim())) {
                out += PreviewInlineSegment("\n", default)
            }
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
            // A single newline inside a paragraph breaks the line here,
            // where CommonMark folds it into a space (#394). The spec is
            // written for documents that are typeset after the fact; this
            // is a notes app whose editor shows the raw text, so a line the
            // author broke and then sees rejoined in preview reads as the
            // preview losing it. It is also worst exactly where it is least
            // expected: between CJK characters the inserted space is a
            // visible gap inside a sentence.
            //
            // Trailing two spaces still produce a HardLineBreak, which has
            // always broken the line — the two now agree instead of
            // disagreeing, and a blank line still starts a new paragraph
            // with the wider spacing that carries.
            //
            // The preview's own callouts have rendered one row per source
            // line since they were added (`CalloutBox` splits on "\n"), so
            // this brings the rest of the preview to what that half of it
            // already did.
            is SoftLineBreak -> out += PreviewInlineSegment("\n", default)
            is HardLineBreak -> out += PreviewInlineSegment("\n", default)
            is TaskListItemMarker -> { /* checkbox marker styled at line level */ }
            else -> appendInlineSegments(child, out, default)
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


    // #403 collapsible sections. IGNORE_CASE because HTML tag names are
    // case-insensitive; DOT_MATCHES_ALL on the summary body because it can
    // span a hard line break inside the same HtmlBlock literal.
    private val DETAILS_OPEN_TAG_REGEX = Regex("""<details\b([^>]*)>""", RegexOption.IGNORE_CASE)
    private val DETAILS_CLOSE_TAG_REGEX = Regex("""</details\s*>""", RegexOption.IGNORE_CASE)
    // \bopen\b alone also matched "open" inside an unrelated attribute's own
    // name or value (class="is-open", data-state="open-pending") since a
    // hyphen or quote is already a non-word character, satisfying \b without
    // this being the boolean `open` attribute at all (a self-review finding).
    // Requiring whitespace-or-start before it and whitespace/=/end after
    // confines the match to `open`, `open=`, or `open="..."` as a standalone
    // token.
    private val DETAILS_OPEN_ATTRIBUTE_REGEX = Regex("""(?:^|\s)open(?=\s|=|$)""", RegexOption.IGNORE_CASE)
    private val SUMMARY_TAG_REGEX = Regex(
        """<summary\b[^>]*>(.*?)</summary\s*>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    private val HTML_IMG_TAG_REGEX = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val HTML_BR_TAG_REGEX = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    /** `<br>`, and the closing tag of an element a browser starts a new line after. */
    private val HTML_LINE_BREAK_TAG_REGEX = Regex(
        """<br\s*/?>|</(?:p|div|li|h[1-6]|tr|blockquote|pre|table|ul|ol|dt|dd|section|article|header|footer)\s*>""",
        RegexOption.IGNORE_CASE
    )
    /** Comments, and the bodies of elements whose text never shows on a page. */
    private val HTML_HIDDEN_CONTENT_REGEX = Regex(
        """<!--.*?-->|<(script|style)\b[^>]*>.*?</\1\s*>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val HTML_ENTITY_REGEX = Regex("""&(#[0-9]{1,7}|#[xX][0-9a-fA-F]{1,6}|[a-zA-Z]{2,8});""")
    private val NAMED_HTML_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to "\u00A0",
        "copy" to "\u00A9", "reg" to "\u00AE", "trade" to "\u2122", "mdash" to "\u2014",
        "ndash" to "\u2013", "hellip" to "\u2026", "middot" to "\u00B7", "times" to "\u00D7"
    )
    private val HORIZONTAL_SPACE_RUN_REGEX = Regex("""[ \t\u00A0]+""")

    /** Sentinel [PreviewLine.extra] marking a `<details open>` section (#403). */
    internal const val OPEN_MARKER = "open"
}
