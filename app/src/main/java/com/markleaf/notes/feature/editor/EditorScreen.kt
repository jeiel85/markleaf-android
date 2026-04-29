package com.markleaf.notes.feature.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val content = remember { mutableStateOf("") }

    // Load note if editing
    if (noteId != null) {
        val db = AppDatabase.getInstance(context)
        val repo = LocalNoteRepository(db)
        val note = runBlocking { repo.getNote(noteId) }
        if (note != null) {
            content.value = note.contentMarkdown
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (noteId != null) "Edit Note" else "New Note",
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.TopStart
        ) {
            BasicTextField(
                value = content.value,
                onValueChange = { content.value = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    if (content.value.isEmpty()) {
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
    }
}
