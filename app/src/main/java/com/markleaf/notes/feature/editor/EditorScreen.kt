package com.markleaf.notes.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
import com.markleaf.notes.data.local.entity.AttachmentEntity
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.MarkdownSyntaxVisibility
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String? = null,
    onBack: () -> Unit,
    onLinkClick: (String) -> Unit = {},
    onNoteClick: (String) -> Unit = {}
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

    val backlinks by if (noteId != null) {
        repo.getBacklinks(noteId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList<com.markleaf.notes.domain.model.Note>()) }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && noteId != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore
            }
            
            val attachmentId = UUID.randomUUID().toString()
            val fileName = "image_${System.currentTimeMillis()}"
            val attachment = AttachmentEntity(
                id = attachmentId,
                noteId = noteId,
                uri = uri.toString(),
                fileName = fileName,
                mimeType = "image/*",
                createdAt = System.currentTimeMillis()
            )
            
            coroutineScope.launch {
                db.attachmentDao().insertAttachment(attachment)
                editorState = MarkdownEditActions.image(editorState, fileName, uri.toString())
                saveTrigger++
            }
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

    // Auto-save
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
                        text = if (isPreviewMode) "Preview" else if (noteId != null) "Edit Note" else "New Note",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_back), "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Text(if (isPreviewMode) "Edit" else "Preview", style = MaterialTheme.typography.labelLarge)
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
                        PreviewLineType.LINK -> Text(
                            text = "[[${line.text}]]",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                .clickable { onLinkClick(line.text) }
                        )
                        PreviewLineType.IMAGE -> {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                AsyncImage(
                                    model = line.extra,
                                    contentDescription = line.text,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentScale = ContentScale.Crop
                                )
                                Text(
                                    text = line.text,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
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
                        PreviewLineType.BODY -> InlineMarkdownText(line = line, onLinkClick = onLinkClick)
                        PreviewLineType.EMPTY -> Spacer(Modifier.height(8.dp))
                    }
                }

                if (backlinks.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(24.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(16.dp))
                        Text("Backlinks", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(backlinks) { backlink ->
                        Text(
                            text = backlink.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.fillMaxWidth().clickable { onNoteClick(backlink.id) }.padding(vertical = 8.dp)
                        )
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
                            checkbox = colorScheme.secondary
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
                            .semantics { contentDescription = "Note content" },
                        textStyle = TextStyle(color = MaterialTheme.colorScheme.onBackground),
                        visualTransformation = markdownVisualTransformation,
                        decorationBox = { innerTextField ->
                            if (editorState.text.isEmpty()) Text("Start writing...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            innerTextField()
                        }
                    )
                }

                MarkdownToolbar(
                    imageEnabled = noteId != null,
                    onBold = {
                        editorState = MarkdownEditActions.bold(editorState)
                        if (isLoaded) saveTrigger++
                    },
                    onItalic = {
                        editorState = MarkdownEditActions.italic(editorState)
                        if (isLoaded) saveTrigger++
                    },
                    onCheckbox = {
                        editorState = MarkdownEditActions.checkbox(editorState)
                        if (isLoaded) saveTrigger++
                    },
                    onMarkdownLink = {
                        editorState = MarkdownEditActions.markdownLink(editorState)
                        if (isLoaded) saveTrigger++
                    },
                    onWikiLink = {
                        editorState = MarkdownEditActions.wikiLink(editorState)
                        if (isLoaded) saveTrigger++
                    },
                    onImage = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    }
                )

                if (backlinks.isNotEmpty()) {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text("Backlinks", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                    LazyColumn(Modifier.fillMaxWidth().height(100.dp)) {
                        items(backlinks) { backlink ->
                            Text(
                                text = backlink.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth().clickable { onNoteClick(backlink.id) }.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkdownToolbar(
    imageEnabled: Boolean,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onCheckbox: () -> Unit,
    onMarkdownLink: () -> Unit,
    onWikiLink: () -> Unit,
    onImage: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBold) {
            Icon(Icons.Default.FormatBold, contentDescription = "Bold")
        }
        IconButton(onClick = onItalic) {
            Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
        }
        IconButton(onClick = onCheckbox) {
            Icon(Icons.Default.CheckBox, contentDescription = "Checkbox")
        }
        IconButton(onClick = onMarkdownLink) {
            Icon(Icons.Default.Link, contentDescription = "Markdown Link")
        }
        IconButton(onClick = onWikiLink) {
            Icon(Icons.Default.Link, contentDescription = "Wiki Link")
        }
        IconButton(
            onClick = onImage,
            enabled = imageEnabled
        ) {
            Icon(Icons.Default.Image, contentDescription = "Image")
        }
    }
}

@Composable
private fun InlineMarkdownText(
    line: PreviewLine,
    onLinkClick: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val annotated = buildAnnotatedString {
        line.segments.forEach { segment ->
            when (segment.type) {
                PreviewInlineType.TEXT -> append(segment.text)
                PreviewInlineType.NOTE_LINK -> {
                    val target = segment.target.orEmpty()
                    pushStringAnnotation(tag = "note-link", annotation = target)
                    withStyle(
                        SpanStyle(
                            color = primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(segment.text)
                    }
                    pop()
                }
                PreviewInlineType.MARKDOWN_LINK -> {
                    val target = segment.target.orEmpty()
                    pushStringAnnotation(tag = "markdown-link", annotation = target)
                    withStyle(
                        SpanStyle(
                            color = primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(segment.text)
                    }
                    pop()
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
        onClick = { offset ->
            annotated.getStringAnnotations("note-link", offset, offset).firstOrNull()?.let {
                onLinkClick(it.item)
                return@ClickableText
            }
            annotated.getStringAnnotations("markdown-link", offset, offset).firstOrNull()?.let {
                if (!it.item.startsWith("http://") && !it.item.startsWith("https://")) {
                    onLinkClick(it.item)
                }
            }
        }
    )
}
