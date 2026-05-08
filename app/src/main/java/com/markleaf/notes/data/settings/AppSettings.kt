package com.markleaf.notes.data.settings

data class AppSettings(
    val markdownSyntaxVisibility: MarkdownSyntaxVisibility = MarkdownSyntaxVisibility.SHOW,
    val lineWidth: EditorLineWidth = EditorLineWidth.COMFORTABLE
)

enum class MarkdownSyntaxVisibility {
    SHOW,
    HIDE
}

enum class EditorLineWidth(
    val label: String,
    val maxWidthDp: Int
) {
    NARROW("Narrow", 640),
    COMFORTABLE("Comfortable", 800),
    WIDE("Wide", 960)
}
