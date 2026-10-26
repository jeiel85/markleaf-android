package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.NotesLayout
import com.markleaf.notes.data.settings.NewNoteShortcut
import com.markleaf.notes.data.settings.OpenNotesAt
import com.markleaf.notes.util.HapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope

/**
 * The Notes page: the list, how notes open, and how new notes start.
 *
 * The title rule is changed through [onNoteTitleSourceChange] because the
 * retitle pass it starts is shared with the screen's resume effect (#262):
 * both read [retitleBusy], so it lives with the screen, not the page.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun NotesSettings(
    appSettings: AppSettings,
    settingsRepository: AppSettingsRepository,
    scope: CoroutineScope,
    db: AppDatabase,
    retitleBusy: Boolean,
    onNoteTitleSourceChange: (NoteTitleSource) -> Unit,
    anchors: SectionAnchors
) {
    val context = LocalContext.current
    SettingsGroupHeading(stringResource(R.string.settings_group_list), first = true)
    SettingsSwitchRow(
        title = stringResource(R.string.show_note_previews),
        titleModifier = anchors.item(SettingsItem.NOTE_PREVIEWS),
        description = stringResource(R.string.show_note_previews_description),
        checked = appSettings.notesShowPreview,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setNotesShowPreview(checked)
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.notes_layout),
        modifier = anchors.item(SettingsItem.NOTES_LAYOUT),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NotesLayout.entries.forEach { layout ->
            val selected = appSettings.notesLayout == layout
            if (selected) {
                Button(onClick = {}) {
                    Text(layout.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            settingsRepository.setNotesLayout(layout)
                        }
                    }
                ) {
                    Text(layout.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.notes_layout_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.note_title_source),
        modifier = anchors.item(SettingsItem.NOTE_TITLE_SOURCE),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NoteTitleSource.entries.forEach { source ->
            val selected = appSettings.noteTitleSource == source
            if (selected) {
                Button(onClick = {}, enabled = !retitleBusy) {
                    Text(source.localizedLabel())
                }
            } else {
                OutlinedButton(
                    enabled = !retitleBusy,
                    onClick = {
                        onNoteTitleSourceChange(source)
                    }
                ) {
                    Text(source.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.note_title_source_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    OpenListOnSetting(
        tag = appSettings.openListOnTag,
        db = db,
        onTagChange = { tag -> scope.launch { settingsRepository.setOpenListOnTag(tag) } },
        titleModifier = anchors.item(SettingsItem.OPEN_LIST_ON)
    )
    Spacer(Modifier.height(12.dp))
    SettingsGroupHeading(stringResource(R.string.settings_group_opening), first = false)
    SettingsSwitchRow(
        title = stringResource(R.string.reopen_last_note),
        titleModifier = anchors.item(SettingsItem.REOPEN_LAST_NOTE),
        description = stringResource(R.string.reopen_last_note_description),
        checked = appSettings.reopenLastNote,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setReopenLastNote(checked)
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    SettingsSwitchRow(
        title = stringResource(R.string.open_notes_in_preview),
        titleModifier = anchors.item(SettingsItem.OPEN_IN_PREVIEW),
        description = stringResource(R.string.open_notes_in_preview_description),
        checked = appSettings.openNotesInPreview,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setOpenNotesInPreview(checked)
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.open_notes_at),
        modifier = anchors.item(SettingsItem.OPEN_NOTES_AT),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OpenNotesAt.entries.forEach { where ->
            val selected = appSettings.openNotesAt == where
            if (selected) {
                Button(onClick = {}) {
                    Text(where.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            settingsRepository.setOpenNotesAt(where)
                            // Switching away from "where I left off"
                            // orphans every recorded position — the
                            // setting no longer reads them, but the
                            // rows would sit there forever, and
                            // turning the setting back on later would
                            // restore stale positions for notes that
                            // moved in the meantime (#262).
                            if (where != OpenNotesAt.LAST_POSITION) {
                                withContext(Dispatchers.IO) {
                                    db.noteViewStateDao().clearAll()
                                }
                            }
                        }
                    }
                ) {
                    Text(where.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.open_notes_at_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    SettingsGroupHeading(stringResource(R.string.settings_group_new_notes), first = false)
    // #481: what the widget's +, the "New note" app shortcut
    // and Android's Notes shortcuts open. Wraps: "Today's note"
    // runs long in several languages.
    Text(
        text = stringResource(R.string.new_note_shortcut),
        modifier = anchors.item(SettingsItem.NEW_NOTE_SHORTCUT),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NewNoteShortcut.entries.forEach { target ->
            if (appSettings.newNoteShortcut == target) {
                Button(onClick = {}) {
                    Text(target.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch { settingsRepository.setNewNoteShortcut(target) }
                    }
                ) {
                    Text(target.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.new_note_shortcut_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    SettingsSwitchRow(
        title = stringResource(R.string.new_note_heading),
        titleModifier = anchors.item(SettingsItem.NEW_NOTE_HEADING),
        description = stringResource(R.string.new_note_heading_description),
        checked = appSettings.newNotesStartWithHeading,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setNewNotesStartWithHeading(checked)
            }
        }
    )
}
