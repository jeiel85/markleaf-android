package com.markleaf.notes.feature.sync

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.markleaf.notes.R
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.sync.MirrorMetadata
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.NoteImporter
import com.markleaf.notes.data.sync.SyncFolderLink
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The folder picker, the question it has to ask, and the link that follows —
 * once, for both screens that offer it.
 *
 * Settings and the Sync Center each had their own copy of "take the grant and
 * write the notes out", and the import that every later sync pass runs was
 * added to neither (#372). Sharing the whole flow is what keeps the two in
 * step; the count in the dialog and the counts in the toast come from the same
 * pass that does the work.
 *
 * Returns the lambda that opens the picker. The dialog is emitted from here, so
 * a caller only wires the button.
 */
@Composable
fun rememberSyncFolderLinker(
    settingsRepository: AppSettingsRepository,
    noteRepository: LocalNoteRepository,
    noteImporter: NoteImporter,
    appSettings: AppSettings
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pending by remember { mutableStateOf<PendingLink?>(null) }

    fun link(folderUri: Uri) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                SyncFolderLink.link(
                    context = context,
                    folderUri = folderUri,
                    settingsRepository = settingsRepository,
                    noteRepository = noteRepository,
                    noteImporter = noteImporter,
                    extension = appSettings.syncFileExtension,
                    metadata = appSettings.mirrorMetadata(),
                    titleSource = appSettings.noteTitleSource
                )
            }
            val brought = result.imported.created + result.imported.updated
            Toast.makeText(
                context,
                context.getString(R.string.sync_linked_format, result.seeded, brought),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            // Persist read+write so the URI keeps working after a reboot.
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            runCatching {
                context.contentResolver.takePersistableUriPermission(folderUri, flags)
            }
            scope.launch {
                val metadata = appSettings.mirrorMetadata()
                val survey = withContext(Dispatchers.IO) {
                    // The complete set, for the reason importChanges needs it:
                    // a file matching an archived or trashed note is not a new
                    // note, and counting it as one would promise arrivals that
                    // never come.
                    val all = noteRepository.getAllNotes()
                    NoteFolderMirror.surveyFolder(context, folderUri, all, metadata)
                }
                // Nothing of the user's is about to be adopted, so there is
                // nothing to ask about — an empty folder, or one holding only
                // files these notes already own.
                if (survey.newFiles > 0) {
                    pending = PendingLink(folderUri, survey.newFiles, metadata)
                } else {
                    link(folderUri)
                }
            }
        }
    }

    pending?.let { request ->
        val dismiss = {
            val abandoned = request.folderUri
            pending = null
            // Hand the grant back unless it is the folder already in use:
            // releasing that one would break a working sync because the user
            // said no to re-linking it.
            if (appSettings.syncFolderUriOrNull() != abandoned) {
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        abandoned,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
            }
            Unit
        }
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.sync_link_import_title)) },
            text = {
                val message = stringResource(
                    R.string.sync_link_import_message_format,
                    request.newFiles
                )
                // The header warning belongs to the default mode only — in
                // sidecar mode the files are left as the user wrote them, and
                // saying otherwise would talk someone out of a link that costs
                // them nothing.
                val body = if (request.metadata is MirrorMetadata.Frontmatter) {
                    message + "\n\n" + stringResource(
                        R.string.sync_link_import_frontmatter_note,
                        stringResource(R.string.sync_metadata_mode),
                        stringResource(R.string.sync_metadata_mode_sidecar)
                    )
                } else {
                    message
                }
                Text(body)
            },
            confirmButton = {
                TextButton(onClick = {
                    val uri = request.folderUri
                    pending = null
                    link(uri)
                }) {
                    Text(stringResource(R.string.sync_link_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = dismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    return { launcher.launch(null) }
}

/** A folder the user picked, waiting on the answer to "shall I read these in?". */
private data class PendingLink(
    val folderUri: Uri,
    val newFiles: Int,
    val metadata: MirrorMetadata
)
