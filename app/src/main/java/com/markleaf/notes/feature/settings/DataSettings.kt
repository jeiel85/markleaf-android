package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R

/**
 * The Data page: exporting notes to a folder.
 *
 * The folder pickers stay with the screen: their results arrive after the
 * picker closes, and the tag being exported has to survive an activity
 * recreation in between.
 */
@Composable
internal fun DataSettings(
    exportTagsAvailable: Boolean,
    onExportAll: () -> Unit,
    onExportTag: () -> Unit,
    anchors: SectionAnchors
) {
    Text(
        text = stringResource(R.string.export_all_notes_description),
        modifier = anchors.item(SettingsItem.EXPORT_ALL),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = {
            onExportAll()
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(stringResource(R.string.export_all_notes))
    }
    OutlinedButton(
        onClick = onExportTag,
        enabled = exportTagsAvailable,
        modifier = anchors.item(SettingsItem.EXPORT_TAG, focusable = false).fillMaxWidth()
    ) {
        Text(stringResource(R.string.export_tag_notes))
    }
}
