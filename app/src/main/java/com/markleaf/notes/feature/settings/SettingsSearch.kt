package com.markleaf.notes.feature.settings

import androidx.annotation.StringRes
import com.markleaf.notes.R
import java.text.Normalizer
import java.util.Locale

/**
 * The settings Settings search can land on, in page order (#517).
 *
 * Each one names the string resources it is found by — its label, its
 * description, the labels of its options — rather than a separate keyword
 * list: those strings are already translated into every shipped language, so
 * search works in all of them without a second set of strings to keep in step.
 * "Dark" finds Theme because Dark is one of its buttons.
 */
internal enum class SettingsItem(
    val section: SettingsShortcut,
    @StringRes val title: Int,
    @StringRes val description: Int? = null,
    @StringRes val options: List<Int> = emptyList()
) {
    THEME(
        SettingsShortcut.APPEARANCE, R.string.theme_label, R.string.theme_mode_description,
        listOf(R.string.theme_mode_system, R.string.theme_mode_light, R.string.theme_mode_dark)
    ),
    COLOR_PALETTE(
        SettingsShortcut.APPEARANCE, R.string.color_palette_label, R.string.theme_description,
        listOf(R.string.theme_markleaf_green, R.string.theme_material_you)
    ),
    WIDGET_OPACITY(SettingsShortcut.APPEARANCE, R.string.widget_opacity_label, R.string.widget_opacity_description),
    WIDGET_COLOR(
        SettingsShortcut.APPEARANCE, R.string.widget_color_label, R.string.widget_color_description,
        listOf(R.string.widget_color_app, R.string.widget_color_custom)
    ),
    MARKDOWN_SYNTAX(SettingsShortcut.MARKDOWN, R.string.show_markdown_syntax, R.string.show_markdown_syntax_description),
    FORMATTING_BUTTON(SettingsShortcut.MARKDOWN, R.string.show_formatting_button, R.string.show_formatting_button_description),
    LINE_WIDTH(
        SettingsShortcut.MARKDOWN, R.string.line_width, R.string.line_width_description,
        listOf(R.string.line_width_narrow, R.string.line_width_comfortable, R.string.line_width_wide)
    ),
    FONT(
        SettingsShortcut.MARKDOWN, R.string.font_label, R.string.font_description,
        listOf(R.string.font_sans, R.string.font_serif, R.string.font_monospace, R.string.font_custom)
    ),
    FONT_SIZE(
        SettingsShortcut.MARKDOWN, R.string.font_size_label, R.string.font_size_description,
        listOf(R.string.font_size_small, R.string.font_size_medium, R.string.font_size_large, R.string.font_size_extra_large)
    ),
    NOTE_PREVIEWS(SettingsShortcut.NOTES, R.string.show_note_previews, R.string.show_note_previews_description),
    REOPEN_LAST_NOTE(SettingsShortcut.NOTES, R.string.reopen_last_note, R.string.reopen_last_note_description),
    OPEN_IN_PREVIEW(SettingsShortcut.NOTES, R.string.open_notes_in_preview, R.string.open_notes_in_preview_description),
    OPEN_NOTES_AT(
        SettingsShortcut.NOTES, R.string.open_notes_at, R.string.open_notes_at_description,
        listOf(R.string.open_notes_at_top, R.string.open_notes_at_bottom, R.string.open_notes_at_last_position)
    ),
    NEW_NOTE_SHORTCUT(
        SettingsShortcut.NOTES, R.string.new_note_shortcut, R.string.new_note_shortcut_description,
        listOf(R.string.new_note_shortcut_new, R.string.new_note_shortcut_today)
    ),
    NOTES_LAYOUT(
        SettingsShortcut.NOTES, R.string.notes_layout, R.string.notes_layout_description,
        listOf(R.string.notes_layout_list, R.string.notes_layout_grid)
    ),
    NOTE_TITLE_SOURCE(
        SettingsShortcut.NOTES, R.string.note_title_source, R.string.note_title_source_description,
        listOf(R.string.note_title_source_first_heading, R.string.note_title_source_first_line)
    ),
    SCREENSHOT_PROTECTION(SettingsShortcut.PRIVACY, R.string.screenshot_protection, R.string.screenshot_protection_description),
    RECENT_NOTES_IN_SHORTCUTS(
        SettingsShortcut.PRIVACY, R.string.recent_notes_in_shortcuts, R.string.recent_notes_in_shortcuts_description
    ),
    BIOMETRIC_LOCK(SettingsShortcut.PRIVACY, R.string.biometric_lock_setting, R.string.biometric_lock_description),
    LOCKED_PASSCODE(
        SettingsShortcut.PRIVACY, R.string.locked_passcode_setting_title, R.string.locked_passcode_setting_description
    ),
    PRIVACY_DASHBOARD(SettingsShortcut.PRIVACY, R.string.privacy_dashboard_button),
    EXPORT_ALL(SettingsShortcut.DATA, R.string.export_all_notes, R.string.export_all_notes_description),
    EXPORT_TAG(SettingsShortcut.DATA, R.string.export_tag_notes),
    SYNC_FOLDER(SettingsShortcut.SYNC, R.string.sync_pick_folder, R.string.sync_explainer),
    SYNC_METADATA(
        SettingsShortcut.SYNC, R.string.sync_metadata_mode, R.string.sync_metadata_mode_description,
        listOf(R.string.sync_metadata_mode_frontmatter, R.string.sync_metadata_mode_sidecar)
    ),
    SYNC_CENTER(SettingsShortcut.SYNC, R.string.sync_center_title),
    VIEW_SOURCE(SettingsShortcut.OPEN_SOURCE, R.string.oss_view_source),
    VIEW_FDROID(SettingsShortcut.OPEN_SOURCE, R.string.oss_view_fdroid),
    VIEW_LICENSE(SettingsShortcut.OPEN_SOURCE, R.string.oss_view_license)
}

/** One row of search results: a setting, or a whole section when [item] is null. */
internal data class SettingsSearchEntry(
    val section: SettingsShortcut,
    val item: SettingsItem?,
    val title: String,
    val texts: List<String>
)

internal object SettingsSearch {
    /**
     * Every entry search can return, with its strings resolved.
     *
     * Input: [text], which turns a string resource into the current language's
     * string; [sectionTitle], the section heading as the page shows it.
     * Output: one entry per section, then one per [SettingsItem], each in page
     * order.
     *
     * Why a lambda instead of a Context: the catalogue is plain data, so the
     * unit test can build it from fake strings and check the matching without
     * a running Android.
     */
    fun entries(
        text: (Int) -> String,
        sectionTitle: (SettingsShortcut) -> String
    ): List<SettingsSearchEntry> {
        val sections = SettingsShortcut.entries.map { section ->
            val heading = sectionTitle(section)
            // The chip's short word ("Sync") finds the section too.
            SettingsSearchEntry(section, null, heading, listOf(heading, text(section.label)))
        }
        val items = SettingsItem.entries.map { item ->
            val title = text(item.title)
            SettingsSearchEntry(
                section = item.section,
                item = item,
                title = title,
                texts = listOf(title) + listOfNotNull(item.description?.let(text)) + item.options.map(text)
            )
        }
        return sections + items
    }

    /**
     * The entries [query] finds.
     *
     * Input: the text typed into the search field.
     * Output: nothing for a blank query; otherwise each entry whose strings
     * hold every word of the query, those whose title holds them first, page
     * order kept within each group.
     *
     * Why every word, in any of the strings: a reader types what they remember
     * ("dark theme", "font size"), and the words are often split between the
     * label and the description. Why titles first: a word that only turns up
     * in some other setting's description is a weaker answer than the setting
     * named after it.
     */
    fun search(query: String, entries: List<SettingsSearchEntry>): List<SettingsSearchEntry> {
        val words = normalize(query).split(WHITESPACE).filter { it.isNotEmpty() }
        if (words.isEmpty()) return emptyList()
        val matches = entries.filter { entry ->
            val haystack = entry.texts.joinToString("\n") { normalize(it) }
            words.all { it in haystack }
        }
        val (inTitle, elsewhere) = matches.partition { entry ->
            val title = normalize(entry.title)
            words.all { it in title }
        }
        return inTitle + elsewhere
    }

    /**
     * Lower case, with Latin accents dropped, so "theme" finds "Thème" and
     * "tieng" finds "tiếng".
     *
     * Why only U+0300–U+036F rather than every combining mark: decomposing
     * also splits Japanese kana from their voicing marks, and dropping those
     * would let か find が — different words, not the same word unaccented.
     * Hangul decomposes into jamo, which are letters and stay, so a syllable
     * still being composed on the keyboard already matches.
     */
    fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(LATIN_ACCENTS, "")
            .lowercase(Locale.ROOT)

    private val LATIN_ACCENTS = Regex("[\\u0300-\\u036F]")
    private val WHITESPACE = Regex("\\s+")
}
