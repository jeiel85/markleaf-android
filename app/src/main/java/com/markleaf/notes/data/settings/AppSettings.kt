package com.markleaf.notes.data.settings

import com.markleaf.notes.core.text.NoteTitleSource

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
    /** Keep the standing `Aa` formatting button on the row above the keyboard
     *  while editing. Off removes that row for people who format by typing
     *  Markdown, for whom it is chrome that never pays for itself; the actions
     *  that appear *while text is selected* are unaffected, because selecting
     *  is how you ask for them (#331). */
    val showFormattingButton: Boolean = true,
    /** Reopen the last-edited note on a plain launch instead of landing on the
     *  notes list. Opt-in (#192). */
    val reopenLastNote: Boolean = false,
    /** Sort order of the notes list, driven by the top-bar sort menu (#191). */
    val notesSortMode: NotesSortMode = NotesSortMode.UPDATED_DESC,
    /** How the notes list arranges its rows. Defaults to [NotesLayout.LIST],
     *  the one-column list Markleaf has always shown (#279). */
    val notesLayout: NotesLayout = NotesLayout.LIST,
    /** Which line of a note becomes its title. Defaults to
     *  [NoteTitleSource.FIRST_HEADING], the original rule (#280). */
    val noteTitleSource: NoteTitleSource = NoteTitleSource.FIRST_HEADING,
    /** True while a [NoteRetitler] pass for a new title rule is in flight. Set
     *  before the pass starts and cleared when it finishes; a process death
     *  mid-pass leaves it set so the Settings screen can resume the pass rather
     *  than showing titles from two rules (#262). */
    val retitlePending: Boolean = false,
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
    val openNotesInPreview: Boolean = false,
    /** Where a note is positioned when it opens. Defaults to [OpenNotesAt.TOP],
     *  which is how Markleaf has always behaved (#214). */
    val openNotesAt: OpenNotesAt = OpenNotesAt.TOP,
    /** Where the note↔file mapping is kept. Defaults to
     *  [SyncMetadataMode.FRONTMATTER], the original behaviour (#216). */
    val syncMetadataMode: SyncMetadataMode = SyncMetadataMode.FRONTMATTER,
    /** A sidecar folder conversion was started and has not reported finishing (#262). */
    val sidecarMigrationPending: Boolean = false,
    /**
     * This install's id, used to name the sidecar index it owns so two devices
     * writing the same folder never write the same file. Generated on first use
     * and never sent anywhere — it exists only to keep filenames apart.
     */
    val syncDeviceId: String? = null
)

/**
 * Where Markleaf keeps the link between a note and its mirror file (#216).
 *
 * Requested by someone whose notes are edited in another app and synced by
 * Nextcloud, for whom the `---` block at the top of every file is text they did
 * not write sitting in their notes.
 */
enum class SyncMetadataMode {
    /**
     * A `---` header at the top of each file. The original behaviour and the
     * default: the id survives a rename by any tool, and a folder of these
     * files can be reconstructed anywhere without help.
     */
    FRONTMATTER,

    /**
     * A hidden per-device index beside the notes; the `.md` files hold only
     * what was typed. Costs the rename resilience and leaves the folder unable
     * to describe itself without the index — see [SidecarIndex].
     */
    SIDECAR
}

/**
 * Where the editor and preview land when a note opens (#214).
 *
 * Requested by someone keeping long append-style notes, for whom the top is the
 * one part of the note they never want to see. [LAST_POSITION] is the third
 * value they asked to try: append-style notes want the bottom, but reading-style
 * ones usually want wherever you stopped.
 */
enum class OpenNotesAt {
    /** The start of the note — the original behaviour, and the default. */
    TOP,

    /** The end of the note, for notes you only ever add to. */
    BOTTOM,

    /**
     * Wherever this note was left last time. The position lives in the app's
     * own database, never in the note's file, and is only recorded while this
     * value is selected — turning the setting off stops Markleaf remembering.
     */
    LAST_POSITION
}

/**
 * How the notes list arranges its rows (#279).
 *
 * Requested by someone who wanted to see more notes at once, so it is a choice
 * rather than a replacement: [LIST] is what everyone already has, and the
 * "Show note previews" setting keeps applying to both.
 */
enum class NotesLayout {
    /** One note per row, full width — the original behaviour and the default. */
    LIST,

    /** Cards in as many columns as the screen width allows. */
    GRID
}

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
