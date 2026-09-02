package com.markleaf.notes.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.EditorFont
import com.markleaf.notes.data.settings.EditorFontSize
import com.markleaf.notes.data.settings.ThemeMode
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.theme.MarkleafTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState

/**
 * Placement screen for [SingleNoteWidget] (#351): choose the note the widget
 * shows and the size its text is drawn at.
 *
 * Reached when the widget is dropped on the home screen, and again from the
 * launcher's own reconfigure action on Android 12+, where the current choices
 * arrive preselected.
 *
 * The list comes from `observeNotes`, which filters `locked = 0` along with
 * trashed and archived notes — so the passcode-gated space is never offered to
 * something drawn on an unlocked home screen. [SingleNoteWidget] guards the
 * render path as well, for a note locked after it was chosen here.
 */
class SingleNoteWidgetConfigureActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Backing out without choosing must leave no widget behind, so the
        // cancelled result stands until a note is actually picked.
        setResult(Activity.RESULT_CANCELED)

        val appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val database = AppDatabase.getInstance(applicationContext)
        val noteRepository = LocalNoteRepository(database)
        val settingsRepository = AppSettingsRepository(applicationContext)
        val initialTextSize = SingleNoteWidgetStore.textSize(applicationContext, appWidgetId)

        setContent {
            val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
            val systemDark = isSystemInDarkTheme()
            MarkleafTheme(
                darkTheme = when (appSettings.themeMode) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
                dynamicColor = appSettings.colorPalette == ColorPalette.MATERIAL_YOU,
                useSerif = appSettings.editorFont == EditorFont.SERIF
            ) {
                val notes by remember { noteRepository.observeNotes() }
                    .collectAsState(initial = emptyList())
                var textSize by rememberSaveable { mutableStateOf(initialTextSize) }
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )

                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        TopAppBar(
                            title = { Text(stringResource(R.string.single_note_widget_configure_title)) },
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { padding ->
                    Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                        TextSizeRow(
                            selected = textSize,
                            onSelect = { textSize = it }
                        )
                        HorizontalDivider()
                        if (notes.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_notes_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(24.dp)
                            )
                        } else {
                            NoteList(
                                notes = notes,
                                onPick = { note -> complete(appWidgetId, note.id, textSize) }
                            )
                        }
                    }
                }
            }
        }
    }

    /** Stores the choice, paints the widget with it, and hands the id back. */
    private fun complete(appWidgetId: Int, noteId: String, textSize: EditorFontSize) {
        SingleNoteWidgetStore.save(applicationContext, appWidgetId, noteId, textSize)
        SingleNoteWidget.updateAppWidget(
            applicationContext,
            AppWidgetManager.getInstance(applicationContext),
            appWidgetId
        )
        setResult(
            Activity.RESULT_OK,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        )
        finish()
    }
}

@Composable
private fun TextSizeRow(
    selected: EditorFontSize,
    onSelect: (EditorFontSize) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = stringResource(R.string.font_size_label),
            style = MaterialTheme.typography.titleSmall
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EditorFontSize.entries.forEach { size ->
                FilterChip(
                    selected = size == selected,
                    onClick = { onSelect(size) },
                    label = { Text(size.localizedLabel()) }
                )
            }
        }
        Text(
            text = stringResource(R.string.single_note_widget_size_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteList(
    notes: List<Note>,
    onPick: (Note) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(notes, key = { it.id }) { note ->
            ListItem(
                headlineContent = {
                    Text(note.title.ifBlank { stringResource(R.string.untitled_parenthesized) })
                },
                supportingContent = {
                    if (note.excerpt.isNotBlank()) {
                        Text(note.excerpt, maxLines = 1)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPick(note) }
            )
        }
    }
}

@Composable
private fun EditorFontSize.localizedLabel(): String = when (this) {
    EditorFontSize.SMALL -> stringResource(R.string.font_size_small)
    EditorFontSize.MEDIUM -> stringResource(R.string.font_size_medium)
    EditorFontSize.LARGE -> stringResource(R.string.font_size_large)
    EditorFontSize.EXTRA_LARGE -> stringResource(R.string.font_size_extra_large)
}
