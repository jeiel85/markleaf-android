package com.markleaf.notes.feature.viewer

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.core.markdown.preview.MarkdownPreviewList
import com.markleaf.notes.core.markdown.PreviewLine
import com.markleaf.notes.util.ExternalFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant

/**
 * Reading one file that is not a note (#326).
 *
 * Markleaf could already take a file *in* — a share, or a tap in a file manager
 * — but only by copying it into the note database, and, with folder sync on,
 * into the sync folder as well. Reading something once left two artifacts
 * behind. This screen renders the file where it is and writes nothing; keeping
 * it is a deliberate tap on **Save as note**, which runs the same import as
 * before.
 *
 * Deliberately not a file browser: one file, no recents, no persisted URI
 * permissions, no second library. Notes still live in exactly one place.
 */
sealed interface FileViewerState {
    /** The file is being read. Brief for a note-sized file; not always for one on a cloud provider. */
    data object Loading : FileViewerState

    data class Loaded(
        val displayName: String?,
        val lines: List<PreviewLine>,
        /** What [FileViewerScreen]'s save action hands back — title seeding included. */
        val noteBody: String,
        val createdAt: Instant? = null,
        val updatedAt: Instant? = null
    ) : FileViewerState

    /** Moved, deleted, not text, or a permission that lapsed while the app was away. */
    data object Unreadable : FileViewerState
}

@Composable
fun FileViewerScreen(
    uri: Uri,
    onBack: () -> Unit = {},
    onSaveAsNote: (String, Instant?, Instant?) -> Unit = { _, _, _ -> },
    contentMaxWidth: Dp = Dp.Unspecified
) {
    val context = LocalContext.current
    var state by remember(uri) { mutableStateOf<FileViewerState>(FileViewerState.Loading) }

    LaunchedEffect(uri) {
        // Reading and parsing both happen off the main thread: a long file's
        // preview parse is comparable to its read, and this screen is often
        // entered straight from a cold start (ACTION_VIEW).
        state = withContext(Dispatchers.IO) {
            val document = ExternalFile.read(context, uri)
                ?: return@withContext FileViewerState.Unreadable
            val noteSeed = ExternalFile.noteSeed(document)
            FileViewerState.Loaded(
                displayName = document.displayName,
                lines = SimpleMarkdownPreview.parse(document.text),
                noteBody = noteSeed.body,
                createdAt = noteSeed.createdAt,
                updatedAt = noteSeed.updatedAt
            )
        }
    }

    FileViewerContent(
        state = state,
        onBack = onBack,
        onSaveAsNote = onSaveAsNote,
        contentMaxWidth = contentMaxWidth
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileViewerContent(
    state: FileViewerState,
    onBack: () -> Unit = {},
    onSaveAsNote: (String, Instant?, Instant?) -> Unit = { _, _, _ -> },
    /** The reading measure the editor honours on wide screens; unconstrained on a phone. */
    contentMaxWidth: Dp = Dp.Unspecified
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = (state as? FileViewerState.Loaded)?.displayName
                                ?: stringResource(R.string.file_viewer_title),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // The bar is the only thing telling the reader this is a
                        // file rather than a note: there is no caret to discover
                        // it with, and the save action would otherwise read as
                        // "save my edits".
                        Text(
                            text = stringResource(R.string.file_viewer_read_only),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (state is FileViewerState.Loaded) {
                        IconButton(onClick = {
                            onSaveAsNote(state.noteBody, state.createdAt, state.updatedAt)
                        }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.NoteAdd,
                                contentDescription = stringResource(R.string.file_viewer_save_as_note)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                FileViewerState.Loading -> CircularProgressIndicator()
                FileViewerState.Unreadable -> Text(
                    text = stringResource(R.string.file_viewer_unreadable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                is FileViewerState.Loaded -> MarkdownPreviewList(
                    lines = state.lines,
                    // Bounded like the tablet editor: a rendered document read
                    // edge to edge on a wide screen is the case the line-width
                    // setting exists for.
                    modifier = Modifier
                        .fillMaxHeight()
                        .widthIn(max = contentMaxWidth),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                    // No onToggleTask and no wikilink handling: a checkbox here
                    // would have nowhere to write, and a wikilink would either
                    // create a note or lie about resolving. The file is read
                    // only until the reader chooses to keep it.
                )
            }
        }
    }
}
