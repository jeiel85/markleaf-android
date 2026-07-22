package com.markleaf.notes.data.settings

data class AppSettings(
    val markdownSyntaxVisibility: MarkdownSyntaxVisibility = MarkdownSyntaxVisibility.SHOW,
    val lineWidth: EditorLineWidth = EditorLineWidth.COMFORTABLE,
    val editorFont: EditorFont = EditorFont.SANS,
    val screenshotProtection: Boolean = false,
    val syncFolderUri: String? = null,
    val syncLastSyncedAt: Long? = null,
    val syncFileExtension: SyncFileExtension = SyncFileExtension.MD,
    val colorPalette: ColorPalette = ColorPalette.MARKLEAF_GREEN,
    val onboardingCompleted: Boolean = false,
    val biometricLockEnabled: Boolean = false,
    /** True when a "Locked notes" passcode has been set (#155). The passcode
     *  hash/salt themselves stay inside [AppSettingsRepository]; only this
     *  derived flag is exposed so UI can decide whether to offer locking and
     *  whether to prompt for setup. */
    val lockPasscodeSet: Boolean = false,
    /** Show the excerpt and edit-time lines under each title in the notes list
     *  and search results. Off collapses rows to title-only so more notes fit
     *  on screen (#188). */
    val notesShowPreview: Boolean = true,
    /** Reopen the last-edited note on a plain launch instead of landing on the
     *  notes list. Opt-in (#192). */
    val reopenLastNote: Boolean = false,
    /** Sort order of the notes list, driven by the top-bar sort menu (#191). */
    val notesSortMode: NotesSortMode = NotesSortMode.UPDATED_DESC,
    /** When true the Search screen matches note titles only (the Quick Access
     *  behaviour); full-text search is one toggle away (#193). Persisted so
     *  whichever mode was used last becomes that user's default. */
    val searchTitlesOnly: Boolean = false,
    /** Id of the note most recently opened in the editor. Only consulted when
     *  [reopenLastNote] is on; stale ids (deleted/trashed notes) are ignored at
     *  launch rather than eagerly cleared. */
    val lastOpenedNoteId: String? = null,
    /** Open existing notes in preview mode instead of the edit surface, so
     *  browsing note to note keeps showing the rendered view without toggling
     *  the eye icon each time. Off by default (notes open in edit). Toggled from
     *  Settings or by long-pressing the editor's view-toggle icon; a note with
     *  no content yet (a just-created note) still opens in edit so you can start
     *  typing (#200). */
    val openNotesInPreview: Boolean = false
)

/** Notes-list sort orders offered by the top-bar sort menu (#191). */
enum class NotesSortMode {
    /** Most recently edited first — the original behaviour and the only mode
     *  that keeps the Today/Yesterday date sections. */
    UPDATED_DESC,
    UPDATED_ASC,
    TITLE_ASC,
    TITLE_DESC
}

/**
 * File extension Markleaf writes mirrored notes with. The body is always
 * Markdown text with our YAML frontmatter regardless of extension — only the
 * suffix differs — and the importer reads both, so flipping this never orphans
 * already-mirrored files.
 */
enum class SyncFileExtension(val value: String) {
    MD("md"),
    TXT("txt")
}

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

/**
 * Typeface for the editor and preview writing surface. Both options use the
 * platform's built-in generic font families, so nothing is bundled with the
 * app — it stays F-Droid-reproducible and works fully offline.
 */
enum class EditorFont {
    /** The system sans-serif (FontFamily.Default) — Markleaf's original look. */
    SANS,

    /** The system serif (Noto Serif on most devices) for an editorial feel. */
    SERIF
}

enum class EditorLineWidth(
    val label: String,
    val maxWidthDp: Int
) {
    NARROW("Narrow", 640),
    COMFORTABLE("Comfortable", 800),
    WIDE("Wide", 960)
}
