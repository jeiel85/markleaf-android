package com.markleaf.notes.data.settings

data class AppSettings(
    val markdownSyntaxVisibility: MarkdownSyntaxVisibility = MarkdownSyntaxVisibility.SHOW,
    val lineWidth: EditorLineWidth = EditorLineWidth.COMFORTABLE,
    val toolbarConfig: ToolbarConfig = ToolbarConfig()
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

data class ToolbarConfig(
    val showBold: Boolean = true,
    val showItalic: Boolean = true,
    val showCheckbox: Boolean = true,
    val showMarkdownLink: Boolean = true,
    val showWikiLink: Boolean = true,
    val showImage: Boolean = true
) {
    companion object {
        val DEFAULT = ToolbarConfig()
    }
}
