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
    BODY,
    EMPTY
}

enum class PreviewInlineType {
    TEXT,
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
    private val inlinePattern = Regex("""\[\[([^\]]+)]]|\[([^\]]+)]\(([^)]+)\)|(?<!\\)\$([^$\n]+)(?<!\\)\$""")
    private val tableDividerPattern = Regex("""^\s*\|?\s*:?-{3,}:?\s*(\|\s*:?-{3,}:?\s*)+\|?\s*$""")

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
        val matches = inlinePattern.findAll(text).toList()
        if (matches.isEmpty()) {
            return listOf(PreviewInlineSegment(text, PreviewInlineType.TEXT))
        }

        val segments = mutableListOf<PreviewInlineSegment>()
        var cursor = 0

        matches.forEach { match ->
            if (match.range.first > cursor) {
                segments += PreviewInlineSegment(
                    text = text.substring(cursor, match.range.first),
                    type = PreviewInlineType.TEXT
                )
            }

            val noteTitle = match.groups[1]?.value?.trim()
            val markdownLabel = match.groups[2]?.value
            val markdownTarget = match.groups[3]?.value?.trim()
            val inlineMath = match.groups[4]?.value?.trim()

            if (!noteTitle.isNullOrBlank()) {
                segments += PreviewInlineSegment(
                    text = noteTitle,
                    type = PreviewInlineType.NOTE_LINK,
                    target = noteTitle
                )
            } else if (!markdownLabel.isNullOrBlank() && !markdownTarget.isNullOrBlank()) {
                segments += PreviewInlineSegment(
                    text = markdownLabel,
                    type = PreviewInlineType.MARKDOWN_LINK,
                    target = markdownTarget
                )
            } else if (!inlineMath.isNullOrBlank()) {
                segments += PreviewInlineSegment(
                    text = inlineMath,
                    type = PreviewInlineType.INLINE_MATH
                )
            }

            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            segments += PreviewInlineSegment(text.substring(cursor), PreviewInlineType.TEXT)
        }

        return segments
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
