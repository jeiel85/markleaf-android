package com.markleaf.notes.feature.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.markleaf.notes.BuildConfig
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.EditorFont
import com.markleaf.notes.data.settings.EditorLineWidth
import com.markleaf.notes.data.settings.MarkdownSyntaxVisibility
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.NoteImporter
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.feature.lock.canUseBiometric
import com.markleaf.notes.util.ExportAllNotes
import com.markleaf.notes.util.HapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacyClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val noteRepository = remember { LocalNoteRepository(AppDatabase.getInstance(context.applicationContext)) }
    val noteImporter = remember { NoteImporter(AppDatabase.getInstance(context.applicationContext)) }
    val exportAllLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            scope.launch {
                val notes = withContext(Dispatchers.IO) { noteRepository.observeNotes().first() }
                    .filter { !it.trashed }
                val count = withContext(Dispatchers.IO) {
                    ExportAllNotes.exportAllNotes(context, folderUri, notes)
                }
                val msg = context.resources.getQuantityString(R.plurals.export_all_done_format, count, count)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val syncFolderLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            // Persist read+write so the URI keeps working after a reboot.
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(folderUri, flags)
            }
            scope.launch {
                settingsRepository.setSyncFolderUri(folderUri.toString())
                // Mirror every existing note immediately so the folder is seeded.
                val notes = withContext(Dispatchers.IO) { noteRepository.observeNotes().first() }
                    .filter { !it.trashed }
                var written = 0
                withContext(Dispatchers.IO) {
                    notes.forEach { note ->
                        if (NoteFolderMirror.writeNote(context, folderUri, note, appSettings.syncFileExtension)) written++
                    }
                }
                settingsRepository.setSyncLastSyncedAt(System.currentTimeMillis())
                val msg = context.resources.getQuantityString(R.plurals.sync_seeded_format, written, written)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    SettingsSection(title = stringResource(R.string.settings_appearance)) {
                        Text(
                            text = stringResource(R.string.theme_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorPalette.entries.forEach { palette ->
                                val selected = appSettings.colorPalette == palette
                                if (selected) {
                                    Button(onClick = {}) {
                                        Text(palette.localizedLabel())
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch { settingsRepository.setColorPalette(palette) }
                                        }
                                    ) {
                                        Text(palette.localizedLabel())
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.theme_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SettingsSection(title = stringResource(R.string.settings_markdown)) {
                        SettingsSwitchRow(
                            title = stringResource(R.string.show_markdown_syntax),
                            description = stringResource(R.string.show_markdown_syntax_description),
                            checked = appSettings.markdownSyntaxVisibility == MarkdownSyntaxVisibility.SHOW,
                            onCheckedChange = { checked ->
                                HapticFeedback.light(context)
                                scope.launch {
                                    settingsRepository.setMarkdownSyntaxVisibility(
                                        if (checked) MarkdownSyntaxVisibility.SHOW else MarkdownSyntaxVisibility.HIDE
                                    )
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.line_width),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorLineWidth.entries.forEach { lineWidth ->
                                val selected = appSettings.lineWidth == lineWidth
                                if (selected) {
                                    Button(onClick = {}) {
                                        Text(lineWidth.localizedLabel())
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                settingsRepository.setLineWidth(lineWidth)
                                            }
                                        }
                                    ) {
                                        Text(lineWidth.localizedLabel())
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.font_label),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            EditorFont.entries.forEach { font ->
                                val selected = appSettings.editorFont == font
                                if (selected) {
                                    Button(onClick = {}) {
                                        Text(font.localizedLabel())
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                settingsRepository.setEditorFont(font)
                                            }
                                        }
                                    ) {
                                        Text(font.localizedLabel())
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.font_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    SettingsSection(title = stringResource(R.string.settings_privacy)) {
                        SettingLine(stringResource(R.string.privacy_no_tracking))
                        SettingLine(stringResource(R.string.privacy_no_internet))
                        SettingLine(stringResource(R.string.privacy_local_first))
                        Spacer(Modifier.height(12.dp))
                        SettingsSwitchRow(
                            title = stringResource(R.string.screenshot_protection),
                            description = stringResource(R.string.screenshot_protection_description),
                            checked = appSettings.screenshotProtection,
                            onCheckedChange = { checked ->
                                HapticFeedback.light(context)
                                scope.launch {
                                    settingsRepository.setScreenshotProtection(checked)
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        val biometricAvailable = remember(context) { context.canUseBiometric() }
                        SettingsSwitchRow(
                            title = stringResource(R.string.biometric_lock_setting),
                            description = if (biometricAvailable) {
                                stringResource(R.string.biometric_lock_description)
                            } else {
                                stringResource(R.string.biometric_lock_unavailable)
                            },
                            checked = appSettings.biometricLockEnabled && biometricAvailable,
                            onCheckedChange = { checked ->
                                if (!biometricAvailable && checked) return@SettingsSwitchRow
                                HapticFeedback.light(context)
                                scope.launch {
                                    settingsRepository.setBiometricLockEnabled(checked)
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        LockedNotesPasscodeSetting(
                            passcodeSet = appSettings.lockPasscodeSet,
                            onSetPasscode = { passcode ->
                                scope.launch {
                                    settingsRepository.setLockPasscode(passcode)
                                    Toast.makeText(
                                        context,
                                        R.string.locked_passcode_set_done,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            onRemovePasscode = {
                                scope.launch {
                                    settingsRepository.clearLockPasscode()
                                    withContext(Dispatchers.IO) { noteRepository.unlockAllLocked() }
                                    Toast.makeText(
                                        context,
                                        R.string.locked_passcode_removed_done,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onPrivacyClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.privacy_dashboard_button))
                        }
                    }

                    SettingsSection(title = stringResource(R.string.settings_data)) {
                        Text(
                            text = stringResource(R.string.export_all_notes_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { exportAllLauncher.launch(null) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.export_all_notes))
                        }
                    }

                    SyncSection(
                        folderUri = appSettings.syncFolderUri,
                        lastSyncedAt = appSettings.syncLastSyncedAt,
                        onPickFolder = { syncFolderLauncher.launch(null) },
                        onSyncNow = {
                            val uri = appSettings.syncFolderUriOrNull() ?: return@SyncSection
                            scope.launch {
                                val notes = withContext(Dispatchers.IO) {
                                    // Full set (incl. trashed/archived) so a hidden note
                                    // isn't re-imported as new — see #148.
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
                        onStopSync = {
                            scope.launch {
                                settingsRepository.setSyncFolderUri(null)
                                Toast.makeText(
                                    context,
                                    R.string.sync_stopped,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onSyncCenterClick = onSyncCenterClick
                    )

                    SettingsSection(title = stringResource(R.string.settings_open_source)) {
                        Text(
                            text = stringResource(R.string.oss_explainer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        SettingLine(
                            stringResource(R.string.oss_license_label) + ": " +
                                stringResource(R.string.oss_license_value)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(context.getString(R.string.oss_source_url))
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.oss_view_source))
                            }
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(
                                            Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse(context.getString(R.string.oss_fdroid_url))
                                            )
                                        )
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.oss_view_fdroid))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(context.getString(R.string.oss_license_url))
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.oss_view_license))
                        }
                    }

                    SettingsSection(title = stringResource(R.string.settings_app)) {
                        SettingLine(stringResource(R.string.version_format, BuildConfig.VERSION_NAME))
                        SettingLine(stringResource(R.string.application_id_format, BuildConfig.APPLICATION_ID))
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncSection(
    folderUri: String?,
    lastSyncedAt: Long?,
    onPickFolder: () -> Unit,
    onSyncNow: () -> Unit,
    onStopSync: () -> Unit,
    onSyncCenterClick: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.sync_title)) {
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
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (lastSyncedAt != null) {
                    stringResource(R.string.sync_status_last_synced_format, formatRelative(lastSyncedAt))
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
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onSyncCenterClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.sync_center_title))
        }
    }
}

/** "content://com.android.externalstorage.documents/tree/primary%3ADropbox%2FMarkleaf"
 *  becomes a friendlier "Dropbox/Markleaf" preview for the Settings status row. */
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

/**
 * The "Locked notes" passcode control (#155): set, change, or remove the passcode
 * that gates the Locked space. This is a UI-visibility gate — the description says
 * so plainly rather than implying encryption at rest.
 */
@Composable
private fun LockedNotesPasscodeSetting(
    passcodeSet: Boolean,
    onSetPasscode: (String) -> Unit,
    onRemovePasscode: () -> Unit
) {
    var showSetDialog by remember { mutableStateOf(false) }
    var showRemoveConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.locked_passcode_setting_title),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.locked_passcode_setting_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (passcodeSet) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showSetDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.locked_passcode_change))
                }
                OutlinedButton(
                    onClick = { showRemoveConfirm = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.locked_passcode_remove))
                }
            }
        } else {
            OutlinedButton(
                onClick = { showSetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.locked_passcode_set))
            }
        }
    }

    if (showSetDialog) {
        SetPasscodeDialog(
            onDismiss = { showSetDialog = false },
            onConfirm = { passcode ->
                showSetDialog = false
                onSetPasscode(passcode)
            }
        )
    }

    if (showRemoveConfirm) {
        AlertDialog(
            onDismissRequest = { showRemoveConfirm = false },
            title = { Text(stringResource(R.string.locked_passcode_remove_confirm_title)) },
            text = { Text(stringResource(R.string.locked_passcode_remove_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showRemoveConfirm = false
                    onRemovePasscode()
                }) {
                    Text(stringResource(R.string.locked_passcode_remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun SetPasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newPasscode by remember { mutableStateOf("") }
    var confirmPasscode by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.locked_passcode_dialog_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = newPasscode,
                    onValueChange = { newPasscode = it; error = null },
                    label = { Text(stringResource(R.string.locked_passcode_new_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPasscode,
                    onValueChange = { confirmPasscode = it; error = null },
                    label = { Text(stringResource(R.string.locked_passcode_confirm_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = error != null,
                    supportingText = error?.let { { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    newPasscode.length < MIN_PASSCODE_LENGTH ->
                        error = R.string.locked_passcode_too_short
                    newPasscode != confirmPasscode ->
                        error = R.string.locked_passcode_mismatch
                    else -> onConfirm(newPasscode)
                }
            }) {
                Text(stringResource(R.string.locked_passcode_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private const val MIN_PASSCODE_LENGTH = 4

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String?,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(Modifier.height(8.dp))
        Column(content = content)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
    }
}

@Composable
private fun SettingLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}

@Composable
private fun EditorLineWidth.localizedLabel(): String {
    return when (this) {
        EditorLineWidth.NARROW -> stringResource(R.string.line_width_narrow)
        EditorLineWidth.COMFORTABLE -> stringResource(R.string.line_width_comfortable)
        EditorLineWidth.WIDE -> stringResource(R.string.line_width_wide)
    }
}

@Composable
private fun ColorPalette.localizedLabel(): String {
    return when (this) {
        ColorPalette.MARKLEAF_GREEN -> stringResource(R.string.theme_markleaf_green)
        ColorPalette.MATERIAL_YOU -> stringResource(R.string.theme_material_you)
    }
}

@Composable
private fun EditorFont.localizedLabel(): String {
    return when (this) {
        EditorFont.SANS -> stringResource(R.string.font_sans)
        EditorFont.SERIF -> stringResource(R.string.font_serif)
    }
}
