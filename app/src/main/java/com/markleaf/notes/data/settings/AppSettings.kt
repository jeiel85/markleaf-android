package com.markleaf.notes.data.settings

data class AppSettings(
    val markdownSyntaxVisibility: MarkdownSyntaxVisibility = MarkdownSyntaxVisibility.SHOW,
    val lineWidth: EditorLineWidth = EditorLineWidth.COMFORTABLE,
    val screenshotProtection: Boolean = false,
    val syncFolderUri: String? = null,
    val syncLastSyncedAt: Long? = null,
    val colorPalette: ColorPalette = ColorPalette.MARKLEAF_GREEN
)

enum class ColorPalette {
    /** Static green palette evoking the leaf in the app name. The original v0.x identity. */
    MARKLEAF_GREEN,

    /** System wallpaper-derived dynamic colors on Android 12+; falls back to green below S. */
    MATERIAL_YOU
}

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
