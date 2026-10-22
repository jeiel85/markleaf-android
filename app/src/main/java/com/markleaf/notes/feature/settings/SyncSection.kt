package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.SyncMetadataMode
import com.markleaf.notes.ui.component.elapsedTimeLabel

/**
 * The sync group on the Sync & export page: the multi-device folder mirror.
 *
 * `internal` rather than private so a snapshot test can render it on its own:
 * it is the one settings page whose appearance depends on state the screen
 * cannot be put into from the outside (a chosen folder, a metadata mode) (#255).
 */
@Composable
internal fun SyncSection(
    folderUri: String?,
    lastSyncedAt: Long?,
    metadataMode: SyncMetadataMode,
    metadataBusy: Boolean,
    onMetadataModeChange: (SyncMetadataMode) -> Unit,
    onPickFolder: () -> Unit,
    onSyncNow: () -> Unit,
    onStopSync: () -> Unit,
    onSyncCenterClick: () -> Unit,
    modifier: Modifier = Modifier,
    itemAnchor: (item: SettingsItem, focusable: Boolean) -> Modifier = { _, _ -> Modifier }
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sync_explainer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.sync_recommended_locations),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(14.dp))

        if (folderUri.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.sync_status_unset),
                modifier = itemAnchor(SettingsItem.SYNC_FOLDER, true),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onPickFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sync_pick_folder))
            }
        } else {
            val displayPath = remember(folderUri) { humanReadableTreePath(folderUri) }
            Text(
                text = stringResource(R.string.sync_status_folder_format, displayPath),
                modifier = itemAnchor(SettingsItem.SYNC_FOLDER, true),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (lastSyncedAt != null) {
                    stringResource(R.string.sync_status_last_synced_format, elapsedTimeLabel(lastSyncedAt))
                } else {
                    stringResource(R.string.sync_status_last_synced_never)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.sync_behavior_summary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onSyncNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.sync_now))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onPickFolder,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.sync_change_folder))
                }
                OutlinedButton(
                    onClick = onStopSync,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.sync_stop))
                }
            }
        }
        // Rendered whether or not a folder is linked. It used to appear only
        // after linking, which put the choice one step *past* the moment it is
        // free: the first link is what writes headers into the user's files, and
        // the dialog that offers to stop it told them to change a setting that
        // was not on the screen yet (#372 review).
        run {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.sync_metadata_mode),
                modifier = itemAnchor(SettingsItem.SYNC_METADATA, true),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SyncMetadataMode.entries.forEach { mode ->
                    val selected = metadataMode == mode
                    if (selected) {
                        Button(onClick = {}, enabled = !metadataBusy) {
                            Text(mode.localizedLabel())
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onMetadataModeChange(mode) },
                            enabled = !metadataBusy
                        ) {
                            Text(mode.localizedLabel())
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.sync_metadata_mode_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (metadataMode == SyncMetadataMode.SIDECAR) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.sync_metadata_sidecar_costs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSyncCenterClick,
            modifier = itemAnchor(SettingsItem.SYNC_CENTER, false).fillMaxWidth()
        ) {
            Text(stringResource(R.string.sync_center_title))
        }
    }
}

@Composable
private fun SyncMetadataMode.localizedLabel(): String {
    return when (this) {
        SyncMetadataMode.FRONTMATTER -> stringResource(R.string.sync_metadata_mode_frontmatter)
        SyncMetadataMode.SIDECAR -> stringResource(R.string.sync_metadata_mode_sidecar)
    }
}

/** "content://com.android.externalstorage.documents/tree/primary%3ADropbox%2FMarkleaf"
 *  becomes a friendlier "Dropbox/Markleaf" preview for the Settings status row. */
internal fun humanReadableTreePath(uriString: String): String {
    val decoded = runCatching {
        java.net.URLDecoder.decode(uriString, "UTF-8")
    }.getOrDefault(uriString)
    val afterTree = decoded.substringAfterLast("/tree/", decoded)
    val afterColon = afterTree.substringAfter(":", afterTree)
    return afterColon.ifBlank { afterTree }
}
