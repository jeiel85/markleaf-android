package com.markleaf.notes.feature.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
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
import com.markleaf.notes.util.HapticFeedback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var showDeleteConfirm by remember(noteId) { mutableStateOf(false) }

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
                        text = if (isPreviewMode) {
                            stringResource(R.string.preview)
                        } else if (noteId != null) {
                            stringResource(R.string.edit_note)
                        } else {
                            stringResource(R.string.new_note)
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
                    TextButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Text(
                            if (isPreviewMode) stringResource(R.string.edit) else stringResource(R.string.preview),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (noteId != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.move_to_trash)
                            )
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
                val colorScheme = MaterialTheme.colorScheme
                val markdownVisualTransformation = if (appSettings.markdownSyntaxVisibility == MarkdownSyntaxVisibility.SHOW) {
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
                        onValueChange = {
                            editorState = it
                            if (isLoaded) saveTrigger++
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = context.getString(R.string.note_content) },
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

                MarkdownToolbar(
                    onBold = {
                        HapticFeedback.light(context)
                        editorState = MarkdownEditActions.bold(editorState)
                        if (isLoaded) saveTrigger++
                    },
                    onItalic = {
                        HapticFeedback.light(context)
                        editorState = MarkdownEditActions.italic(editorState)
                        if (isLoaded) saveTrigger++
                    },
                    onCheckbox = {
                        HapticFeedback.light(context)
                        editorState = MarkdownEditActions.checkbox(editorState)
                        if (isLoaded) saveTrigger++
                    },
                    onMarkdownLink = {
                        HapticFeedback.light(context)
                        editorState = MarkdownEditActions.markdownLink(editorState)
                        if (isLoaded) saveTrigger++
                    }
                )
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MarkdownToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onCheckbox: () -> Unit,
    onMarkdownLink: () -> Unit
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
            label = stringResource(R.string.bold),
            onClick = onBold
        ) {
            Icon(Icons.Default.FormatBold, contentDescription = stringResource(R.string.bold))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.italic),
            onClick = onItalic
        ) {
            Icon(Icons.Default.FormatItalic, contentDescription = stringResource(R.string.italic))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.checkbox),
            onClick = onCheckbox
        ) {
            Icon(Icons.Default.CheckBox, contentDescription = stringResource(R.string.checkbox))
        }
        ToolbarTooltipIconButton(
            label = stringResource(R.string.markdown_link),
            onClick = onMarkdownLink
        ) {
            Icon(Icons.Default.Link, contentDescription = stringResource(R.string.markdown_link))
        }
    }
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
