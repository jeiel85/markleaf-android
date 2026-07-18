package com.markleaf.notes.feature.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.MarkdownEditActions
import com.markleaf.notes.core.markdown.MarkdownSyntaxColors
import com.markleaf.notes.core.markdown.MarkdownSyntaxVisualTransformation
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.core.markdown.preview.MarkdownPreviewList
import com.markleaf.notes.core.markdown.preview.extractHeadings
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteLinkRepository
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.MarkdownSyntaxVisibility
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.util.AttachmentManager
import com.markleaf.notes.util.ExportUtil
import com.markleaf.notes.util.HapticFeedback
import com.markleaf.notes.util.ExportPdf
import com.markleaf.notes.util.ShareNoteUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String? = null,
    onBack: () -> Unit,
    onNavigateToNote: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { LocalNoteRepository(db) }
    val tagRepo = remember { LocalTagRepository(db) }
    val linkRepo = remember { LocalNoteLinkRepository(db) }
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val coroutineScope = rememberCoroutineScope()

    var editorState by remember(noteId) { mutableStateOf(TextFieldValue("")) }
    var saveTrigger by remember(noteId) { mutableStateOf(0) }
    var isLoaded by remember(noteId) { mutableStateOf(noteId == null) }
    var shouldRequestEditorFocus by remember(noteId) { mutableStateOf(noteId == null) }
    val editorFocusRequester = remember(noteId) { FocusRequester() }
    var isPreviewMode by remember(noteId) { mutableStateOf(false) }
    var isFocusMode by remember(noteId) { mutableStateOf(false) }
    var isFormattingExpanded by remember(noteId) { mutableStateOf(false) }
    var showDeleteConfirm by remember(noteId) { mutableStateOf(false) }

    val previewListState = rememberLazyListState()
    var showInfo by remember(noteId) { mutableStateOf(false) }
    var pendingPreviewScrollIndex by remember(noteId) { mutableStateOf<Int?>(null) }
    val shouldPreparePreview = isPreviewMode || showInfo
    val previewLines = remember(editorState.text, shouldPreparePreview) {
        if (shouldPreparePreview) SimpleMarkdownPreview.parse(editorState.text) else emptyList()
    }
    val tocHeadings = remember(previewLines) { extractHeadings(previewLines) }
    val currentTitle = remember(editorState.text, noteId) {
        if (noteId == null) "" else TitleExtractor.extractTitle(editorState.text)
    }
    val backlinksFlow = remember(currentTitle, noteId) {
        linkRepo.observeBacklinks(currentTitle, noteId.orEmpty())
    }
    val backlinks by backlinksFlow.collectAsState(initial = emptyList())
    val editorStats = remember(editorState.text) { computeStats(editorState.text) }
    val editorStatsText = stringResource(
        R.string.editor_stats_format,
        editorStats.words,
        editorStats.chars,
        editorStats.readMinutes
    )

    var isFindOpen by remember(noteId) { mutableStateOf(false) }
    var findQuery by remember(noteId) { mutableStateOf("") }
    var findIndex by remember(noteId) { mutableStateOf(0) }
    var replaceQuery by remember(noteId) { mutableStateOf("") }

    // Wikilink autocomplete: when the user has typed `[[query` without
    // closing it on the same line, surface matching note titles.
    val allNotes by repo.observeNotes().collectAsState(initial = emptyList())
    val wikilinkQuery by remember {
        derivedStateOf { detectWikilinkQuery(editorState) }
    }
    val wikilinkSuggestions = remember(wikilinkQuery, allNotes, noteId) {
        val q = wikilinkQuery ?: return@remember emptyList()
        val needle = q.lowercase()
        allNotes
            .filter { it.id != noteId && it.title.isNotBlank() }
            .filter { needle.isEmpty() || it.title.lowercase().contains(needle) }
            .take(MAX_WIKILINK_SUGGESTIONS)
    }

    // Tag autocomplete: when the cursor sits inside an in-progress `#tag`,
    // surface existing tags. Mirrors the wikilink dropdown but keyed off `#`
    // with TagParser's rules (see detectTagQuery) so URL fragments and `##`
    // never trigger it.
    val allTags by tagRepo.observeAllTags().collectAsState(initial = emptyList())
    val tagQuery by remember {
        derivedStateOf { detectTagQuery(editorState) }
    }
    val tagSuggestions = remember(tagQuery, allTags) {
        val q = tagQuery ?: return@remember emptyList()
        val needle = q.lowercase()
        val names = allTags.map { it.name }.distinct()
        val matches = names.filter { needle.isEmpty() || it.contains(needle) }
        // Prefix matches first, then substring matches; never re-suggest the
        // exact tag the user has already finished typing.
        (matches.filter { it.startsWith(needle) } + matches.filterNot { it.startsWith(needle) })
            .filter { it != needle }
            .take(MAX_TAG_SUGGESTIONS)
    }
    val quickInsertQuery by remember {
        derivedStateOf { detectQuickInsertQuery(editorState) }
    }
    val allQuickInsertItems = quickInsertDisplayItems()
    val quickInsertItems = remember(quickInsertQuery, allQuickInsertItems) {
        val query = quickInsertQuery ?: return@remember emptyList()
        val filtered = filterQuickInsertCommands(
            allQuickInsertItems.map { item ->
                QuickInsertSearchItem(item.command, item.label)
            },
            query.text
        )
        filtered.map { searchItem ->
            allQuickInsertItems.first { it.command == searchItem.command }
        }
    }
    var quickInsertSelectedIndex by remember(noteId) { mutableStateOf(0) }
    LaunchedEffect(quickInsertQuery?.text, quickInsertItems.size) {
        quickInsertSelectedIndex = 0
    }
    var overflowExpanded by remember(noteId) { mutableStateOf(false) }
    var pendingExport by remember(noteId) { mutableStateOf<Note?>(null) }
    var imageAltEditing by remember(noteId) { mutableStateOf<Pair<String, String>?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        shouldRequestEditorFocus = true
        val nid = noteId
        if (uri != null && nid != null) {
            coroutineScope.launch {
                val result = withContext(Dispatchers.IO) {
                    AttachmentManager.copyIntoStorage(context, nid, uri)
                }
                if (result != null) {
                    val insertion = "![](${result.relativePath})\n"
                    val cursor = editorState.selection.max
                    val updatedText = editorState.text.substring(0, cursor) +
                        insertion +
                        editorState.text.substring(cursor)
                    editorState = editorState.copy(
                        text = updatedText,
                        selection = TextRange(cursor + insertion.length)
                    )
                    saveTrigger++
                } else {
                    Toast.makeText(context, R.string.attachment_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val exportSingleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        val note = pendingExport
        pendingExport = null
        if (uri != null && note != null) {
            coroutineScope.launch {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        // The note's first line is already its title (Markleaf has
                        // no separate title field), so write the Markdown as-is —
                        // prepending a heading would duplicate the title (#143).
                        os.write(note.contentMarkdown.toByteArray())
                    }
                    Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.export_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val findMatches = remember(editorState.text, findQuery) {
        findAllRanges(editorState.text, findQuery)
    }
    LaunchedEffect(findMatches) {
        if (findIndex >= findMatches.size) findIndex = 0
    }
    LaunchedEffect(findIndex, findMatches) {
        if (findMatches.isNotEmpty()) {
            val range = findMatches[findIndex.coerceIn(findMatches.indices)]
            editorState = editorState.copy(
                selection = TextRange(range.first, range.last + 1)
            )
        }
    }

    LaunchedEffect(noteId) {
        if (noteId == null) {
            isLoaded = true
        } else {
            val content = repo.getNote(noteId)?.contentMarkdown.orEmpty()
            editorState = TextFieldValue(content)
            shouldRequestEditorFocus = content.isEmpty()
            isLoaded = true
        }
    }

    LaunchedEffect(noteId, saveTrigger, isLoaded) {
        if (noteId != null && isLoaded && saveTrigger > 0) {
            delay(1000)
            val currentNote = repo.getNote(noteId)
            if (currentNote != null) {
                val content = editorState.text
                val updatedNote = currentNote.copy(
                    title = TitleExtractor.extractTitle(content),
                    contentMarkdown = content,
                    excerpt = TitleExtractor.generateExcerpt(content),
                    updatedAt = java.time.Instant.now()
                )
                repo.updateNote(updatedNote)
                tagRepo.reindexTagsForNote(noteId, content)
                linkRepo.reindexLinksForNote(noteId, content)
                appSettings.syncFolderUri?.let { uriString ->
                    val uri = runCatching { android.net.Uri.parse(uriString) }.getOrNull()
                    // Never mirror a locked note to the sync folder — the Locked
                    // space is meant to stay on-device, and the mirror writes plain
                    // text (#155). Removing the lock re-includes it on the next save.
                    if (uri != null && !updatedNote.locked) {
                        val ok = withContext(Dispatchers.IO) {
                            val wrote = NoteFolderMirror.writeNote(context, uri, updatedNote, appSettings.syncFileExtension)
                            if (wrote) {
                                val attachments = AttachmentManager.filesForNote(context, noteId)
                                if (attachments.isNotEmpty()) {
                                    NoteFolderMirror.mirrorAttachments(context, uri, noteId, attachments)
                                }
                            }
                            wrote
                        }
                        if (ok) {
                            // Stamp the synced snapshot so the next reconcile
                            // can distinguish "remote echo" from "remote edit
                            // by another device since this snapshot."
                            repo.updateNote(updatedNote.copy(lastImportedAt = updatedNote.updatedAt))
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(shouldRequestEditorFocus, isLoaded, isPreviewMode) {
        if (shouldRequestEditorFocus && isLoaded && !isPreviewMode) {
            withFrameNanos { }
            editorFocusRequester.requestFocus()
            shouldRequestEditorFocus = false
        }
    }

    LaunchedEffect(isPreviewMode, pendingPreviewScrollIndex) {
        val index = pendingPreviewScrollIndex ?: return@LaunchedEffect
        if (isPreviewMode) {
            withFrameNanos { }
            previewListState.animateScrollToItem(index)
            pendingPreviewScrollIndex = null
        }
    }

    val isFormattingPreempted =
        isFindOpen ||
            isFocusMode ||
            isPreviewMode ||
            showInfo ||
            overflowExpanded ||
            showDeleteConfirm ||
            imageAltEditing != null ||
            (quickInsertQuery != null && quickInsertItems.isNotEmpty()) ||
            (wikilinkQuery != null && wikilinkSuggestions.isNotEmpty()) ||
            (tagQuery != null && tagSuggestions.isNotEmpty())
    LaunchedEffect(isFormattingPreempted) {
        if (isFormattingPreempted) isFormattingExpanded = false
    }
    BackHandler(enabled = isFormattingExpanded) {
        isFormattingExpanded = false
        shouldRequestEditorFocus = true
    }

    Scaffold(
        topBar = {
            EditorTopAppBar(
                title = when {
                    isFocusMode -> stringResource(R.string.focus_mode)
                    isPreviewMode -> stringResource(R.string.preview)
                    noteId != null -> stringResource(R.string.edit_note)
                    else -> stringResource(R.string.new_note)
                },
                isPreviewMode = isPreviewMode,
                isFocusMode = isFocusMode,
                showMore = !isPreviewMode || noteId != null,
                moreExpanded = overflowExpanded,
                onBack = onBack,
                onTogglePreview = {
                    val returningToEdit = isPreviewMode
                    isPreviewMode = !isPreviewMode
                    if (returningToEdit) shouldRequestEditorFocus = true
                },
                onExitFocusMode = { isFocusMode = false },
                onOpenInfo = {
                    overflowExpanded = false
                    showInfo = true
                },
                onOpenMore = { overflowExpanded = true },
                onDismissMore = { overflowExpanded = false },
                moreMenuContent = {
                    if (!isPreviewMode) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            text = { Text(stringResource(R.string.find_in_note)) },
                            onClick = {
                                overflowExpanded = false
                                isFindOpen = !isFindOpen
                                if (isFindOpen) {
                                    findQuery = ""
                                    replaceQuery = ""
                                }
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(Icons.Default.CenterFocusStrong, contentDescription = null)
                            },
                            text = { Text(stringResource(R.string.focus_mode)) },
                            onClick = {
                                overflowExpanded = false
                                isFocusMode = true
                            }
                        )
                    }
                    if (noteId != null) {
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            text = { Text(stringResource(R.string.share_via_system)) },
                            onClick = {
                                overflowExpanded = false
                                coroutineScope.launch {
                                    val current = repo.getNote(noteId) ?: return@launch
                                    val live = current.copy(
                                        title = TitleExtractor.extractTitle(editorState.text),
                                        contentMarkdown = editorState.text,
                                        excerpt = TitleExtractor.generateExcerpt(editorState.text)
                                    )
                                    ShareNoteUtil.shareNote(context, live)
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_as_file)) },
                            onClick = {
                                overflowExpanded = false
                                coroutineScope.launch {
                                    val current = repo.getNote(noteId) ?: return@launch
                                    val live = current.copy(
                                        title = TitleExtractor.extractTitle(editorState.text),
                                        contentMarkdown = editorState.text,
                                        excerpt = TitleExtractor.generateExcerpt(editorState.text)
                                    )
                                    pendingExport = live
                                    exportSingleLauncher.launch(ExportUtil.generateFileName(live))
                                }
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_pdf)) },
                            onClick = {
                                overflowExpanded = false
                                coroutineScope.launch {
                                    val current = repo.getNote(noteId) ?: return@launch
                                    val live = current.copy(
                                        title = TitleExtractor.extractTitle(editorState.text),
                                        contentMarkdown = editorState.text,
                                        excerpt = TitleExtractor.generateExcerpt(editorState.text)
                                    )
                                    ExportPdf.export(context, live)
                                }
                            }
                        )
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            text = { Text(stringResource(R.string.move_to_trash)) },
                            onClick = {
                                overflowExpanded = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Crossfade(
            targetState = isPreviewMode,
            label = "Editor preview mode"
        ) { previewMode ->
            if (previewMode) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MarkdownPreviewList(
                        lines = previewLines,
                        modifier = Modifier.weight(1f, fill = false),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        listState = previewListState,
                        onWikilinkClick = { title ->
                            coroutineScope.launch {
                                val existing = db.noteDao().getNoteByTitle(title)
                                val targetId = if (existing != null) {
                                    existing.id
                                } else {
                                    val seed = "# $title\n\n"
                                    val newNote = com.markleaf.notes.domain.model.Note(
                                        id = java.util.UUID.randomUUID().toString(),
                                        title = title,
                                        contentMarkdown = seed,
                                        excerpt = "",
                                        createdAt = java.time.Instant.now(),
                                        updatedAt = java.time.Instant.now()
                                    )
                                    repo.createNote(newNote)
                                    newNote.id
                                }
                                onNavigateToNote(targetId)
                            }
                        },
                        onImageLongPress = { path, currentAlt ->
                            imageAltEditing = path to currentAlt
                        }
                    )
                }
            } else {
                // imePadding() shrinks the editor body when the soft keyboard is up so
                // BasicTextField's built-in cursor bring-into-view can keep the caret
                // above the keyboard. Without it, enableEdgeToEdge() lets the IME draw
                // over the last lines and they stay hidden (#136).
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    if (isFindOpen && !isFocusMode) {
                        FindBar(
                            query = findQuery,
                            onQueryChange = {
                                findQuery = it
                                findIndex = 0
                            },
                            currentIndex = findIndex,
                            totalMatches = findMatches.size,
                            onPrev = {
                                if (findMatches.isNotEmpty()) {
                                    findIndex = (findIndex - 1 + findMatches.size) % findMatches.size
                                }
                            },
                            onNext = {
                                if (findMatches.isNotEmpty()) {
                                    findIndex = (findIndex + 1) % findMatches.size
                                }
                            },
                            onClose = {
                                isFindOpen = false
                                findQuery = ""
                                replaceQuery = ""
                                shouldRequestEditorFocus = true
                            },
                            replaceQuery = replaceQuery,
                            onReplaceQueryChange = { replaceQuery = it },
                            onReplaceOne = {
                                if (findMatches.isNotEmpty()) {
                                    val safeIndex = findIndex.coerceIn(findMatches.indices)
                                    val target = findMatches[safeIndex]
                                    editorState = replaceRange(editorState, target, replaceQuery)
                                    shouldRequestEditorFocus = true
                                    if (isLoaded) saveTrigger++
                                }
                            },
                            onReplaceAll = {
                                if (findMatches.isNotEmpty()) {
                                    val count = findMatches.size
                                    editorState = replaceAllRanges(editorState, findMatches, replaceQuery)
                                    shouldRequestEditorFocus = true
                                    if (isLoaded) saveTrigger++
                                    Toast.makeText(
                                        context,
                                        context.resources.getQuantityString(R.plurals.replace_all_done_format, count, count),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        )
                    }

                    val colorScheme = MaterialTheme.colorScheme
                    val markdownSyntaxVisible =
                        appSettings.markdownSyntaxVisibility == MarkdownSyntaxVisibility.SHOW && !isFocusMode
                    // Stabilise the transformation instance: without `remember` a
                    // fresh MarkdownSyntaxVisualTransformation is allocated on every
                    // recomposition (i.e. every keystroke), which forces Compose to
                    // re-run the syntax filter over the whole document each time.
                    // Re-key only when the colours or visibility actually change.
                    val markdownVisualTransformation = remember(colorScheme, markdownSyntaxVisible) {
                        if (markdownSyntaxVisible) {
                            MarkdownSyntaxVisualTransformation(
                                MarkdownSyntaxColors(
                                    heading = colorScheme.primary,
                                    emphasis = colorScheme.tertiary,
                                    link = colorScheme.primary,
                                    syntax = colorScheme.onSurfaceVariant,
                                    checkbox = colorScheme.secondary,
                                    code = colorScheme.tertiary
                                )
                            )
                        } else {
                            VisualTransformation.None
                        }
                    }
                    val onQuickInsertPick: (QuickInsertCommand) -> Unit = pick@{ command ->
                        val query = detectQuickInsertQuery(editorState) ?: return@pick
                        HapticFeedback.light(context)
                        editorState = applyQuickInsertCommand(editorState, query, command)
                        quickInsertSelectedIndex = 0
                        shouldRequestEditorFocus = true
                        if (isLoaded) saveTrigger++
                        if (command == QuickInsertCommand.IMAGE) {
                            imagePickerLauncher.launch(arrayOf("image/*"))
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .pointerInput(isFormattingExpanded) {
                                if (isFormattingExpanded) {
                                    awaitEachGesture {
                                        awaitFirstDown(pass = PointerEventPass.Initial)
                                        isFormattingExpanded = false
                                    }
                                }
                            },
                        contentAlignment = Alignment.TopStart
                    ) {
                        BasicTextField(
                            value = editorState,
                            onValueChange = { incoming ->
                                isFormattingExpanded = false
                                editorState = MarkdownEditActions.applyAutoContinuation(editorState, incoming)
                                if (isLoaded) saveTrigger++
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(editorFocusRequester)
                                .semantics { contentDescription = context.getString(R.string.note_content) }
                                .onPreviewKeyEvent { event ->
                                    if (event.type != KeyEventType.KeyDown) {
                                        false
                                    } else {
                                        val quickInsertHandled =
                                            !isFocusMode && quickInsertQuery != null && quickInsertItems.isNotEmpty() &&
                                                when (event.key) {
                                                    Key.DirectionDown -> {
                                                        quickInsertSelectedIndex =
                                                            (quickInsertSelectedIndex + 1) % quickInsertItems.size
                                                        true
                                                    }
                                                    Key.DirectionUp -> {
                                                        quickInsertSelectedIndex =
                                                            (quickInsertSelectedIndex - 1 + quickInsertItems.size) %
                                                                quickInsertItems.size
                                                        true
                                                    }
                                                    Key.Enter -> {
                                                        val safeIndex =
                                                            safeQuickInsertIndex(
                                                                quickInsertSelectedIndex,
                                                                quickInsertItems.size
                                                            )
                                                        onQuickInsertPick(quickInsertItems[safeIndex].command)
                                                        true
                                                    }
                                                    else -> false
                                                }
                                        if (quickInsertHandled) {
                                            true
                                        } else if (event.key == Key.Tab) {
                                            editorState = if (event.isShiftPressed) {
                                                MarkdownEditActions.outdent(editorState)
                                            } else {
                                                MarkdownEditActions.indent(editorState)
                                            }
                                            if (isLoaded) saveTrigger++
                                            true
                                        } else if (event.isCtrlPressed || event.isMetaPressed) {
                                            // Hardware-keyboard formatting shortcuts. Accept Ctrl
                                            // (typical Android external keyboards) or Meta/Cmd
                                            // (Mac-style tablet keyboards) so both feel native.
                                            // Bare Ctrl+S is intentionally NOT bound: writers reflex-
                                            // hit it to "save", and since Markleaf auto-saves we must
                                            // not turn that keystroke into a strikethrough that mangles
                                            // text — strikethrough requires the explicit Shift.
                                            val action: ((TextFieldValue) -> TextFieldValue)? = when (event.key) {
                                                Key.B -> MarkdownEditActions::bold
                                                Key.I -> MarkdownEditActions::italic
                                                Key.K -> MarkdownEditActions::markdownLink
                                                Key.S -> if (event.isShiftPressed) MarkdownEditActions::strikethrough else null
                                                else -> null
                                            }
                                            if (action != null) {
                                                editorState = action(editorState)
                                                shouldRequestEditorFocus = true
                                                if (isLoaded) saveTrigger++
                                                true
                                            } else {
                                                false
                                            }
                                        } else {
                                            false
                                        }
                                    }
                                },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            visualTransformation = markdownVisualTransformation,
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxSize()) {
                                    innerTextField()
                                    if (editorState.text.isEmpty()) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize().padding(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.EditNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                modifier = Modifier.size(64.dp)
                                            )
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = stringResource(R.string.editor_empty_title),
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = stringResource(R.string.editor_empty_hint),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(top = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    if (quickInsertQuery != null && quickInsertItems.isNotEmpty() && !isFocusMode) {
                        QuickInsertPanel(
                            items = quickInsertItems,
                            selectedIndex = quickInsertSelectedIndex.coerceIn(quickInsertItems.indices),
                            onPick = onQuickInsertPick
                        )
                    } else if (wikilinkQuery != null && wikilinkSuggestions.isNotEmpty() && !isFocusMode) {
                        WikilinkSuggestionsRow(
                            suggestions = wikilinkSuggestions,
                            onPick = { title ->
                                editorState = completeWikilink(editorState, title)
                                shouldRequestEditorFocus = true
                                if (isLoaded) saveTrigger++
                            }
                        )
                    } else if (tagQuery != null && tagSuggestions.isNotEmpty() && !isFocusMode) {
                        TagSuggestionsRow(
                            suggestions = tagSuggestions,
                            onPick = { tag ->
                                editorState = completeTag(editorState, tag)
                                shouldRequestEditorFocus = true
                                if (isLoaded) saveTrigger++
                            }
                        )
                    }

                    if (!isFormattingPreempted) {
                        EditorFormattingControls(
                            state = EditorFormattingUiState(
                                selectionActive = !editorState.selection.collapsed,
                                expanded = isFormattingExpanded,
                                enabled = isLoaded
                            ),
                            onExpandedChange = { expanded ->
                                isFormattingExpanded = expanded
                                if (!expanded) shouldRequestEditorFocus = true
                            },
                            onAction = { action ->
                                HapticFeedback.light(context)
                                when (val result = action.applyTo(editorState)) {
                                    is EditorFormattingResult.Edited -> {
                                        editorState = result.value
                                        shouldRequestEditorFocus = true
                                        if (isLoaded) saveTrigger++
                                    }
                                    EditorFormattingResult.PickImage -> {
                                        imagePickerLauncher.launch(arrayOf("image/*"))
                                    }
                                }
                            },
                            backgroundColor = MaterialTheme.colorScheme.background
                        )
                    }
                }
            }
        }
    }

    imageAltEditing?.let { (path, currentAlt) ->
        var draft by remember(path) { mutableStateOf(currentAlt) }
        AlertDialog(
            onDismissRequest = { imageAltEditing = null },
            title = { Text(stringResource(R.string.image_alt_dialog_title)) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.image_alt_dialog_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text(stringResource(R.string.image_alt_dialog_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    editorState = replaceImageAlt(editorState, path, draft)
                    if (isLoaded) saveTrigger++
                    imageAltEditing = null
                }) {
                    Text(stringResource(R.string.image_alt_dialog_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { imageAltEditing = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showDeleteConfirm && noteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.move_to_trash_title)) },
            text = { Text(stringResource(R.string.move_to_trash_editor_message)) },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    coroutineScope.launch {
                        repo.moveToTrash(noteId)
                        onBack()
                    }
                }) {
                    Text(stringResource(R.string.move_to_trash))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showInfo) {
        EditorInfoSheet(
            state = EditorInfoUiState(
                statsText = editorStatsText,
                headings = tocHeadings,
                backlinks = backlinks
            ),
            onHeadingClick = { index ->
                showInfo = false
                pendingPreviewScrollIndex = index
                if (!isPreviewMode) isPreviewMode = true
            },
            onBacklinkClick = { id ->
                showInfo = false
                onNavigateToNote(id)
            },
            onDismiss = { showInfo = false }
        )
    }
}
private const val MAX_WIKILINK_SUGGESTIONS = 8
private const val MAX_TAG_SUGGESTIONS = 8

/**
 * If the cursor sits inside an *unclosed* `[[…` wikilink (no `]]` between
 * the opening `[[` and the cursor, no newline either), return the partial
 * query text. Returns null when there's nothing to autocomplete.
 */
internal fun detectWikilinkQuery(value: TextFieldValue): String? {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val before = value.text.substring(0, cursor)
    val openIdx = before.lastIndexOf("[[")
    if (openIdx < 0) return null
    val between = before.substring(openIdx + 2)
    if (between.contains("]]") || between.contains("\n")) return null
    return between
}

/**
 * Replace the open `[[query` segment ending at the cursor with `[[Title]]`
 * and place the cursor just after `]]`. Used when the user picks a wikilink
 * autocomplete suggestion.
 */
internal fun completeWikilink(value: TextFieldValue, title: String): TextFieldValue {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val before = value.text.substring(0, cursor)
    val openIdx = before.lastIndexOf("[[")
    if (openIdx < 0) return value
    val replacement = "[[$title]]"
    val newText = value.text.substring(0, openIdx) + replacement + value.text.substring(cursor)
    val newCursor = openIdx + replacement.length
    return value.copy(text = newText, selection = TextRange(newCursor))
}

// A tag body may contain letters/digits (any script), `_`, `/` (hierarchy), and
// `-`; a tag must *start* with a letter or `_`. These mirror TagParser's regex so
// the autocomplete trigger and the persisted tag index agree on what a tag is.
private fun isTagBodyChar(c: Char): Boolean =
    c.isLetterOrDigit() || c == '_' || c == '/' || c == '-'

private fun isTagStartChar(c: Char): Boolean =
    c.isLetter() || c == '_'

/**
 * If the cursor sits inside an *in-progress* `#tag` (a `#` that starts the
 * content or follows whitespace, with only valid tag characters between it and
 * the cursor), return the partial tag text (possibly empty right after `#`).
 * Returns null otherwise. Mirrors [com.markleaf.notes.util.TagParser]'s rules so
 * URL fragments (`…com#frag`), `##`, and mid-word `a#b` never autocomplete.
 */
internal fun detectTagQuery(value: TextFieldValue): String? {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val before = value.text.substring(0, cursor)
    val hashIdx = before.lastIndexOf('#')
    if (hashIdx < 0) return null
    if (hashIdx > 0 && !before[hashIdx - 1].isWhitespace()) return null
    val query = before.substring(hashIdx + 1)
    // Whitespace or punctuation between the `#` and the cursor means the tag
    // already closed — there is nothing to complete.
    if (query.any { !isTagBodyChar(it) }) return null
    // A tag cannot start with a digit, `/`, or `-`.
    if (query.isNotEmpty() && !isTagStartChar(query[0])) return null
    return query
}

/**
 * Replace the open `#query` segment ending at the cursor with `#tag ` (a
 * trailing space closes the tag so the dropdown dismisses and the writer keeps
 * typing). Used when the user picks a tag autocomplete suggestion.
 */
internal fun completeTag(value: TextFieldValue, tag: String): TextFieldValue {
    val cursor = value.selection.start.coerceIn(0, value.text.length)
    val before = value.text.substring(0, cursor)
    val hashIdx = before.lastIndexOf('#')
    if (hashIdx < 0) return value
    val after = value.text.substring(cursor)
    // Close the tag with a trailing space so the dropdown dismisses, unless the
    // next character is already whitespace (avoids a doubled space mid-line).
    val trailing = if (after.firstOrNull()?.isWhitespace() == true) "" else " "
    val replacement = "#$tag$trailing"
    val newText = value.text.substring(0, hashIdx) + replacement + after
    val newCursor = hashIdx + replacement.length
    return value.copy(text = newText, selection = TextRange(newCursor))
}

/**
 * Rewrite the alt text of `![oldAlt](path)` (or `![](path)`) to use [newAlt]
 * while keeping [path] intact. If the same path appears multiple times in the
 * body, only the first occurrence is updated — rare in practice because every
 * inserted attachment uses a UUID filename.
 */
internal fun replaceImageAlt(value: TextFieldValue, path: String, newAlt: String): TextFieldValue {
    val pattern = Regex("""!\[[^\[\]\n]*]\(${Regex.escape(path)}\)""")
    val match = pattern.find(value.text) ?: return value
    val replacement = "![${newAlt}](${path})"
    val newText = value.text.substring(0, match.range.first) +
        replacement +
        value.text.substring(match.range.last + 1)
    val delta = replacement.length - match.value.length
    val newCursor = value.selection.start + delta
    return value.copy(
        text = newText,
        selection = TextRange(newCursor.coerceIn(0, newText.length))
    )
}

internal fun findAllRanges(text: String, query: String): List<IntRange> {
    if (query.isEmpty() || text.isEmpty()) return emptyList()
    val lower = text.lowercase()
    val q = query.lowercase()
    val ranges = mutableListOf<IntRange>()
    var idx = 0
    while (idx <= lower.length - q.length) {
        val found = lower.indexOf(q, idx)
        if (found < 0) break
        ranges += found until (found + q.length)
        idx = found + q.length.coerceAtLeast(1)
    }
    return ranges
}

internal fun replaceRange(
    state: TextFieldValue,
    range: IntRange,
    replacement: String
): TextFieldValue {
    val text = state.text
    val start = range.first.coerceIn(0, text.length)
    val endExclusive = (range.last + 1).coerceIn(start, text.length)
    val newText = text.substring(0, start) + replacement + text.substring(endExclusive)
    val caret = (start + replacement.length).coerceIn(0, newText.length)
    return TextFieldValue(text = newText, selection = TextRange(caret))
}

internal fun replaceAllRanges(
    state: TextFieldValue,
    ranges: List<IntRange>,
    replacement: String
): TextFieldValue {
    if (ranges.isEmpty()) return state
    val text = state.text
    val sorted = ranges.sortedBy { it.first }
    val builder = StringBuilder()
    var cursor = 0
    for (range in sorted) {
        val start = range.first.coerceIn(0, text.length)
        val endExclusive = (range.last + 1).coerceIn(start, text.length)
        if (start > cursor) {
            builder.append(text, cursor, start)
        }
        builder.append(replacement)
        cursor = endExclusive
    }
    if (cursor < text.length) {
        builder.append(text, cursor, text.length)
    }
    val newText = builder.toString()
    return TextFieldValue(text = newText, selection = TextRange(newText.length.coerceAtLeast(0)))
}

@Composable
private fun FindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentIndex: Int,
    totalMatches: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    onReplaceOne: () -> Unit,
    onReplaceAll: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = { Text(stringResource(R.string.find_in_note_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = if (totalMatches == 0) "0/0"
                    else "${currentIndex + 1}/$totalMatches",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                IconButton(onClick = onPrev, enabled = totalMatches > 0) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.find_previous_match)
                    )
                }
                IconButton(onClick = onNext, enabled = totalMatches > 0) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.find_next_match)
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.close)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    placeholder = { Text(stringResource(R.string.replace_in_note_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(
                    onClick = onReplaceOne,
                    enabled = totalMatches > 0
                ) {
                    Text(stringResource(R.string.replace_match))
                }
                TextButton(
                    onClick = onReplaceAll,
                    enabled = totalMatches > 0
                ) {
                    Text(stringResource(R.string.replace_all_matches))
                }
            }
        }
    }
}

private data class EditorStats(val words: Int, val chars: Int, val readMinutes: Int)

private fun computeStats(text: String): EditorStats {
    val chars = text.length
    val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
    // Mixed-language heuristic: 200 wpm OR 500 chars/min, whichever is larger.
    val wordMinutes = words / 200.0
    val charMinutes = chars / 500.0
    val minutes = max(1, ceil(max(wordMinutes, charMinutes)).toInt())
    val finalMinutes = if (chars == 0) 0 else minutes
    return EditorStats(words = words, chars = chars, readMinutes = finalMinutes)
}

@Composable
private fun WikilinkSuggestionsRow(
    suggestions: List<com.markleaf.notes.domain.model.Note>,
    onPick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.wikilink_suggestions_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            suggestions.forEach { note ->
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(note.title) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun TagSuggestionsRow(
    suggestions: List<String>,
    onPick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                text = stringResource(R.string.tag_suggestions_title),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
            suggestions.forEach { tag ->
                Text(
                    text = "#$tag",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(tag) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

