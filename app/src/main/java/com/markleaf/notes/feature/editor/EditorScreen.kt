package com.markleaf.notes.feature.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.AttachmentEntity
import com.markleaf.notes.data.repository.LocalNoteRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.UUID

import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Divider

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
    
    var content by remember { mutableStateOf("") }
    var saveTrigger by remember { mutableStateOf(0) }
    var isPreviewMode by remember { mutableStateOf(false) }

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
                // Ignore if not possible
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
            
            runBlocking {
                AppDatabase.getInstance(context).attachmentDao().insertAttachment(attachment)
            }
            
            val imageMarkdown = "\n![$fileName]($uri)\n"
            content += imageMarkdown
            saveTrigger++
        }
    }

    // Load note if editing
    if (noteId != null) {
        val db = AppDatabase.getInstance(context)
        val repo = LocalNoteRepository(db)
        val note = runBlocking { repo.getNote(noteId) }
        if (note != null && content.isEmpty()) {
            content = note.contentMarkdown
        }
    }

    // Auto-save with debounce
    LaunchedEffect(saveTrigger) {
        if (noteId != null && content.isNotEmpty()) {
            delay(1000) // 1 second debounce
            val db = AppDatabase.getInstance(context)
            val repo = LocalNoteRepository(db)
            val currentNote = runBlocking { repo.getNote(noteId) }
            if (currentNote != null) {
                val updatedNote = currentNote.copy(
                    contentMarkdown = content,
                    updatedAt = java.time.Instant.now()
                )
                runBlocking { repo.updateNote(updatedNote) }
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
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isPreviewMode && noteId != null) {
                        IconButton(onClick = { 
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Default.Image, contentDescription = "Add Image")
                        }
                    }
                    IconButton(onClick = { isPreviewMode = !isPreviewMode }) {
                        Text(
                            text = if (isPreviewMode) "Edit" else "Preview",
                            style = MaterialTheme.typography.labelLarge
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
        if (isPreviewMode) {
            val previewLines = SimpleMarkdownPreview.parse(content)
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
                        PreviewLineType.BULLET -> Text(
                            text = "• ${line.text}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        PreviewLineType.CHECKBOX_DONE -> Text(
                            text = "☑ ${line.text}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        PreviewLineType.CHECKBOX_TODO -> Text(
                            text = "☐ ${line.text}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        PreviewLineType.BODY -> Text(
                            text = line.text,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        PreviewLineType.EMPTY -> Text(
                            text = "",
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                if (backlinks.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Backlinks",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(backlinks) { backlink ->
                        Text(
                            text = backlink.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNoteClick(backlink.id) }
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.TopStart
                ) {
                    BasicTextField(
                        value = content,
                        onValueChange = {
                            content = it
                            saveTrigger++ // Trigger auto-save
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        decorationBox = { innerTextField ->
                            if (content.isEmpty()) {
                                Text(
                                    text = "Start writing...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                if (backlinks.isNotEmpty()) {
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Backlinks",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    ) {
                        items(backlinks) { backlink ->
                            Text(
                                text = backlink.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNoteClick(backlink.id) }
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
