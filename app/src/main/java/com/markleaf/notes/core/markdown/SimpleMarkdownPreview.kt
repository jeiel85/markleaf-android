package com.markleaf.notes.core.markdown

enum class PreviewLineType {
    H1,
    H2,
    H3,
    TABLE_HEADER,
    TABLE_ROW,
    BULLET,
    CHECKBOX_DONE,
    CHECKBOX_TODO,
    IMAGE,
    LINK,
    MATH_BLOCK,
    BLOCKQUOTE,
    ORDERED_LIST,
    HORIZONTAL_RULE,
    BODY,
    EMPTY
}

enum class PreviewInlineType {
    TEXT,
    BOLD,
    ITALIC,
    BOLD_ITALIC,
    STRIKETHROUGH,
    INLINE_CODE,
    NOTE_LINK,
    MARKDOWN_LINK,
    INLINE_MATH
}

data class PreviewInlineSegment(
    val text: String,
    val type: PreviewInlineType,
    val target: String? = null
)

data class PreviewLine(
    val text: String,
    val type: PreviewLineType,
    val extra: String? = null, // For image URI or other metadata
    val segments: List<PreviewInlineSegment> = emptyList(),
    val cells: List<String> = emptyList()
)

object SimpleMarkdownPreview {
    private data class InlineMatch(
        val start: Int,
        val end: Int,
        val type: PreviewInlineType,
        val displayText: String,
        val target: String? = null
    )

    private val tableDividerPattern = Regex("""^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$""")

    private val boldItalicRegex = Regex("""\*\*\*(.+?)\*\*\*""")
    private val boldRegex = Regex("""\*\*(.+?)\*\*""")
    private val italicStarRegex = Regex("""(?<!\*)\*([^*\n]+?)\*(?!\*)""")
    private val italicUnderscoreRegex = Regex("""(?<!\w)_([^_\n]+?)_(?!\w)""")
    private val strikethroughRegex = Regex("""~~(.+?)~~""")
    private val inlineCodeRegex = Regex("""`([^`\n]+?)`""")
    private val wikiLinkRegex = Regex("""\[\[([^\]]+)]]""")
    private val markdownLinkRegex = Regex("""\[([^\]]+)\]\(([^)]+)\)""")
    private val inlineMathRegex = Regex("""(?<!\\)\$([^$\n]+?)(?<!\\)\$""")

    fun parse(markdown: String): List<PreviewLine> {
        val rawLines = markdown.lines()
        val result = mutableListOf<PreviewLine>()
        var index = 0

        while (index < rawLines.size) {
            val line = rawLines[index].trimEnd()
            val nextLine = rawLines.getOrNull(index + 1)?.trimEnd()

            when {
                isTableHeader(line, nextLine) -> {
                    result += PreviewLine(
                        text = line,
                        type = PreviewLineType.TABLE_HEADER,
                        cells = parseTableCells(line)
                    )
                    index += 2
                    while (index < rawLines.size && isTableRow(rawLines[index])) {
                        val tableLine = rawLines[index].trimEnd()
                        result += PreviewLine(
                            text = tableLine,
                            type = PreviewLineType.TABLE_ROW,
                            cells = parseTableCells(tableLine)
                        )
                        index++
                    }
                }
                line.trim() == "$$" -> {
                    val mathLines = mutableListOf<String>()
                    index++
                    while (index < rawLines.size && rawLines[index].trim() != "$$") {
                        mathLines += rawLines[index]
                        index++
                    }
                    if (index < rawLines.size && rawLines[index].trim() == "$$") {
                        index++
                    }
                    result += PreviewLine(
                        text = mathLines.joinToString("\n").trim(),
                        type = PreviewLineType.MATH_BLOCK
                    )
                }
                line.trim().startsWith("$$") && line.trim().endsWith("$$") && line.trim().length > 4 -> {
                    result += PreviewLine(
                        text = line.trim().removeSurrounding("$$").trim(),
                        type = PreviewLineType.MATH_BLOCK
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

        wikiLinkRegex.findAll(text).forEach { match ->
            val title = match.groupValues[1].trim()
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.NOTE_LINK, title, title)
        }

        markdownLinkRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.MARKDOWN_LINK, match.groupValues[1], match.groupValues[2].trim())
        }

        inlineMathRegex.findAll(text).forEach { match ->
            allMatches += InlineMatch(match.range.first, match.range.last, PreviewInlineType.INLINE_MATH, match.groupValues[1].trim())
        }

        // Sort by start position, then longer matches first for same start
        allMatches.sortWith(compareBy({ it.start }, { -(it.end - it.start) }))

        // Resolve overlaps: keep non-overlapping matches from left to right
        val resolvedMatches = mutableListOf<InlineMatch>()
        var lastEnd = -1
        for (match in allMatches) {
            if (match.start > lastEnd) {
                resolvedMatches += match
                lastEnd = match.end
            }
        }

        // Build segments from resolved matches
        val segments = mutableListOf<PreviewInlineSegment>()
        var cursor = 0

        for (match in resolvedMatches) {
            if (match.start > cursor) {
                segments += PreviewInlineSegment(text.substring(cursor, match.start), PreviewInlineType.TEXT)
            }
            segments += PreviewInlineSegment(text = match.displayText, type = match.type, target = match.target)
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
            line.startsWith("![") && line.contains("](") && line.endsWith(")") -> {
                val alt = line.substringAfter("![").substringBefore("]")
                val uri = line.substringAfter("](").substringBeforeLast(")")
                PreviewLine(alt, PreviewLineType.IMAGE, extra = uri)
            }
            line.startsWith("[[") && line.contains("]]") -> {
                val title = line.substringAfter("[[").substringBefore("]]").trim()
                PreviewLine(title, PreviewLineType.LINK)
            }
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

    private fun isTableHeader(line: String, nextLine: String?): Boolean {
        return isTableRow(line) && nextLine != null && tableDividerPattern.matches(nextLine)
    }

    private fun isTableRow(line: String): Boolean {
        val trimmed = line.trim()
        return trimmed.contains("|") && parseTableCells(trimmed).size >= 2
    }

    private fun parseTableCells(line: String): List<String> {
        return line
            .trim()
            .trim('|')
            .split('|')
            .map { it.trim() }
    }
}
