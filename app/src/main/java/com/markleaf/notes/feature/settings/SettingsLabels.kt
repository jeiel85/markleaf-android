package com.markleaf.notes.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.markleaf.notes.R
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.EditorFont
import com.markleaf.notes.data.settings.EditorFontSize
import com.markleaf.notes.data.settings.EditorLineSpacing
import com.markleaf.notes.data.settings.EditorLineWidth
import com.markleaf.notes.data.settings.NotesLayout
import com.markleaf.notes.data.settings.NewNoteShortcut
import com.markleaf.notes.data.settings.OpenNotesAt
import com.markleaf.notes.data.settings.ThemeMode
import com.markleaf.notes.data.settings.WidgetOpacity
import java.text.NumberFormat

@Composable
internal fun EditorLineWidth.localizedLabel(): String {
    return when (this) {
        EditorLineWidth.NARROW -> stringResource(R.string.line_width_narrow)
        EditorLineWidth.COMFORTABLE -> stringResource(R.string.line_width_comfortable)
        EditorLineWidth.WIDE -> stringResource(R.string.line_width_wide)
    }
}

@Composable
internal fun NewNoteShortcut.localizedLabel(): String {
    return when (this) {
        NewNoteShortcut.NEW_NOTE -> stringResource(R.string.new_note_shortcut_new)
        NewNoteShortcut.TODAYS_NOTE -> stringResource(R.string.new_note_shortcut_today)
    }
}

@Composable
internal fun OpenNotesAt.localizedLabel(): String {
    return when (this) {
        OpenNotesAt.TOP -> stringResource(R.string.open_notes_at_top)
        OpenNotesAt.BOTTOM -> stringResource(R.string.open_notes_at_bottom)
        OpenNotesAt.LAST_POSITION -> stringResource(R.string.open_notes_at_last_position)
    }
}

@Composable
internal fun NotesLayout.localizedLabel(): String {
    return when (this) {
        NotesLayout.LIST -> stringResource(R.string.notes_layout_list)
        NotesLayout.GRID -> stringResource(R.string.notes_layout_grid)
    }
}

@Composable
internal fun NoteTitleSource.localizedLabel(): String {
    return when (this) {
        NoteTitleSource.FIRST_HEADING -> stringResource(R.string.note_title_source_first_heading)
        NoteTitleSource.FIRST_LINE -> stringResource(R.string.note_title_source_first_line)
    }
}

@Composable
internal fun ColorPalette.localizedLabel(): String {
    return when (this) {
        ColorPalette.MARKLEAF_GREEN -> stringResource(R.string.theme_markleaf_green)
        ColorPalette.MATERIAL_YOU -> stringResource(R.string.theme_material_you)
    }
}

/**
 * "75%" in the viewer's own number format (#469).
 *
 * Input: the step's [WidgetOpacity.percent].
 * Output: the percentage as the app's current locale writes it.
 *
 * Why NumberFormat instead of a translated string: the only localised part is
 * how a percentage is written — `75 %` in French and German, `75%` in English —
 * and the platform already knows that for every locale, including any added
 * later. The locale comes from the composition so it follows the per-app
 * language (#470) rather than whatever `Locale.getDefault()` last held.
 */
@Composable
internal fun WidgetOpacity.localizedLabel(): String {
    val locale = LocalConfiguration.current.locales[0]
    return NumberFormat.getPercentInstance(locale).format(percent / 100.0)
}

@Composable
internal fun ThemeMode.localizedLabel(): String {
    return when (this) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
        ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
    }
}

@Composable
internal fun EditorFontSize.localizedLabel(): String {
    return when (this) {
        EditorFontSize.SMALL -> stringResource(R.string.font_size_small)
        EditorFontSize.MEDIUM -> stringResource(R.string.font_size_medium)
        EditorFontSize.LARGE -> stringResource(R.string.font_size_large)
        EditorFontSize.EXTRA_LARGE -> stringResource(R.string.font_size_extra_large)
    }
}

@Composable
internal fun EditorLineSpacing.localizedLabel(): String {
    return when (this) {
        EditorLineSpacing.COMPACT -> stringResource(R.string.line_spacing_compact)
        EditorLineSpacing.NORMAL -> stringResource(R.string.line_spacing_normal)
        EditorLineSpacing.RELAXED -> stringResource(R.string.line_spacing_relaxed)
    }
}

@Composable
internal fun EditorFont.localizedLabel(): String {
    return when (this) {
        EditorFont.SANS -> stringResource(R.string.font_sans)
        EditorFont.SERIF -> stringResource(R.string.font_serif)
        EditorFont.MONOSPACE -> stringResource(R.string.font_monospace)
        EditorFont.CUSTOM -> stringResource(R.string.font_custom)
    }
}
