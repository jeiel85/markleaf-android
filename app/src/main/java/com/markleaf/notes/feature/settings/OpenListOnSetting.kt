package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.util.TagParser

/**
 * "Open the list on" (#545): all notes, or one tag the list is filtered to
 * each time the app opens. The tags offered are the ones the tag rail and the
 * Tags screen show; [tag] is stored normalized, as the rail selects it.
 */
@Composable
internal fun OpenListOnSetting(
    tag: String?,
    db: AppDatabase,
    onTagChange: (String?) -> Unit,
    titleModifier: Modifier = Modifier
) {
    val tagRepository = remember(db) { LocalTagRepository(db) }
    val tags by tagRepository.observeVisibleTags().collectAsState(initial = emptyList())
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val allNotes = stringResource(R.string.all_notes)

    Text(
        text = stringResource(R.string.open_list_on),
        modifier = titleModifier,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { showPicker = true }) {
        Text(tag?.let { "#$it" } ?: allNotes)
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.open_list_on_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(stringResource(R.string.open_list_on)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    item(key = "all") {
                        TextButton(onClick = {
                            onTagChange(null)
                            showPicker = false
                        }) { Text(allNotes) }
                    }
                    items(tags, key = { it.id }) { option ->
                        TextButton(onClick = {
                            onTagChange(TagParser.normalizeTagName(option.name))
                            showPicker = false
                        }) { Text("#${option.name}") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
