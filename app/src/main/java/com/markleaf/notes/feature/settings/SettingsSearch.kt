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
    val section: SettingsCategory,
    @StringRes val title: Int,
    @StringRes val description: Int? = null,
    @StringRes val options: List<Int> = emptyList()
) {
    THEME(
        SettingsCategory.APPEARANCE, R.string.theme_label, R.string.theme_mode_description,
        listOf(R.string.theme_mode_system, R.string.theme_mode_light, R.string.theme_mode_dark)
    ),
    COLOR_PALETTE(
        SettingsCategory.APPEARANCE, R.string.color_palette_label, R.string.theme_description,
        listOf(R.string.theme_markleaf_green, R.string.theme_material_you)
    ),
    WIDGET_OPACITY(SettingsCategory.APPEARANCE, R.string.widget_opacity_label, R.string.widget_opacity_description),
    WIDGET_COLOR(
        SettingsCategory.APPEARANCE, R.string.widget_color_label, R.string.widget_color_description,
        listOf(R.string.widget_color_app, R.string.widget_color_custom)
    ),
    FONT(
        SettingsCategory.EDITOR, R.string.font_label, R.string.font_description,
        listOf(R.string.font_sans, R.string.font_serif, R.string.font_monospace, R.string.font_custom)
    ),
    FONT_SIZE(
        SettingsCategory.EDITOR, R.string.font_size_label, R.string.font_size_description,
        listOf(R.string.font_size_small, R.string.font_size_medium, R.string.font_size_large, R.string.font_size_extra_large)
    ),
    LINE_WIDTH(
        SettingsCategory.EDITOR, R.string.line_width, R.string.line_width_description,
        listOf(R.string.line_width_narrow, R.string.line_width_comfortable, R.string.line_width_wide)
    ),
    MARKDOWN_SYNTAX(SettingsCategory.EDITOR, R.string.show_markdown_syntax, R.string.show_markdown_syntax_description),
    FORMATTING_BUTTON(SettingsCategory.EDITOR, R.string.show_formatting_button, R.string.show_formatting_button_description),
    SNIPPET_BUTTONS(SettingsCategory.EDITOR, R.string.snippet_buttons_label, R.string.snippet_buttons_description),
    NOTE_PREVIEWS(SettingsCategory.NOTES, R.string.show_note_previews, R.string.show_note_previews_description),
    NOTES_LAYOUT(
        SettingsCategory.NOTES, R.string.notes_layout, R.string.notes_layout_description,
        listOf(R.string.notes_layout_list, R.string.notes_layout_grid)
    ),
    NOTE_TITLE_SOURCE(
        SettingsCategory.NOTES, R.string.note_title_source, R.string.note_title_source_description,
        listOf(R.string.note_title_source_first_heading, R.string.note_title_source_first_line)
    ),
    REOPEN_LAST_NOTE(SettingsCategory.NOTES, R.string.reopen_last_note, R.string.reopen_last_note_description),
    OPEN_IN_PREVIEW(SettingsCategory.NOTES, R.string.open_notes_in_preview, R.string.open_notes_in_preview_description),
    OPEN_NOTES_AT(
        SettingsCategory.NOTES, R.string.open_notes_at, R.string.open_notes_at_description,
        listOf(R.string.open_notes_at_top, R.string.open_notes_at_bottom, R.string.open_notes_at_last_position)
    ),
    NEW_NOTE_SHORTCUT(
        SettingsCategory.NOTES, R.string.new_note_shortcut, R.string.new_note_shortcut_description,
        listOf(R.string.new_note_shortcut_new, R.string.new_note_shortcut_today)
    ),
    NEW_NOTE_HEADING(SettingsCategory.NOTES, R.string.new_note_heading, R.string.new_note_heading_description),
    BIOMETRIC_LOCK(SettingsCategory.PRIVACY, R.string.biometric_lock_setting, R.string.biometric_lock_description),
    LOCKED_PASSCODE(
        SettingsCategory.PRIVACY, R.string.locked_passcode_setting_title, R.string.locked_passcode_setting_description
    ),
    SCREENSHOT_PROTECTION(SettingsCategory.PRIVACY, R.string.screenshot_protection, R.string.screenshot_protection_description),
    RECENT_NOTES_IN_SHORTCUTS(
        SettingsCategory.PRIVACY, R.string.recent_notes_in_shortcuts, R.string.recent_notes_in_shortcuts_description
    ),
    PRIVACY_DASHBOARD(SettingsCategory.PRIVACY, R.string.privacy_dashboard_button),
    SYNC_FOLDER(SettingsCategory.SYNC, R.string.sync_pick_folder, R.string.sync_explainer),
    SYNC_METADATA(
        SettingsCategory.SYNC, R.string.sync_metadata_mode, R.string.sync_metadata_mode_description,
        listOf(R.string.sync_metadata_mode_frontmatter, R.string.sync_metadata_mode_sidecar)
    ),
    SYNC_CENTER(SettingsCategory.SYNC, R.string.sync_center_title),
    EXPORT_ALL(SettingsCategory.SYNC, R.string.export_all_notes, R.string.export_all_notes_description),
    EXPORT_TAG(SettingsCategory.SYNC, R.string.export_tag_notes),
    // Only on the page in sideload builds (D074); store builds leave it out of
    // the catalogue through `include`.
    UPDATE_CHECK(SettingsCategory.ABOUT, R.string.update_check_title, R.string.update_check_description),
    VIEW_SOURCE(SettingsCategory.ABOUT, R.string.oss_view_source),
    VIEW_FDROID(SettingsCategory.ABOUT, R.string.oss_view_fdroid),
    VIEW_LICENSE(SettingsCategory.ABOUT, R.string.oss_view_license)
}

/** One row of search results: a setting, or a whole section when [item] is null. */
internal data class SettingsSearchEntry(
    val section: SettingsCategory,
    val item: SettingsItem?,
    val title: String,
    val texts: List<String>
)

internal object SettingsSearch {
    /**
     * Every entry search can return, with its strings resolved.
     *
     * Input: [text], which turns a string resource into the current language's
     * string; [include], false for a setting this build doesn't show;
     * [extraTexts], option labels that aren't string resources (the widget
     * opacity percentages, formatted by the locale).
     * Output: one entry per category, found by its name and the group
     * headings on its page, then one per included [SettingsItem], each in
     * page order.
     *
     * Why a lambda instead of a Context: the catalogue is plain data, so the
     * unit test can build it from fake strings and check the matching without
     * a running Android.
     */
    fun entries(
        text: (Int) -> String,
        include: (SettingsItem) -> Boolean = { true },
        extraTexts: (SettingsItem) -> List<String> = { emptyList() }
    ): List<SettingsSearchEntry> {
        val sections = SettingsCategory.entries.map { section ->
            val title = text(section.label)
            SettingsSearchEntry(section, null, title, listOf(title) + section.groups.map(text))
        }
        val items = SettingsItem.entries.filter(include).map { item ->
            val title = text(item.title)
            SettingsSearchEntry(
                section = item.section,
                item = item,
                title = title,
                texts = listOf(title) + listOfNotNull(item.description?.let(text)) +
                    item.options.map(text) + extraTexts(item)
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
     * Input: any string from the page or the search field.
     * Output: the form both sides are compared in.
     *
     * Why decompose, strip, then recompose: only Latin accents are dropped
     * (U+0300–U+036F after a Latin letter). Every other mark is put back on
     * its letter, because left decomposed a bare letter would still match it
     * as a substring — и would find й, か would find が, which are different
     * letters, not the same letter unaccented.
     *
     * Why Hangul is then decomposed again: a syllable still being composed on
     * the keyboard ("서" on the way to "설") should already match, and that
     * only works on jamo.
     */
    fun normalize(text: String): String {
        val stripped = Normalizer.normalize(text, Normalizer.Form.NFD).replace(LATIN_ACCENTS, "")
        val composed = Normalizer.normalize(stripped, Normalizer.Form.NFC).lowercase(Locale.ROOT)
        return buildString {
            composed.forEach { c ->
                if (c in HANGUL_SYLLABLES) append(Normalizer.normalize(c.toString(), Normalizer.Form.NFD))
                else append(c)
            }
        }
    }

    private val HANGUL_SYLLABLES = '\uAC00'..'\uD7A3'
    private val LATIN_ACCENTS = Regex("(?<=\\p{IsLatin})[\\u0300-\\u036F]+")
    private val WHITESPACE = Regex("\\s+")
}
