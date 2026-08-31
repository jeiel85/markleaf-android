package com.markleaf.notes.core.markdown

enum class PreviewLineType {
    H1,
    H2,
    H3,
    // H4–H6 used to collapse into H3, which was invisible while the preview was
    // the only consumer — three sizes read as "a heading" either way. The
    // outline screen made it visible: indentation is its only level cue, so a
    // note nesting past three levels rendered as a flat list (#255).
    H4,
    H5,
    H6,
    BULLET,
    CHECKBOX_DONE,
    CHECKBOX_TODO,
    BLOCKQUOTE,
    CALLOUT,
    ORDERED_LIST,
    HORIZONTAL_RULE,
    BODY,
    EMPTY,
    CODE_BLOCK,
    FRONTMATTER,
    FOOTNOTE_DEF,
    IMAGE,
    TABLE
}

/** Per-column alignment hint from the GFM table header separator `|:---|---:|`. */
enum class TableAlignment { LEFT, CENTER, RIGHT }

/** Parsed GFM table structure carried on [PreviewLine.tableData] when type == TABLE. */
data class TableData(
    val headers: List<String>,
    val rows: List<List<String>>,
    val alignments: List<TableAlignment>,
    /**
     * Per-cell inline segments mirroring [headers] / [rows], so links and inline
     * styles survive into table cells (#197). An empty list for a cell means
     * "render the plain string" — kept as a fallback so hand-built instances
     * (tests, legacy paths) keep working without segment data.
     */
    val headerSegments: List<List<PreviewInlineSegment>> = emptyList(),
    val rowSegments: List<List<List<PreviewInlineSegment>>> = emptyList()
)

enum class PreviewInlineType {
    TEXT,
    BOLD,
    ITALIC,
    BOLD_ITALIC,
    STRIKETHROUGH,
    INLINE_CODE,
    FOOTNOTE_REF,
    WIKILINK,
    LINK
}

enum class CalloutKind {
    NOTE,
    TIP,
    IMPORTANT,
    WARNING,
    CAUTION;

    companion object {
        fun parse(raw: String): CalloutKind? = when (raw.trim().uppercase()) {
            "NOTE" -> NOTE
            "TIP" -> TIP
            "IMPORTANT" -> IMPORTANT
            "WARNING", "WARN" -> WARNING
            "CAUTION", "DANGER" -> CAUTION
            else -> null
        }
    }
}

data class PreviewInlineSegment(
    val text: String,
    val type: PreviewInlineType,
    /** Only meaningful for [PreviewInlineType.LINK] — the URL the user tapped. */
    val href: String? = null
)

data class PreviewLine(
    val text: String,
    val type: PreviewLineType,
    val extra: String? = null,
    val segments: List<PreviewInlineSegment> = emptyList(),
    /** Only set when [type] is [PreviewLineType.TABLE]. */
    val tableData: TableData? = null,
    /**
     * 0-based index of the source line this row came from, when we can say for
     * certain. Set by the rows that have to point back at the note's text:
     * checklist items, because tapping a checkbox has to flip the `[ ]` on
     * exactly one line and counting checkboxes would drift the moment a
     * `- [ ]` appeared inside a fenced code block (#219); and headings, because
     * the outline scrolls the editor to one and a rendered-list index cannot
     * locate a caret (#215). Null means "this row cannot point back".
     */
    val sourceLine: Int? = null,
    /**
     * How deeply a list row is nested: 0 for a top-level item, 1 for an item
     * written underneath it, and so on. Set by [CommonMarkPreviewAdapter] as it
     * descends into a nested list or into the blocks a list item carries, and
     * used by the renderer to indent the row. Rows that are not part of a list
     * leave it at 0.
     */
    val depth: Int = 0,
    /**
     * True when this row belongs to a *loose* list — one whose items are
     * separated by blank lines in the source. CommonMark makes the
     * distinction and so should the rendering: a loose list is prose the
     * author spaced out on purpose, a tight one is a compact enumeration.
     */
    val looseList: Boolean = false,
    /**
     * Text-size multiplier for this row (#346), stamped by
     * `MarkdownPreviewList` from the user's setting. Defaults to 1f so the
     * parser, tests, and every existing call site render exactly as before.
     */
    val fontScale: Float = 1f
)

object SimpleMarkdownPreview {
    private data class InlineMatch(
        val start: Int,
        val end: Int,
        val type: PreviewInlineType,
        val displayText: String
    )

    private val boldItalicRegex = Regex("""\*\*\*(.+?)\*\*\*""")
    private val boldRegex = Regex("""\*\*(.+?)\*\*""")
    private val italicStarRegex = Regex("""(?<!\*)\*([^*\n]+?)\*(?!\*)""")
    private val italicUnderscoreRegex = Regex("""(?<!\w)_([^_\n]+?)_(?!\w)""")
    private val strikethroughRegex = Regex("""~~(.+?)~~""")
    private val inlineCodeRegex = Regex("""`([^`\n]+?)`""")
    private val footnoteRefRegex = Regex("""\[\^([A-Za-z0-9_-]+)](?!:)""")
    private val footnoteDefRegex = Regex("""^\[\^([A-Za-z0-9_-]+)]:\s*(.*)$""")
    private val calloutHeadRegex = Regex("""^>\s*\[!([A-Za-z]+)]\s*$""")

    /**
     * Parses a markdown string into [PreviewLine]s.
     *
     * Since v2.3.0, this delegates to [CommonMarkPreviewAdapter] (commonmark-java
     * + extensions). The hand-rolled implementation that lived here from v0.x
     * to v2.2.x is preserved as [parseHandRolled] below — kept for tests and
     * fallback debugging, but no longer the primary path.
     */
    fun parse(markdown: String): List<PreviewLine> = CommonMarkPreviewAdapter.parse(markdown)

    /** The original hand-rolled parser. Retained for parity tests during the v2.3 swap. */
    internal fun parseHandRolled(markdown: String): List<PreviewLine> {
        val rawLines = markdown.lines()
        val result = mutableListOf<PreviewLine>()
        var index = 0

        // Frontmatter: a leading `---` … `---` block at the top of the document.
        if (rawLines.isNotEmpty() && rawLines[0].trim() == "---") {
            val closeOffset = rawLines.subList(1, rawLines.size)
                .indexOfFirst { it.trim() == "---" }
            if (closeOffset >= 0) {
                val body = rawLines.subList(1, 1 + closeOffset).joinToString("\n")
                result += PreviewLine(text = body, type = PreviewLineType.FRONTMATTER)
                index = 1 + closeOffset + 1
            }
        }

        while (index < rawLines.size) {
            val line = rawLines[index].trimEnd()

            when {
                line.trim().startsWith("```") -> {
                    val language = line.trim().removePrefix("```").trim()
                    val codeLines = mutableListOf<String>()
                    index++
                    while (index < rawLines.size && !rawLines[index].trim().startsWith("```")) {
                        codeLines += rawLines[index]
                        index++
                    }
                    if (index < rawLines.size && rawLines[index].trim().startsWith("```")) {
                        index++
                    }
                    result += PreviewLine(
                        text = codeLines.joinToString("\n"),
                        type = PreviewLineType.CODE_BLOCK,
                        extra = language.takeIf { it.isNotEmpty() }
                    )
                }
                calloutHeadRegex.matches(line) -> {
                    val kind = calloutHeadRegex.find(line)!!.groupValues[1]
                    val bodyLines = mutableListOf<String>()
                    index++
                    while (index < rawLines.size && rawLines[index].trimEnd().startsWith(">")) {
                        val body = rawLines[index].trimEnd().removePrefix(">").let {
                            if (it.startsWith(" ")) it.removePrefix(" ") else it
                        }
                        bodyLines += body
                        index++
                    }
                    result += PreviewLine(
                        text = bodyLines.joinToString("\n"),
                        type = PreviewLineType.CALLOUT,
                        extra = kind
                    )
                }
                footnoteDefRegex.matches(line) -> {
                    val match = footnoteDefRegex.find(line)!!
                    val body = match.groupValues[2]
                    result += PreviewLine(
                        text = body,
                        type = PreviewLineType.FOOTNOTE_DEF,
                        extra = match.groupValues[1],
                        segments = parseInlineSegments(body)
                    )
                    index++
                }
                else -> {
                    result += parseLine(line)
                    index++
                }
            }
        }

        return result
    }

    fun parseInlineSegments(text: String): List<PreviewInlineSegment> {
        val allMatches = mutableListOf<InlineMatch>()

        boldItalicRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.BOLD_ITALIC, match.groupValues[1])
        }

        boldRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.BOLD, match.groupValues[1])
        }

        italicStarRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.ITALIC, match.groupValues[1])
        }

        italicUnderscoreRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.ITALIC, match.groupValues[1])
        }

        strikethroughRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.STRIKETHROUGH, match.groupValues[1])
        }

        inlineCodeRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.INLINE_CODE, match.groupValues[1])
        }

        footnoteRefRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.FOOTNOTE_REF, match.groupValues[1])
        }

        allMatches.sortWith(compareBy({ it.start }, { -(it.end - it.start) }))

        val resolvedMatches = mutableListOf<InlineMatch>()
        var lastEnd = -1
        for (match in allMatches) {
            if (match.start > lastEnd) {
                resolvedMatches += match
                lastEnd = match.end
            }
        }

        val segments = mutableListOf<PreviewInlineSegment>()
        var cursor = 0

        for (match in resolvedMatches) {
            if (match.start > cursor) {
                segments += PreviewInlineSegment(text.substring(cursor, match.start), PreviewInlineType.TEXT)
            }
            segments += PreviewInlineSegment(text = match.displayText, type = match.type)
            cursor = match.end + 1
        }

        if (cursor < text.length) {
            segments += PreviewInlineSegment(text.substring(cursor), PreviewInlineType.TEXT)
        }

        return if (segments.isEmpty()) {
            listOf(PreviewInlineSegment(text, PreviewInlineType.TEXT))
        } else {
            segments
        }
    }

    private fun parseLine(line: String): PreviewLine {
        return when {
            line.isBlank() -> PreviewLine("", PreviewLineType.EMPTY)
            // Longest marker first. `startsWith("### ")` is already false for
            // `#### ` — the fourth character is `#`, not a space — but ordering
            // by length keeps that from being something a reader has to work out.
            line.startsWith("###### ") ->
                PreviewLine(line.removePrefix("###### ").trim(), PreviewLineType.H6)
            line.startsWith("##### ") ->
                PreviewLine(line.removePrefix("##### ").trim(), PreviewLineType.H5)
            line.startsWith("#### ") ->
                PreviewLine(line.removePrefix("#### ").trim(), PreviewLineType.H4)
            line.startsWith("### ") -> PreviewLine(line.removePrefix("### ").trim(), PreviewLineType.H3)
            line.startsWith("## ") -> PreviewLine(line.removePrefix("## ").trim(), PreviewLineType.H2)
            line.startsWith("# ") -> PreviewLine(line.removePrefix("# ").trim(), PreviewLineType.H1)
            line.startsWith("- [x] ", ignoreCase = true) -> PreviewLine(
                line.removePrefix("- [x] ").trim(),
                PreviewLineType.CHECKBOX_DONE
            )
            line.startsWith("- [ ] ") -> PreviewLine(
                line.removePrefix("- [ ] ").trim(),
                PreviewLineType.CHECKBOX_TODO
            )
            line.startsWith("- ") -> PreviewLine(line.removePrefix("- ").trim(), PreviewLineType.BULLET)
            line.startsWith("> ") -> PreviewLine(line.removePrefix("> ").trim(), PreviewLineType.BLOCKQUOTE, segments = parseInlineSegments(line.removePrefix("> ").trim()))
            line.matches(Regex("""^\d+\.\s+.+""")) -> {
                val match = Regex("""^(\d+)\.\s+(.+)""").find(line)
                PreviewLine(
                    text = match!!.groupValues[2],
                    type = PreviewLineType.ORDERED_LIST,
                    extra = match.groupValues[1]
                )
            }
            line.matches(Regex("""^(---|\*\*\*|___)\s*$""")) -> PreviewLine("", PreviewLineType.HORIZONTAL_RULE)
            else -> PreviewLine(line, PreviewLineType.BODY, segments = parseInlineSegments(line))
        }
    }
}
