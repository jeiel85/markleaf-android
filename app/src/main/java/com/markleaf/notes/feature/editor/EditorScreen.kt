package com.markleaf.notes.feature.editor

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.CalloutKind
import com.markleaf.notes.core.markdown.MarkdownEditActions
import com.markleaf.notes.core.markdown.MarkdownSyntaxColors
import com.markleaf.notes.core.markdown.MarkdownSyntaxVisualTransformation
import com.markleaf.notes.core.markdown.PreviewInlineType
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.PreviewLine
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.MarkdownSyntaxVisibility
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.util.ExportUtil
import com.markleaf.notes.util.HapticFeedback
import com.markleaf.notes.util.ShareNoteUtil
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val repo = remember { LocalNoteRepository(db) }
    val tagRepo = remember { LocalTagRepository(db) }
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
    var pendingExport by remember(noteId) { mutableStateOf<Note?>(null) }
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
                            IconButton(onClick = { isFocusMode = true }) {
                                Icon(
                                    Icons.Default.CenterFocusStrong,
                                    contentDescription = stringResource(R.string.focus_mode)
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
                            IconButton(onClick = { showDeleteConfirm = true }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.move_to_trash)
                                )
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(previewLines) { line ->
                    when (line.type) {
                        PreviewLineType.H1 -> Text(
                            text = line.text,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        PreviewLineType.H2 -> Text(
                            text = line.text,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
                        )
                        PreviewLineType.H3 -> Text(
                            text = line.text,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
                        )
                        PreviewLineType.BULLET -> Text("• ${line.text}", style = MaterialTheme.typography.bodyLarge)
                        PreviewLineType.CHECKBOX_DONE -> Text("☑ ${line.text}", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        PreviewLineType.CHECKBOX_TODO -> Text("☐ ${line.text}", style = MaterialTheme.typography.bodyLarge)
                        PreviewLineType.CODE_BLOCK -> MarkdownCodeBlock(line.text, line.extra)
                        PreviewLineType.BODY -> InlineMarkdownText(line = line)
                        PreviewLineType.BLOCKQUOTE -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                InlineMarkdownText(line = line)
                                HorizontalDivider(
                                    modifier = Modifier.padding(top = 4.dp),
                                    thickness = 2.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                        PreviewLineType.CALLOUT -> CalloutBox(line)
                        PreviewLineType.FRONTMATTER -> FrontmatterBlock(line.text)
                        PreviewLineType.FOOTNOTE_DEF -> FootnoteDefRow(line)
                        PreviewLineType.ORDERED_LIST -> Text(
                            text = "${line.extra ?: "1"}. ${line.text}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        PreviewLineType.HORIZONTAL_RULE -> HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        PreviewLineType.EMPTY -> Spacer(Modifier.height(8.dp))
                    }
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
@OptIn(ExperimentalMaterial3Api::class)
private fun MarkdownToolbar(
    onAction: ((TextFieldValue) -> TextFieldValue) -> Unit
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

@Composable
private fun InlineMarkdownText(
    line: PreviewLine
) {
    val annotated = buildAnnotatedString {
        line.segments.forEach { segment ->
            when (segment.type) {
                PreviewInlineType.TEXT -> append(segment.text)
                PreviewInlineType.BOLD -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(segment.text)
                    }
                }
                PreviewInlineType.ITALIC -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(segment.text)
                    }
                }
                PreviewInlineType.BOLD_ITALIC -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) {
                        append(segment.text)
                    }
                }
                PreviewInlineType.STRIKETHROUGH -> {
                    withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) {
                        append(segment.text)
                    }
                }
                PreviewInlineType.INLINE_CODE -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        append(segment.text)
                    }
                }
                PreviewInlineType.FOOTNOTE_REF -> {
                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            baselineShift = BaselineShift.Superscript
                        )
                    ) {
                        append(segment.text)
                    }
                }
            }
        }
    }

    ClickableText(
        text = annotated,
        style = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onBackground
        ),
        modifier = Modifier.padding(vertical = 2.dp),
        onClick = { /* no inline click targets in simplified preview */ }
    )
}

@Composable
private fun CalloutBox(line: PreviewLine) {
    val kind = CalloutKind.parse(line.extra.orEmpty())
    val (containerColor, accentColor, label, icon) = when (kind) {
        CalloutKind.NOTE -> CalloutVisuals(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            stringResource(R.string.callout_note),
            "ℹ"
        )
        CalloutKind.TIP -> CalloutVisuals(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.secondary,
            stringResource(R.string.callout_tip),
            "💡"
        )
        CalloutKind.IMPORTANT -> CalloutVisuals(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.tertiary,
            stringResource(R.string.callout_important),
            "★"
        )
        CalloutKind.WARNING -> CalloutVisuals(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            stringResource(R.string.callout_warning),
            "⚠"
        )
        CalloutKind.CAUTION -> CalloutVisuals(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            stringResource(R.string.callout_caution),
            "⛔"
        )
        null -> CalloutVisuals(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            line.extra.orEmpty(),
            "•"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, color = accentColor)
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (line.text.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            line.text.split("\n").forEach { bodyLine ->
                if (bodyLine.isBlank()) {
                    Spacer(Modifier.height(4.dp))
                } else {
                    InlineMarkdownText(
                        line = PreviewLine(
                            text = bodyLine,
                            type = PreviewLineType.BODY,
                            segments = SimpleMarkdownPreview.parseInlineSegments(bodyLine)
                        )
                    )
                }
            }
        }
    }
}

private data class CalloutVisuals(
    val containerColor: androidx.compose.ui.graphics.Color,
    val accentColor: androidx.compose.ui.graphics.Color,
    val label: String,
    val icon: String
)

@Composable
private fun FrontmatterBlock(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun FootnoteDefRow(line: PreviewLine) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "[^${line.extra}]",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp)
        )
        InlineMarkdownText(line = line.copy(type = PreviewLineType.BODY))
    }
}

@Composable
private fun MarkdownCodeBlock(text: String, language: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (!language.isNullOrEmpty()) {
            Text(
                text = language,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
