package com.markleaf.notes.core.markdown

enum class PreviewLineType {
    H1,
    H2,
    H3,
    BULLET,
    CHECKBOX_DONE,
    CHECKBOX_TODO,
    IMAGE,
    LINK,
    BODY,
    EMPTY
}

enum class PreviewInlineType {
    TEXT,
    NOTE_LINK,
    MARKDOWN_LINK
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
    val segments: List<PreviewInlineSegment> = emptyList()
)

object SimpleMarkdownPreview {
    private val inlineLinkPattern = Regex("""\[\[([^\]]+)]]|\[([^\]]+)]\(([^)]+)\)""")

    fun parse(markdown: String): List<PreviewLine> {
        return markdown
            .lines()
            .map { raw ->
                val line = raw.trimEnd()
                when {
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
    }

    fun parseInlineSegments(text: String): List<PreviewInlineSegment> {
        val matches = inlineLinkPattern.findAll(text).toList()
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
            }

            cursor = match.range.last + 1
        }

        if (cursor < text.length) {
            segments += PreviewInlineSegment(text.substring(cursor), PreviewInlineType.TEXT)
        }

        return segments
    }
}
