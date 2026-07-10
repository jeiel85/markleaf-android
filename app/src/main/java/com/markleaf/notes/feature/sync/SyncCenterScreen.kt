package com.markleaf.notes.feature.sync

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.SyncFileExtension
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.NoteImporter
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.viewmodel.SyncCenterViewModel
import com.markleaf.notes.util.HapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncCenterScreen(
    viewModel: SyncCenterViewModel,
    onBack: () -> Unit,
    onNoteClick: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val noteRepository = remember { LocalNoteRepository(AppDatabase.getInstance(context.applicationContext)) }
    val noteImporter = remember { NoteImporter(AppDatabase.getInstance(context.applicationContext)) }
    
    val conflictNotes by viewModel.conflictNotes.collectAsState()
    
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isSyncing by remember { mutableStateOf(false) }

    val syncFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(folderUri, flags)
            }
            scope.launch {
                settingsRepository.setSyncFolderUri(folderUri.toString())
                val notes = withContext(Dispatchers.IO) { noteRepository.observeNotes().first() }
                    .filter { !it.trashed }
                var written = 0
                withContext(Dispatchers.IO) {
                    notes.forEach { note ->
                        if (NoteFolderMirror.writeNote(context, folderUri, note, appSettings.syncFileExtension)) written++
                    }
                }
                settingsRepository.setSyncLastSyncedAt(System.currentTimeMillis())
                val msg = context.getString(R.string.sync_seeded_format, written)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sync_center_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Folder Mirror Sync settings details card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (appSettings.syncFolderUri.isNullOrBlank()) Icons.Default.SyncDisabled else Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = if (appSettings.syncFolderUri.isNullOrBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.sync_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = stringResource(R.string.sync_explainer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (appSettings.syncFolderUri.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.sync_status_unset),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Button(
                                    onClick = { syncFolderLauncher.launch(null) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.sync_pick_folder))
                                }
                            } else {
                                val displayPath = remember(appSettings.syncFolderUri) {
                                    humanReadableTreePath(appSettings.syncFolderUri!!)
                                }
                                Text(
                                    text = stringResource(R.string.sync_status_folder_format, displayPath),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = if (appSettings.syncLastSyncedAt != null) {
                                        stringResource(R.string.sync_status_last_synced_format, formatRelative(appSettings.syncLastSyncedAt!!))
                                    } else {
                                        stringResource(R.string.sync_status_last_synced_never)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Spacer(Modifier.height(4.dp))
                                
                                Button(
                                    onClick = {
                                        val uriString = appSettings.syncFolderUri ?: return@Button
                                        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@Button
                                        isSyncing = true
                                        scope.launch {
                                            val notes = withContext(Dispatchers.IO) {
                                                // Full set (incl. trashed/archived) so a hidden
                                                // note isn't re-imported as new — see #148.
                                                noteRepository.getAllNotes()
                                            }
                                            val result = withContext(Dispatchers.IO) {
                                                NoteFolderMirror.importChanges(
                                                    context = context,
                                                    folderUri = uri,
                                                    existing = notes,
                                                    applyUpdate = { updated ->
                                                        noteImporter.update(updated)
                                                    },
                                                    applyCreate = { created ->
                                                        noteImporter.create(created)
                                                    }
                                                )
                                            }
                                            settingsRepository.setSyncLastSyncedAt(System.currentTimeMillis())
                                            isSyncing = false
                                            val msg = if (result.conflicts > 0) {
                                                context.getString(
                                                    R.string.sync_done_with_conflicts_format,
                                                    result.updated,
                                                    result.created,
                                                    result.conflicts,
                                                    result.skipped
                                                )
                                            } else {
                                                context.getString(
                                                    R.string.sync_done_format,
                                                    result.updated,
                                                    result.created,
                                                    result.skipped
                                                )
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isSyncing
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (isSyncing) "Syncing..." else stringResource(R.string.sync_now))
                                }
                                
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.sync_file_format),
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SyncFileExtension.values().forEach { ext ->
                                        FilterChip(
                                            selected = appSettings.syncFileExtension == ext,
                                            onClick = {
                                                scope.launch { settingsRepository.setSyncFileExtension(ext) }
                                            },
                                            label = { Text("." + ext.value) }
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        val uriString = appSettings.syncFolderUri ?: return@OutlinedButton
                                        val uri = runCatching { Uri.parse(uriString) }.getOrNull()
                                            ?: return@OutlinedButton
                                        scope.launch {
                                            val notes = withContext(Dispatchers.IO) {
                                                noteRepository.observeNotes().first()
                                            }.filter { !it.trashed }
                                            val renamed = withContext(Dispatchers.IO) {
                                                var n = 0
                                                notes.forEach {
                                                    if (NoteFolderMirror.renameToTitle(context, uri, it)) n++
                                                }
                                                n
                                            }
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.sync_tidy_done_format, renamed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.sync_tidy_filenames))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { syncFolderLauncher.launch(null) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.sync_change_folder))
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                settingsRepository.setSyncFolderUri(null)
                                                Toast.makeText(
                                                    context,
                                                    R.string.sync_stopped,
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text(stringResource(R.string.sync_stop))
                                    }
                                }
                            }
                        }
                    }
                }

                // Conflict Center Section Title
                item {
                    Text(
                        text = stringResource(R.string.conflict_center_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.conflict_center_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Conflict Center List
                if (conflictNotes.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.conflict_center_empty),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = stringResource(R.string.conflict_center_empty_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(conflictNotes, key = { it.id }) { note ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(note.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.title.ifBlank { stringResource(R.string.untitled_parenthesized) },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            HapticFeedback.light(context)
                                            noteToDelete = note
                                            showDeleteConfirm = true
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = stringResource(R.string.delete),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                
                                if (note.excerpt.isNotBlank()) {
                                    Text(
                                        text = note.excerpt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Text(
                                    text = "Updated: ${formatRelative(note.updatedAt.toEpochMilli())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.conflict_delete_confirm_title)) },
            text = { Text(stringResource(R.string.conflict_delete_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        val note = noteToDelete
                        showDeleteConfirm = false
                        if (note != null) {
                            viewModel.deleteConflictNote(note.id)
                            scope.launch(Dispatchers.IO) {
                                com.markleaf.notes.util.AttachmentManager.deleteAllForNote(context, note.id)
                            }
                            appSettings.syncFolderUri?.let { uriString ->
                                val uri = runCatching { Uri.parse(uriString) }.getOrNull()
                                if (uri != null) {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            NoteFolderMirror.deleteNote(context, uri, note.id)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

private fun humanReadableTreePath(uriString: String): String {
    val decoded = runCatching {
        java.net.URLDecoder.decode(uriString, "UTF-8")
    }.getOrDefault(uriString)
    val afterTree = decoded.substringAfterLast("/tree/", decoded)
    val afterColon = afterTree.substringAfter(":", afterTree)
    return afterColon.ifBlank { afterTree }
}

private fun formatRelative(epochMillis: Long): String {
    val deltaMs = System.currentTimeMillis() - epochMillis
    return when {
        deltaMs < 60_000 -> "방금 전"
        deltaMs < 3_600_000 -> "${deltaMs / 60_000}분 전"
        deltaMs < 86_400_000 -> "${deltaMs / 3_600_000}시간 전"
        else -> "${deltaMs / 86_400_000}일 전"
    }
}
