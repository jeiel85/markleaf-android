package com.markleaf.notes.core.markdown

enum class PreviewLineType {
    H1,
    H2,
    H3,
    BULLET,
    CHECKBOX_DONE,
    CHECKBOX_TODO,
    BODY,
    EMPTY
}

data class PreviewLine(
    val text: String,
    val type: PreviewLineType
)

object SimpleMarkdownPreview {
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
                    line.startsWith("- [x] ", ignoreCase = true) -> PreviewLine(
                        line.removePrefix("- [x] ").trim(),
                        PreviewLineType.CHECKBOX_DONE
                    )
                    line.startsWith("- [ ] ") -> PreviewLine(
                        line.removePrefix("- [ ] ").trim(),
                        PreviewLineType.CHECKBOX_TODO
                    )
                    line.startsWith("- ") -> PreviewLine(line.removePrefix("- ").trim(), PreviewLineType.BULLET)
                    else -> PreviewLine(line, PreviewLineType.BODY)
                }
            }
    }
}
