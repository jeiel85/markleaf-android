package com.markleaf.notes.core.markdown

enum class PreviewLineType {
    H1,
    H2,
    H3,
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
    FOOTNOTE_DEF
}

enum class PreviewInlineType {
    TEXT,
    BOLD,
    ITALIC,
    BOLD_ITALIC,
    STRIKETHROUGH,
    INLINE_CODE,
    FOOTNOTE_REF
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
    val type: PreviewInlineType
)

data class PreviewLine(
    val text: String,
    val type: PreviewLineType,
    val extra: String? = null,
    val segments: List<PreviewInlineSegment> = emptyList()
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
