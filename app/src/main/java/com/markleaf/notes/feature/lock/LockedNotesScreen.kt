package com.markleaf.notes.feature.lock

import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.LockPasscodeAttempt
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.ui.component.EmptyState
import com.markleaf.notes.ui.viewmodel.LockedNotesViewModel
import androidx.compose.runtime.collectAsState
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The passcode-gated "Locked notes" space (#155).
 *
 * Security posture — this is a *UI-visibility* gate, matching [BiometricLockGate].
 * The note bodies are the same Room rows as any other note; the passcode only
 * decides whether this screen reveals them. It is not encryption at rest. The
 * unlocked state is [rememberSaveable] so it survives a config change but resets
 * when the destination leaves the back stack or the app restarts — re-entry
 * always asks for the passcode again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockedNotesScreen(
    viewModel: LockedNotesViewModel,
    onBack: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    var unlocked by rememberSaveable { mutableStateOf(false) }

    // Locked notes are private by definition, so this screen raises FLAG_SECURE
    // regardless of the global screenshot-protection setting — otherwise their
    // titles and bodies leak through screenshots and the Recents thumbnail
    // (#156). On exit the flag returns to whatever the setting asks for.
    val screenshotProtection = appSettings.screenshotProtection
    DisposableEffect(screenshotProtection) {
        val window = context.findFragmentActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            if (!screenshotProtection) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.locked_notes_title)) },
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
            when {
                !appSettings.lockPasscodeSet -> NoPasscodeState(onOpenSettings = onOpenSettings)
                !unlocked -> UnlockGate(
                    attempt = { settingsRepository.attemptLockPasscode(it) },
                    retryAtMillis = { settingsRepository.lockPasscodeRetryAtMillis() },
                    onUnlocked = { unlocked = true }
                )
                else -> LockedNotesList(
                    viewModel = viewModel,
                    onNoteClick = onNoteClick
                )
            }
        }
    }
}

@Composable
private fun NoPasscodeState(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.height(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.locked_no_passcode_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.locked_no_passcode_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onOpenSettings) {
            Text(stringResource(R.string.locked_open_settings))
        }
    }
}

/**
 * Remaining backoff as `m:ss`, rounded up so a fresh 30s wait never reads "0:29".
 * Digits follow the device locale.
 */
private fun formatLockoutWait(millis: Long): String {
    val totalSeconds = ((millis + 999L) / 1_000L).toInt()
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}

@Composable
private fun UnlockGate(
    attempt: suspend (String) -> LockPasscodeAttempt,
    retryAtMillis: suspend () -> Long,
    onUnlocked: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var passcode by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }
    // Millis remaining on the persisted brute-force backoff, 0 when attempts are
    // allowed. Ticked down once a second so the gate re-enables itself (#156).
    var lockoutRemaining by remember { mutableStateOf(0L) }

    fun applyRetryAt(retryAt: Long) {
        lockoutRemaining = (retryAt - System.currentTimeMillis()).coerceAtLeast(0L)
    }

    // A backoff started in an earlier session is still in force on re-entry.
    LaunchedEffect(Unit) { applyRetryAt(retryAtMillis()) }

    LaunchedEffect(lockoutRemaining > 0L) {
        while (lockoutRemaining > 0L) {
            delay(1_000L)
            lockoutRemaining = (lockoutRemaining - 1_000L).coerceAtLeast(0L)
        }
    }

    fun submit() {
        if (checking || passcode.isEmpty() || lockoutRemaining > 0L) return
        checking = true
        scope.launch {
            when (val result = attempt(passcode)) {
                is LockPasscodeAttempt.Success -> {
                    checking = false
                    onUnlocked()
                }
                is LockPasscodeAttempt.Wrong -> {
                    checking = false
                    error = true
                    passcode = ""
                    applyRetryAt(result.retryAtMillis)
                }
                is LockPasscodeAttempt.LockedOut -> {
                    checking = false
                    passcode = ""
                    applyRetryAt(result.retryAtMillis)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.locked_unlock_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = passcode,
            onValueChange = {
                passcode = it
                error = false
            },
            label = { Text(stringResource(R.string.locked_passcode_label)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            enabled = lockoutRemaining == 0L,
            isError = error || lockoutRemaining > 0L,
            supportingText = when {
                lockoutRemaining > 0L -> {
                    {
                        Text(
                            stringResource(
                                R.string.locked_too_many_attempts,
                                formatLockoutWait(lockoutRemaining)
                            )
                        )
                    }
                }
                error -> {
                    { Text(stringResource(R.string.locked_wrong_passcode)) }
                }
                else -> null
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { submit() },
            enabled = passcode.isNotEmpty() && !checking && lockoutRemaining == 0L,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.locked_unlock_button))
        }
    }
}

@Composable
private fun LockedNotesList(
    viewModel: LockedNotesViewModel,
    onNoteClick: (String) -> Unit
) {
    val lockedNotes by viewModel.lockedNotes.collectAsState()
    val haptics = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())

    if (lockedNotes.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.locked_notes_empty),
            hint = stringResource(R.string.locked_notes_empty_hint)
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(lockedNotes, key = { it.id }) { note ->
                LockedNoteRow(
                    note = note,
                    onClick = { onNoteClick(note.id) },
                    onRemoveLock = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.removeLock(note.id)
                        // Locking deleted the note's mirror file, so unlocking has to
                        // put it back — otherwise the note would stay missing from the
                        // sync folder until its next edit (#156).
                        appSettings.syncFolderUri?.let { uriString ->
                            val uri = runCatching {
                                android.net.Uri.parse(uriString)
                            }.getOrNull()
                            if (uri != null) {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        NoteFolderMirror.writeNote(
                                            context,
                                            uri,
                                            note.copy(locked = false)
                                        )
                                    }
                                }
                            }
                        }
                    },
                    onMoveToTrash = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.moveToTrash(note.id)
                    },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LockedNoteRow(
    note: Note,
    onClick: () -> Unit,
    onRemoveLock: () -> Unit,
    onMoveToTrash: () -> Unit,
    onLongPress: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(Color.Transparent)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        onLongPress()
                        menuExpanded = true
                    }
                )
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = note.title.ifEmpty { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (note.excerpt.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = note.excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Filled.LockOpen, contentDescription = null) },
                text = { Text(stringResource(R.string.remove_from_locked)) },
                onClick = {
                    menuExpanded = false
                    onRemoveLock()
                }
            )
            DropdownMenuItem(
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                text = { Text(stringResource(R.string.move_to_trash)) },
                onClick = {
                    menuExpanded = false
                    onMoveToTrash()
                }
            )
        }
    }
}
