package com.markleaf.notes.feature.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTooltipState
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    var isPreviewMode by remember(noteId) { mutableStateOf(false) }
    var isFocusMode by remember(noteId) { mutableStateOf(false) }
    var showDeleteConfirm by remember(noteId) { mutableStateOf(false) }

    var isFindOpen by remember(noteId) { mutableStateOf(false) }
    var findQuery by remember(noteId) { mutableStateOf("") }
    var findIndex by remember(noteId) { mutableStateOf(0) }
    var shareMenuExpanded by remember(noteId) { mutableStateOf(false) }
    var overflowExpanded by remember(noteId) { mutableStateOf(false) }
    var pendingExport by remember(noteId) { mutableStateOf<Note?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
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
                        os.write(ExportUtil.generateMarkdownContent(note).toByteArray())
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
            editorState = TextFieldValue(repo.getNote(noteId)?.contentMarkdown.orEmpty())
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
                    if (uri != null) {
                        withContext(Dispatchers.IO) {
                            NoteFolderMirror.writeNote(context, uri, updatedNote)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when {
                            isFocusMode -> stringResource(R.string.focus_mode)
                            isPreviewMode -> stringResource(R.string.preview)
                            noteId != null -> stringResource(R.string.edit_note)
                            else -> stringResource(R.string.new_note)
                        },
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
                    }
                },
                actions = {
                    if (isFocusMode) {
                        IconButton(onClick = { isFocusMode = false }) {
                            Icon(
                                Icons.Default.CenterFocusWeak,
                                contentDescription = stringResource(R.string.exit_focus_mode)
                            )
                        }
                    } else {
                        if (!isPreviewMode) {
                            IconButton(onClick = {
                                isFindOpen = !isFindOpen
                                if (isFindOpen) findQuery = ""
                            }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = stringResource(R.string.find_in_note)
                                )
                            }
                        }
                        TextButton(onClick = { isPreviewMode = !isPreviewMode }) {
                            Text(
                                if (isPreviewMode) stringResource(R.string.edit) else stringResource(R.string.preview),
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        if (noteId != null) {
                            Box {
                                IconButton(onClick = { shareMenuExpanded = true }) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = stringResource(R.string.share_note)
                                    )
                                }
                                DropdownMenu(
                                    expanded = shareMenuExpanded,
                                    onDismissRequest = { shareMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.share_via_system)) },
                                        onClick = {
                                            shareMenuExpanded = false
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
                                            shareMenuExpanded = false
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
                                }
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
                                    if (!isPreviewMode) {
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
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (isPreviewMode) {
            val previewLines = SimpleMarkdownPreview.parse(editorState.text)
            val currentTitle = remember(editorState.text) {
                TitleExtractor.extractTitle(editorState.text)
            }
            val backlinks by linkRepo.observeBacklinks(currentTitle, noteId.orEmpty())
                .collectAsState(initial = emptyList())
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                MarkdownPreviewList(
                    lines = previewLines,
                    modifier = Modifier.weight(1f, fill = false),
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
                    }
                )
                if (backlinks.isNotEmpty()) {
                    BacklinksPanel(
                        backlinks = backlinks,
                        onClick = { id -> onNavigateToNote(id) }
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
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
                        }
                    )
                }

                val colorScheme = MaterialTheme.colorScheme
                val markdownVisualTransformation = if (
                    appSettings.markdownSyntaxVisibility == MarkdownSyntaxVisibility.SHOW && !isFocusMode
                ) {
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
                Box(Modifier.weight(1f), contentAlignment = Alignment.TopStart) {
                    BasicTextField(
                        value = editorState,
                        onValueChange = { incoming ->
                            editorState = MarkdownEditActions.applyAutoContinuation(editorState, incoming)
                            if (isLoaded) saveTrigger++
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.note_content) }
                            .onPreviewKeyEvent { event ->
                                if (event.type == KeyEventType.KeyDown && event.key == Key.Tab) {
                                    editorState = if (event.isShiftPressed) {
                                        MarkdownEditActions.outdent(editorState)
                                    } else {
                                        MarkdownEditActions.indent(editorState)
                                    }
                                    if (isLoaded) saveTrigger++
                                    true
                                } else {
                                    false
                                }
                            },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                        visualTransformation = markdownVisualTransformation,
                        decorationBox = { innerTextField ->
                            if (editorState.text.isEmpty()) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Text(
                                        text = "✏️",
                                        style = MaterialTheme.typography.displayMedium,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.editor_empty_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = stringResource(R.string.editor_empty_hint),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            } else {
                                innerTextField()
                            }
                        }
                    )
                }

                if (!isFocusMode && editorState.text.isNotEmpty()) {
                    val stats = remember(editorState.text) { computeStats(editorState.text) }
                    EditorStatsRow(stats)
                }

                if (!isFocusMode) {
                    MarkdownToolbar(
                        onAction = { action ->
                            HapticFeedback.light(context)
                            editorState = action(editorState)
                            if (isLoaded) saveTrigger++
                        },
                        onPickImage = {
                            HapticFeedback.light(context)
                            imagePickerLauncher.launch(arrayOf("image/*"))
                        }
                    )
                }
            }
        }
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

@Composable
private fun FindBar(
    query: String,
    onQueryChange: (String) -> Unit,
    currentIndex: Int,
    totalMatches: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
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
private fun EditorStatsRow(stats: EditorStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                R.string.editor_stats_format,
                stats.words,
                stats.chars,
                stats.readMinutes
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BacklinksPanel(
    backlinks: List<com.markleaf.notes.domain.model.Note>,
    onClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.backlinks_section_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(6.dp))
        backlinks.forEach { note ->
            Text(
                text = note.title.ifEmpty { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onClick(note.id) }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MarkdownToolbar(
    onAction: ((TextFieldValue) -> TextFieldValue) -> Unit,
    onPickImage: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarTooltipIconButton(
            label = stringResource(R.string.heading),
            onClick = { onAction(MarkdownEditActions::heading) }
        ) {
            Icon(Icons.Default.Title, contentDescription = stringResource(R.string.heading))
        }
        ToolbarDivider()
        ToolbarTooltipIconButton(
            label = stringResource(R.string.bullet_list),
            onClick = { onAction(MarkdownEditActions::bulletList) }
        ) {
            Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = stringResource(R.string.bullet_list))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.ordered_list),
            onClick = { onAction(MarkdownEditActions::orderedList) }
        ) {
            Icon(Icons.Default.FormatListNumbered, contentDescription = stringResource(R.string.ordered_list))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.checkbox),
            onClick = { onAction(MarkdownEditActions::checkbox) }
        ) {
            Icon(Icons.Default.CheckBox, contentDescription = stringResource(R.string.checkbox))
        }
        ToolbarDivider()
        ToolbarTooltipIconButton(
            label = stringResource(R.string.bold),
            onClick = { onAction(MarkdownEditActions::bold) }
        ) {
            Icon(Icons.Default.FormatBold, contentDescription = stringResource(R.string.bold))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.italic),
            onClick = { onAction(MarkdownEditActions::italic) }
        ) {
            Icon(Icons.Default.FormatItalic, contentDescription = stringResource(R.string.italic))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.strikethrough),
            onClick = { onAction(MarkdownEditActions::strikethrough) }
        ) {
            Icon(Icons.Default.FormatStrikethrough, contentDescription = stringResource(R.string.strikethrough))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.inline_code),
            onClick = { onAction(MarkdownEditActions::inlineCode) }
        ) {
            Icon(Icons.Default.Code, contentDescription = stringResource(R.string.inline_code))
        }
        ToolbarDivider()
        ToolbarTooltipIconButton(
            label = stringResource(R.string.blockquote),
            onClick = { onAction(MarkdownEditActions::blockquote) }
        ) {
            Icon(Icons.Default.FormatQuote, contentDescription = stringResource(R.string.blockquote))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.code_block),
            onClick = { onAction(MarkdownEditActions::codeBlock) }
        ) {
            Icon(Icons.Default.DataObject, contentDescription = stringResource(R.string.code_block))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.horizontal_rule),
            onClick = { onAction(MarkdownEditActions::horizontalRule) }
        ) {
            Icon(Icons.Default.HorizontalRule, contentDescription = stringResource(R.string.horizontal_rule))
        }
        ToolbarDivider()
        ToolbarTooltipIconButton(
            label = stringResource(R.string.markdown_link),
            onClick = { onAction(MarkdownEditActions::markdownLink) }
        ) {
            Icon(Icons.Default.Link, contentDescription = stringResource(R.string.markdown_link))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.insert_image),
            onClick = onPickImage
        ) {
            Icon(Icons.Default.Image, contentDescription = stringResource(R.string.insert_image))
        }
    }
}


@Composable
private fun ToolbarDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(20.dp)
            .padding(horizontal = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ToolbarTooltipIconButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(label)
            }
        },
        state = rememberTooltipState()
    ) {
        IconButton(
            modifier = Modifier.semantics { contentDescription = label },
            onClick = onClick,
            enabled = enabled
        ) {
            content()
        }
    }
}

