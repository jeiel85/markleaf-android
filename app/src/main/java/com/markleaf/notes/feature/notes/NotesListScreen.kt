package com.markleaf.notes.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardCommandKey
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.NotesLayout
import com.markleaf.notes.data.settings.NotesSortMode
import com.markleaf.notes.update.UpdateSurface
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.navigation.NoteSource
import com.markleaf.notes.navigation.fabSharedBounds
import com.markleaf.notes.navigation.noteSharedBounds
import com.markleaf.notes.ui.component.NotesSortMenuButton
import com.markleaf.notes.ui.component.SelectionMark
import com.markleaf.notes.ui.component.SelectionTopAppBar
import com.markleaf.notes.ui.component.rememberNoteSelection
import com.markleaf.notes.ui.component.formatUpdatedTime
import com.markleaf.notes.ui.component.pressScale
import com.markleaf.notes.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesViewModel,
    onNoteClick: (String?) -> Unit,
    onFabClick: () -> Unit,
    onSearchClick: () -> Unit = {},
    onTagsClick: () -> Unit = {},
    onArchiveClick: () -> Unit = {},
    onLockedClick: () -> Unit = {},
    onTrashClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onOpenFileClick: () -> Unit = {},
    onConflictCopiesClick: () -> Unit = {},
    lockPasscodeSet: Boolean = false,
    onRequestSetPasscode: () -> Unit = {},
    onCollapseClick: (() -> Unit)? = null,
    onShowTagRail: (() -> Unit)? = null,
    selectedNoteId: String? = null,
    selectedTag: String? = null,
    // Clears [selectedTag] from a chip under the app bar. Passed where nothing
    // else shows the filter: on phones, which have no tag rail, and on a tablet
    // whose rail is hidden. A list opened on a tag by "Open the list on" (#545)
    // would otherwise be filtered with no sign of it and no way back.
    onClearTag: (() -> Unit)? = null,
    // Injectable like EditorScreen's (#158): the production DataStore is one
    // instance per JVM, so a test that needs the grid layout passes its own.
    settingsRepository: AppSettingsRepository? = null,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = MaterialTheme.colorScheme.onBackground
) {
    val hapticFeedback = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = settingsRepository ?: remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val notesState = remember { mutableStateOf<List<Note>>(emptyList()) }
    val displayedState = remember { mutableStateOf<List<Note>>(emptyList()) }
    var overflowExpanded by remember { mutableStateOf(false) }
    var showQuickSwitcher by remember { mutableStateOf(false) }
    // Shown when the user taps "Move to Locked" but hasn't set a passcode yet —
    // locking a note is only meaningful once there's a passcode to gate it (#155).
    var showSetPasscodePrompt by remember { mutableStateOf(false) }
    val conflictCopyCount by viewModel.conflictCopyCount.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.notes.collect { noteList ->
            notesState.value = noteList
        }
    }
    LaunchedEffect(Unit) {
        viewModel.displayedNotes.collect { noteList ->
            displayedState.value = noteList
        }
    }
    // The quick switcher and the "create your first note" state consider every
    // note; the list itself shows the tag-filtered subset (which equals the full
    // list on phones, where there is no tag rail to set a filter).
    val notes = notesState.value
    val displayed = displayedState.value
    val listState = rememberLazyListState()
    val gridState = rememberLazyGridState()
    // A new tag filter starts at the top. Left alone, the list keeps the row it
    // had at the top (its key is stable), so clearing #journal with "Older" at
    // the top left the pinned notes above it, off screen (#545). Saved, so
    // coming back from a note — which recomposes this from scratch on a phone —
    // is not a change and keeps where the reader had scrolled.
    var scrolledForTag by rememberSaveable { mutableStateOf(selectedTag) }
    LaunchedEffect(selectedTag) {
        if (selectedTag == scrolledForTag) return@LaunchedEffect
        scrolledForTag = selectedTag
        // Apart, because a layout that is not on screen waits for its first
        // layout before it scrolls: in sequence, an unshown list held the
        // grid's reset back (#555 review).
        launch { listState.scrollToItem(0) }
        launch { gridState.scrollToItem(0) }
    }
    val sortMode = appSettings.notesSortMode
    val sections = remember(displayed, sortMode) { groupNotesForDisplay(displayed, sortMode) }

    // Several notes at once (#596). Started from a note's long-press menu or
    // from the overflow menu; while it is up a tap picks a note instead of
    // opening it.
    val selection = rememberNoteSelection()
    LaunchedEffect(displayed) {
        // Not while the list is still loading: a selection restored after a
        // rotation would be emptied before its notes arrive.
        if (displayed.isNotEmpty()) selection.retain(displayed.mapTo(HashSet()) { it.id })
    }
    val picked = remember(displayed, selection.ids) { selection.of(displayed) }
    BackHandler(enabled = selection.active) { selection.clear() }
    var selectionMenuExpanded by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val undoLabel = stringResource(R.string.undo)

    // Both layouts, the long-press menu and the selection bar drive these
    // (#279, #596), so none of them can drift apart - in particular locking,
    // which has to clean up the mirror file as well.
    fun trashNotes(noteIds: List<String>) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.moveToTrash(noteIds)
        val message = context.resources.getQuantityString(
            R.plurals.notes_moved_to_trash, noteIds.size, noteIds.size
        )
        scope.launch {
            // A second move replaces the first one's Undo rather than queueing
            // behind it, where it would only show once the first had timed out.
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreFromTrash(noteIds)
        }
    }

    /** Moves [notesToLock] into the Locked space; false when there is no passcode yet. */
    fun lockNotes(notesToLock: List<Note>): Boolean {
        if (!lockPasscodeSet) {
            showSetPasscodePrompt = true
            return false
        }
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.setLocked(notesToLock.map { it.id }, true)
        // The export guard only stops *future* saves, so a note mirrored
        // before it was locked would leave a readable plaintext copy in the
        // sync folder. Remove it now, or the Locked space's privacy promise is
        // only skin deep. LockedNotesScreen re-mirrors on unlock (#156).
        appSettings.syncFolderUriOrNull()?.let { uri ->
            val metadata = appSettings.mirrorMetadata()
            scope.launch {
                withContext(Dispatchers.IO) {
                    notesToLock.forEach { NoteFolderMirror.deleteNote(context, uri, it.id, metadata) }
                }
            }
        }
        return true
    }

    if (showQuickSwitcher) {
        QuickSwitcherDialog(
            notes = notes,
            onSelect = { id ->
                showQuickSwitcher = false
                onNoteClick(id)
            },
            onDismiss = { showQuickSwitcher = false }
        )
    }

    if (showSetPasscodePrompt) {
        AlertDialog(
            onDismissRequest = { showSetPasscodePrompt = false },
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            title = { Text(stringResource(R.string.locked_need_passcode_title)) },
            text = { Text(stringResource(R.string.locked_need_passcode_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showSetPasscodePrompt = false
                    onRequestSetPasscode()
                }) {
                    Text(stringResource(R.string.locked_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPasscodePrompt = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.onPreviewKeyEvent { event ->
            // Ctrl+K (or Cmd+K on physical Mac/iPad keyboards) opens the
            // quick switcher. No-op when no hardware keyboard is attached —
            // touch users reach the same dialog via the ⋮ overflow menu.
            if (event.type == KeyEventType.KeyDown &&
                event.key == Key.K &&
                (event.isCtrlPressed || event.isMetaPressed)
            ) {
                showQuickSwitcher = true
                true
            } else {
                false
            }
        },
        containerColor = containerColor,
        contentColor = contentColor,
        topBar = {
            Column {
                if (selection.active) {
                    val pickedIds = picked.map { it.id }
                    // Pin when any picked note is unpinned, so a mixed
                    // selection ends up all pinned; Unpin only when all are.
                    val pinAll = picked.any { !it.pinned }
                    SelectionTopAppBar(
                        count = picked.size,
                        onClose = { selection.clear() },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = containerColor,
                            titleContentColor = contentColor,
                            actionIconContentColor = contentColor,
                            navigationIconContentColor = contentColor
                        ),
                        actions = {
                            IconButton(
                                enabled = picked.isNotEmpty(),
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.setPinned(pickedIds, pinAll)
                                    selection.clear()
                                }
                            ) {
                                Icon(
                                    imageVector = if (pinAll) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = stringResource(if (pinAll) R.string.pin else R.string.unpin)
                                )
                            }
                            IconButton(
                                enabled = picked.isNotEmpty(),
                                onClick = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setArchived(pickedIds, true)
                                    selection.clear()
                                }
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = stringResource(R.string.archive_action))
                            }
                            IconButton(
                                enabled = picked.isNotEmpty(),
                                onClick = {
                                    trashNotes(pickedIds)
                                    selection.clear()
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.move_to_trash))
                            }
                            Box {
                                IconButton(onClick = { selectionMenuExpanded = true }) {
                                    Icon(
                                        Icons.Default.MoreVert,
                                        contentDescription = stringResource(R.string.more_options)
                                    )
                                }
                                DropdownMenu(
                                    expanded = selectionMenuExpanded,
                                    onDismissRequest = { selectionMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                        text = { Text(stringResource(R.string.select_all)) },
                                        enabled = picked.size < displayed.size,
                                        onClick = {
                                            selectionMenuExpanded = false
                                            selection.selectAll(displayed.map { it.id })
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                        text = { Text(stringResource(R.string.move_to_locked)) },
                                        enabled = picked.isNotEmpty(),
                                        onClick = {
                                            selectionMenuExpanded = false
                                            if (lockNotes(picked)) selection.clear()
                                        }
                                    )
                                }
                            }
                        }
                    )
                } else TopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.notes_title),
                            style = MaterialTheme.typography.headlineMedium
                        )
                    },
                    navigationIcon = {
                        // Re-show the tag rail after it was hidden (tablet only — the
                        // host passes this just when the rail is collapsed). Paired with
                        // the rail's own "<" hide button.
                        if (onShowTagRail != null) {
                            IconButton(onClick = onShowTagRail) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(R.string.show_tags)
                                )
                            }
                        }
                    },
                    actions = {
                        if (onCollapseClick != null) {
                            IconButton(onClick = onCollapseClick) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.collapse_note_list))
                            }
                        }
                        NotesSortMenuButton(
                            sortMode = sortMode,
                            onSortModeChange = { mode ->
                                scope.launch { settingsRepository.setNotesSortMode(mode) }
                            }
                        )
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        }
                        IconButton(onClick = onTagsClick) {
                            Icon(Icons.AutoMirrored.Filled.Label, contentDescription = stringResource(R.string.tags))
                        }
                        Box {
                            IconButton(onClick = { overflowExpanded = true }) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = stringResource(R.string.more_options)
                                )
                            }
                            DropdownMenu(
                                expanded = overflowExpanded,
                                onDismissRequest = { overflowExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.KeyboardCommandKey, contentDescription = null) },
                                    text = { Text(stringResource(R.string.quick_switcher_title)) },
                                    onClick = {
                                        overflowExpanded = false
                                        showQuickSwitcher = true
                                    }
                                )
                                if (displayed.isNotEmpty()) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
                                        text = { Text(stringResource(R.string.select_notes)) },
                                        onClick = {
                                            overflowExpanded = false
                                            selection.start()
                                        }
                                    )
                                }
                                // Reading a file that is not a note (#326). It sits
                                // above the note destinations because it is the one
                                // entry here that leaves the note library alone.
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) },
                                    text = { Text(stringResource(R.string.open_file)) },
                                    onClick = {
                                        overflowExpanded = false
                                        onOpenFileClick()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Inventory2, contentDescription = null) },
                                    text = { Text(stringResource(R.string.archive)) },
                                    onClick = {
                                        overflowExpanded = false
                                        onArchiveClick()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                                    text = { Text(stringResource(R.string.locked_notes_title)) },
                                    onClick = {
                                        overflowExpanded = false
                                        onLockedClick()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                                    text = { Text(stringResource(R.string.trash)) },
                                    onClick = {
                                        overflowExpanded = false
                                        onTrashClick()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    text = { Text(stringResource(R.string.settings)) },
                                    onClick = {
                                        overflowExpanded = false
                                        onSettingsClick()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = containerColor,
                        titleContentColor = contentColor,
                        actionIconContentColor = contentColor,
                        navigationIconContentColor = contentColor
                    )
                )
                // Under the app bar rather than an item in the list: it appears
                // once the conflict count arrives, after the list has laid out,
                // and an item inserted above the first visible row is scrolled
                // out of view by the list keeping that row where it was (#434).
                if (selectedTag != null && onClearTag != null) {
                    SelectedTagChip(
                        tag = selectedTag,
                        onClear = onClearTag,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                ConflictCopiesBanner(
                    count = conflictCopyCount,
                    onClick = onConflictCopiesClick,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
                // 사이드로드 빌드에서 새 버전이 있을 때만 한 줄이 생긴다. 스토어 빌드가 얻는
                // 구현은 아무것도 그리지 않는 스텁이다(D074, docs/AGENT_SPEC.md §15.9).
                // 위 배너와 같은 이유로 목록 밖에 둔다: 확인 결과는 목록이 그려진 뒤에
                // 오고, 그 전까지 높이가 0인 첫 항목은 목록이 화면 밖으로 셈해 버려서
                // 나중에 생긴 배너가 뷰포트 위에 숨었다(#262).
                UpdateSurface.Banner(Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            // Hidden while selecting: a new note mid-selection would open the
            // editor over a bar of actions waiting for the picked notes.
            if (!selection.active) FloatingActionButton(
                modifier = Modifier.fabSharedBounds(),
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFabClick()
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = stringResource(R.string.add_note)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "📝",
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = stringResource(R.string.no_notes_yet),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.create_first_note_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 6.dp),
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onFabClick,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = stringResource(R.string.create_note),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        } else if (displayed.isEmpty() && selectedTag != null) {
            // A tag filter is active but matches nothing — don't show the
            // "create your first note" onboarding, which would be misleading.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_notes_for_tag),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        } else {
            val togglePin: (Note) -> Unit = { note ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                viewModel.setPinned(note.id, !note.pinned)
            }
            val archive: (Note) -> Unit = { note ->
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.setArchived(note.id, true)
            }
            val lock: (Note) -> Unit = { note -> lockNotes(listOf(note)) }
            val moveToTrash: (Note) -> Unit = { note -> trashNotes(listOf(note.id)) }
            val longPress: () -> Unit = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            }

            when (appSettings.notesLayout) {
                NotesLayout.LIST -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    sections.forEach { section ->
                        if (section.titleResId != null) {
                            item(key = "header-${section.titleResId}") {
                                SectionHeader(
                                    stringResource(section.titleResId),
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                        items(section.notes, key = { it.id }) { note ->
                            NoteRow(
                                note = note,
                                selected = note.id == selectedNoteId,
                                checked = if (selection.active) note.id in selection.ids else null,
                                onToggle = { selection.toggle(note.id) },
                                onSelect = { selection.start(note.id) },
                                showPreview = appSettings.notesShowPreview,
                                modifier = Modifier.animateItem(),
                                onClick = { onNoteClick(note.id) },
                                onTogglePin = { togglePin(note) },
                                onArchive = { archive(note) },
                                onLock = { lock(note) },
                                onMoveToTrash = { moveToTrash(note) },
                                onLongPress = longPress
                            )
                        }
                    }
                }

                NotesLayout.GRID -> LazyVerticalGrid(
                    state = gridState,
                    // Adaptive, not a fixed column count: this screen is also the
                    // tablet's narrow list pane beside the editor, where a fixed
                    // two columns would leave nothing readable in either.
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sections.forEach { section ->
                        if (section.titleResId != null) {
                            item(
                                key = "header-${section.titleResId}",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                SectionHeader(
                                    stringResource(section.titleResId),
                                    modifier = Modifier.animateItem(),
                                    // The grid already pads its content by 12dp;
                                    // the list's 24 would double up here.
                                    horizontalPadding = 12.dp
                                )
                            }
                        }
                        items(section.notes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                selected = note.id == selectedNoteId,
                                checked = if (selection.active) note.id in selection.ids else null,
                                onToggle = { selection.toggle(note.id) },
                                onSelect = { selection.start(note.id) },
                                showPreview = appSettings.notesShowPreview,
                                modifier = Modifier.animateItem(),
                                onClick = { onNoteClick(note.id) },
                                onTogglePin = { togglePin(note) },
                                onArchive = { archive(note) },
                                onLock = { lock(note) },
                                onMoveToTrash = { moveToTrash(note) },
                                onLongPress = longPress
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 24.dp
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = modifier
            .padding(start = horizontalPadding, top = 20.dp, end = horizontalPadding, bottom = 6.dp)
            .semantics { heading() }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteRow(
    note: Note,
    selected: Boolean,
    // Null outside a selection; otherwise whether this note is picked (#596).
    checked: Boolean?,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    showPreview: Boolean,
    onClick: (String) -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onLock: () -> Unit,
    onMoveToTrash: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val itemBackground = when {
        checked == true -> MaterialTheme.colorScheme.secondaryContainer
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
        else -> Color.Transparent
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier.noteSharedBounds(note.id)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interactionSource)
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(itemBackground)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = { if (checked != null) onToggle() else onClick(note.id) },
                    onLongClick = {
                        onLongPress()
                        // While selecting, a long press picks like a tap does
                        // rather than opening one note's menu over the bar.
                        if (checked != null) onToggle() else menuExpanded = true
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {
                    if (checked != null) this.selected = checked
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (checked != null) SelectionMark(checked)
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = stringResource(R.string.pinned),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .height(14.dp)
                    )
                }
                Text(
                    text = note.title.ifEmpty { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showPreview) {
                if (note.excerpt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = note.excerpt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatUpdatedTime(LocalContext.current, note.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        NoteContextMenu(
            note = note,
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onSelect = onSelect,
            onTogglePin = onTogglePin,
            onArchive = onArchive,
            onLock = onLock,
            onMoveToTrash = onMoveToTrash
        )
    }
}

/**
 * A note as a grid tile (#279) — the same content and the same long-press menu
 * as [NoteRow], laid out for a column narrower than the screen: the title gets
 * two lines instead of one and the excerpt four instead of two, because a tile
 * has height to spare and width it does not.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    selected: Boolean,
    // Null outside a selection; otherwise whether this note is picked (#596).
    checked: Boolean?,
    onToggle: () -> Unit,
    onSelect: () -> Unit,
    showPreview: Boolean,
    onClick: (String) -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onLock: () -> Unit,
    onMoveToTrash: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val itemBackground = when {
        checked == true -> MaterialTheme.colorScheme.secondaryContainer
        selected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.36f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier.noteSharedBounds(note.id, NoteSource.TILE)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // A title-only tile is otherwise a thin sliver next to a tile
                // with an excerpt; the minimum keeps a row of them even.
                .heightIn(min = if (showPreview) 120.dp else 64.dp)
                .pressScale(interactionSource)
                .clip(MaterialTheme.shapes.medium)
                .background(itemBackground)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = { if (checked != null) onToggle() else onClick(note.id) },
                    onLongClick = {
                        onLongPress()
                        // While selecting, a long press picks like a tap does
                        // rather than opening one note's menu over the bar.
                        if (checked != null) onToggle() else menuExpanded = true
                    }
                )
                .padding(12.dp)
                .semantics(mergeDescendants = true) {
                    if (checked != null) this.selected = checked
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                if (checked != null) SelectionMark(checked, size = 18.dp)
                if (note.pinned) {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = stringResource(R.string.pinned),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 6.dp, top = 2.dp)
                            .height(14.dp)
                    )
                }
                Text(
                    text = note.title.ifEmpty { stringResource(R.string.untitled) },
                    style = MaterialTheme.typography.titleSmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showPreview) {
                if (note.excerpt.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = note.excerpt,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatUpdatedTime(LocalContext.current, note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }

        NoteContextMenu(
            note = note,
            expanded = menuExpanded,
            onDismiss = { menuExpanded = false },
            onSelect = onSelect,
            onTogglePin = onTogglePin,
            onArchive = onArchive,
            onLock = onLock,
            onMoveToTrash = onMoveToTrash
        )
    }
}

/** The long-press menu shared by [NoteRow] and [NoteCard]. */
@Composable
private fun NoteContextMenu(
    note: Note,
    expanded: Boolean,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onLock: () -> Unit,
    onMoveToTrash: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        // First, because it is the way to every other action here for several
        // notes at once (#596).
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null) },
            text = { Text(stringResource(R.string.select)) },
            onClick = {
                onDismiss()
                onSelect()
            }
        )
        DropdownMenuItem(
            leadingIcon = {
                Icon(
                    imageVector = if (note.pinned) Icons.Outlined.PushPin else Icons.Filled.PushPin,
                    contentDescription = null
                )
            },
            text = {
                Text(
                    if (note.pinned) stringResource(R.string.unpin)
                    else stringResource(R.string.pin)
                )
            },
            onClick = {
                onDismiss()
                onTogglePin()
            }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
            text = { Text(stringResource(R.string.archive_action)) },
            onClick = {
                onDismiss()
                onArchive()
            }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            text = { Text(stringResource(R.string.move_to_locked)) },
            onClick = {
                onDismiss()
                onLock()
            }
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
            text = { Text(stringResource(R.string.move_to_trash)) },
            onClick = {
                onDismiss()
                onMoveToTrash()
            }
        )
    }
}

private data class NoteSection(val titleResId: Int?, val notes: List<Note>)

/**
 * Sections for the list under the active sort mode (#191). The default
 * newest-first mode keeps the Today/Yesterday date buckets; every other mode
 * renders a Pinned section (pinning outranks any sort) followed by one flat,
 * header-less section in the chosen order — date buckets would be misleading
 * when the list isn't date-ordered.
 */
private fun groupNotesForDisplay(notes: List<Note>, mode: NotesSortMode): List<NoteSection> {
    if (mode == NotesSortMode.UPDATED_DESC) return groupNotes(notes)
    if (notes.isEmpty()) return emptyList()

    val sorted = sortNotesForDisplay(notes, mode)
    val pinned = sorted.filter { it.pinned }
    val rest = sorted.filter { !it.pinned }
    val sections = mutableListOf<NoteSection>()
    if (pinned.isNotEmpty()) sections += NoteSection(R.string.section_pinned, pinned)
    if (rest.isNotEmpty()) sections += NoteSection(null, rest)
    return sections
}

private fun groupNotes(notes: List<Note>): List<NoteSection> {
    if (notes.isEmpty()) return emptyList()

    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    val pinned = notes.filter { it.pinned }
    val rest = notes.filter { !it.pinned }

    val byBucket = linkedMapOf<Int, MutableList<Note>>()
    for (note in rest) {
        val date = note.updatedAt.atZone(zone).toLocalDate()
        val daysFromToday = ChronoUnit.DAYS.between(date, today)
        val bucket = when {
            daysFromToday <= 0L -> R.string.section_today
            daysFromToday == 1L -> R.string.section_yesterday
            daysFromToday in 2..7 -> R.string.section_past_week
            else -> R.string.section_older
        }
        byBucket.getOrPut(bucket) { mutableListOf() }.add(note)
    }

    val sections = mutableListOf<NoteSection>()
    if (pinned.isNotEmpty()) sections += NoteSection(R.string.section_pinned, pinned)
    val ordered = listOf(
        R.string.section_today,
        R.string.section_yesterday,
        R.string.section_past_week,
        R.string.section_older
    )
    for (bucket in ordered) {
        byBucket[bucket]?.takeIf { it.isNotEmpty() }?.let {
            sections += NoteSection(bucket, it)
        }
    }
    return sections
}

/** The tag the list is filtered to, and the way back to all notes (#545). */
@Composable
private fun SelectedTagChip(tag: String, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val clearLabel = stringResource(R.string.show_all_notes)
    InputChip(
        selected = true,
        onClick = onClear,
        label = { Text("#$tag") },
        trailingIcon = { Icon(Icons.Default.Close, contentDescription = clearLabel) },
        modifier = modifier
    )
}
