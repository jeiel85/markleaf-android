package com.markleaf.notes.feature.editor

import android.util.Log
import android.widget.Toast
import com.markleaf.notes.BuildConfig
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.core.markdown.MarkdownEditActions
import com.markleaf.notes.core.markdown.MarkdownSyntaxVisualTransformation
import com.markleaf.notes.core.markdown.PreviewLineType
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.core.markdown.markdownSyntaxColors
import com.markleaf.notes.core.markdown.preview.MarkdownPreviewList
import com.markleaf.notes.core.markdown.preview.TocHeading
import com.markleaf.notes.core.markdown.preview.extractHeadings
import com.markleaf.notes.core.markdown.preview.visiblePreviewLines
import com.markleaf.notes.core.text.TitleExtractor
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.local.entity.NoteViewStateEntity
import com.markleaf.notes.data.repository.LocalNoteLinkRepository
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.MarkdownSyntaxVisibility
import com.markleaf.notes.data.settings.OpenNotesAt
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.util.AttachmentManager
import com.markleaf.notes.util.ExportUtil
import com.markleaf.notes.util.HapticFeedback
import com.markleaf.notes.util.LocalMarkdownLink
import com.markleaf.notes.util.ExportPdf
import com.markleaf.notes.util.ShareNoteUtil
import com.markleaf.notes.widget.WidgetRefresh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant

internal fun editorSaveTime(
    persistedContent: String,
    content: String,
    openedContent: String?,
    openedUpdatedAt: Instant?,
    now: Instant
): Instant? = when {
    persistedContent == content -> null
    content == openedContent -> openedUpdatedAt ?: now
    else -> now
}

internal fun editorNeedsMirrorRetry(
    syncFolderConfigured: Boolean,
    locked: Boolean,
    lastImportedAt: Instant?,
    updatedAt: Instant
): Boolean = syncFolderConfigured && !locked && lastImportedAt != updatedAt

/** The production settings repository — the process-wide DataStore singleton. */
@Composable
private fun rememberAppSettingsRepository(): AppSettingsRepository {
    val context = LocalContext.current
    return remember(context) { AppSettingsRepository(context.applicationContext) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: String? = null,
    onBack: () -> Unit,
    onNavigateToNote: (String) -> Unit = {},
    // Injectable for the same reason [AppSettingsRepository] takes a DataStore
    // (#158): the production `preferencesDataStore` delegate caches one instance
    // per JVM, so a Robolectric test cannot put the real store into a chosen
    // state. Composing this screen with a setting turned off is what the golden
    // for "Show formatting button" needs (#331) — the row is *not mounted* when
    // it is off, so nothing below screen level can show that it left no gap.
    settingsRepository: AppSettingsRepository = rememberAppSettingsRepository(),
    // Where the empty-note cleanup below launches its delete from. It has to be
    // a scope that outlives *this* composable, not the `rememberCoroutineScope()`
    // declared further down — that one is cancelled as part of the same teardown
    // that fires the DisposableEffect's onDispose, which races the delete against
    // its own cancellation. The caller passes down a scope tied to an ancestor
    // (the NavHost); the default here only covers tests and previews that never
    // exercise disposal.
    hostScope: CoroutineScope = rememberCoroutineScope()
) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getInstance(context) }
    val viewStateDao = remember { db.noteViewStateDao() }
    // Last-known position of the surface that is not on screen, kept in memory
    // instead of reading the row back on every debounced save (#262). Seeded
    // from the persisted row when the note loads, so the first write still
    // preserves the other surface's value.
    var lastCaretOffset by remember(noteId) { mutableStateOf<Int?>(null) }
    var lastPreviewIndex by remember(noteId) { mutableStateOf<Int?>(null) }
    val repo = remember { LocalNoteRepository(db) }
    val tagRepo = remember { LocalTagRepository(db) }
    val linkRepo = remember { LocalNoteLinkRepository(db) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val coroutineScope = rememberCoroutineScope()

    var editorState by remember(noteId) { mutableStateOf(TextFieldValue("")) }
    var openedContent by remember(noteId) { mutableStateOf<String?>(null) }
    var openedUpdatedAt by remember(noteId) { mutableStateOf<Instant?>(null) }
    // Per open note, and dropped when the screen leaves: Markleaf keeps no
    // on-disk edit history, so this is a way back from the edit you just made,
    // not a version store (#360).
    val undoHistory = remember(noteId) { EditorUndoHistory() }
    val titleSource = appSettings.noteTitleSource
    // The persistence behind the debounce gate: reads the note fresh from the
    // DB so a save never clobbers a newer row, then updates the sync mirror
    // when one is configured (#262).
    suspend fun autosave(content: String) {
        val id = noteId ?: return
        val currentNote = repo.getNote(id)
        if (currentNote != null) {
            val saveTime = editorSaveTime(
                currentNote.contentMarkdown, content, openedContent, openedUpdatedAt, Instant.now()
            )
            val syncUri = appSettings.syncFolderUriOrNull()
            if (saveTime == null && !editorNeedsMirrorRetry(
                    syncUri != null, currentNote.locked, currentNote.lastImportedAt, currentNote.updatedAt
                )) return
            val updatedNote = if (saveTime != null) {
                currentNote.copy(
                    title = TitleExtractor.extractTitle(content, appSettings.noteTitleSource),
                    contentMarkdown = content,
                    excerpt = TitleExtractor.generateExcerpt(content, appSettings.noteTitleSource),
                    updatedAt = saveTime
                )
            } else currentNote
            if (saveTime != null) {
                repo.updateNote(updatedNote)
                tagRepo.reindexTagsForNote(id, content)
                linkRepo.reindexLinksForNote(id, content)
            }
            syncUri?.let { uri ->
                // Never mirror a locked note to the sync folder — the Locked
                // space is meant to stay on-device, and the mirror writes plain
                // text (#155). Removing the lock re-includes it on the next save.
                if (!updatedNote.locked) {
                    val ok = withContext(Dispatchers.IO) {
                        val wrote = NoteFolderMirror.writeNote(
                            context,
                            uri,
                            updatedNote,
                            appSettings.syncFileExtension,
                            appSettings.mirrorMetadata()
                        )
                        if (wrote) {
                            val attachments = AttachmentManager.filesForNote(context, id)
                            if (attachments.isNotEmpty()) {
                                NoteFolderMirror.mirrorAttachments(context, uri, id, attachments)
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
            // A single-note widget draws this note's body, so it goes stale the
            // moment the row changes. MainActivity.onPause also refreshes, but
            // pressing Home inside the one-second debounce window runs that
            // refresh *before* this save lands — leaving the old text on the
            // home screen until the next pause. Refreshing here means the widget
            // follows the write rather than racing it (#351).
            //
            // Both kinds, not just the single-note one: this save also moves the
            // note to the top of the recent list the other widget draws, and it
            // loses the same race (#262).
            if (saveTime != null) WidgetRefresh.notesChanged(context)
        }
    }
    // Debounced autosave gate: every edit and formatting action bumps it, and
    // it coalesces a burst into one save of the latest text (#262). The save
    // body lives in [autosave] so the timing/ordering contract is testable.
    val saver = remember(noteId) {
        DebouncedSaver(
            debounceMillis = SAVE_DEBOUNCE_MS,
            readContent = { editorState.text },
            save = { content -> autosave(content) }
        )
    }
    var isLoaded by remember(noteId) { mutableStateOf(noteId == null) }
    // #405: a note that is left with nothing in it -- never typed into, or
    // typed into and then cleared back out -- has nothing worth keeping, so
    // it is removed instead of sitting in the list as a blank row. Gated on
    // isLoaded so a note whose real content has not finished loading yet is
    // never mistaken for an empty one. DisposableEffect(noteId) reruns only
    // when noteId itself changes, which for this screen means this note's
    // visit is actually ending -- the back icon, system back, and switching
    // to a different note all remove this composable from composition.
    //
    // The row is re-read fresh rather than trusted to a flag this screen set
    // earlier: a first version gated this on a `wasSentToTrash` boolean set
    // only by this file's own delete-confirm dialog, which a review caught
    // missing every OTHER way a note's disposition can change out from under
    // an open editor -- archiving, locking, or trashing the same note from
    // the tablet's list pane, which sits open beside this one in the
    // two-pane layout and reaches NotesViewModel directly, never touching
    // this screen at all. Checking the note's own persisted flags instead
    // covers every such surface by construction, present and future, rather
    // than one flag per action that happens to also navigate away.
    DisposableEffect(noteId) {
        onDispose {
            val id = noteId
            if (id != null && isLoaded && editorState.text.isBlank()) {
                hostScope.launch {
                    // Best-effort cleanup: a failure here (a revoked SAF grant
                    // on the sync folder, a disk error) must not crash the
                    // app over a note the user was not even trying to save.
                    // CancellationException is rethrown rather than swallowed
                    // -- that one is how structured concurrency itself works,
                    // not a failure to log.
                    try {
                        val current = repo.getNote(id)
                        val hasDeliberateDisposition = current != null && (
                            current.trashed || current.archived || current.locked || current.pinned
                        )
                        if (current != null && !hasDeliberateDisposition) {
                            // Sequential, deliberately: an earlier version ran
                            // these three concurrently via a nested
                            // `coroutineScope { launch {...} }` to match
                            // TrashScreen's own delete-forever flow, and that
                            // made EditorDiscardsBlankNoteTest /
                            // EditorDiscardsClearedNoteTest genuinely flaky
                            // (reproduced locally: 2 of 5 runs failed,
                            // alternating which one) -- composeRule.waitForIdle()
                            // does not reliably wait out a child launch nested
                            // inside another launch the way it does hostScope's
                            // own direct suspension points. Correctness over a
                            // minor latency win.
                            repo.deleteForever(id)
                            withContext(Dispatchers.IO) {
                                AttachmentManager.deleteAllForNote(context, id)
                            }
                            appSettings.syncFolderUriOrNull()?.let { uri ->
                                withContext(Dispatchers.IO) {
                                    NoteFolderMirror.deleteNote(context, uri, id, appSettings.mirrorMetadata())
                                }
                            }
                            WidgetRefresh.notesChanged(context)
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (error: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.d("EditorScreen", "empty-note discard failed for $id", error)
                        }
                    }
                }
            }
        }
    }
    var shouldRequestEditorFocus by remember(noteId) { mutableStateOf(noteId == null) }
    // Every edit path in this screen writes to [editorState], so the history
    // watches that one value rather than being pushed to from each call site —
    // an edit path added later is undoable without being wired up by hand.
    // [EditorUndoHistory.record] ignores values that carry no text change, so
    // the step a restore puts back does not become a step of its own.
    LaunchedEffect(noteId) {
        snapshotFlow { editorState }.collect { undoHistory.record(it) }
    }
    val restoreFromHistory: (TextFieldValue?) -> Unit = { restored ->
        if (restored != null) {
            HapticFeedback.light(context)
            editorState = restored
            shouldRequestEditorFocus = true
            // The autosave gate reads the live text when it fires, so the
            // restored version is what reaches the row — an undo the note is
            // not saved with would be no undo at all.
            if (isLoaded) saver.requestSave()
        }
    }
    val performUndo = { restoreFromHistory(undoHistory.undo()) }
    val performRedo = { restoreFromHistory(undoHistory.redo()) }
    // Every edit the app makes on the user's behalf — a formatting action, a
    // completion, a replace, a checkbox tap — goes through here rather than
    // assigning [editorState] directly. From the value stream alone a two-
    // character insertion is indistinguishable from two keystrokes, so without
    // the marker the next keystroke merges into the action and one undo takes
    // back both (#360).
    val applyEdit: (TextFieldValue) -> Unit = { next ->
        undoHistory.beginNewStep()
        editorState = next
    }
    val editorFocusRequester = remember(noteId) { FocusRequester() }
    var isPreviewMode by remember(noteId) { mutableStateOf(false) }
    // True when the settings read timed out and the note opened on defaults.
    // The position recorder consults it — see [recordsPosition] (#204).
    var openedOnFallbackSettings by remember(noteId) { mutableStateOf(false) }
    var isFocusMode by remember(noteId) { mutableStateOf(false) }
    var isFormattingExpanded by remember(noteId) { mutableStateOf(false) }
    var showDeleteConfirm by remember(noteId) { mutableStateOf(false) }

    val previewListState = rememberLazyListState()
    var showInfo by remember(noteId) { mutableStateOf(false) }
    var showOutline by remember(noteId) { mutableStateOf(false) }
    var pendingPreviewScroll by remember(noteId) { mutableStateOf<PreviewScrollRequest?>(null) }
    val shouldPreparePreview = isPreviewMode || showOutline
    val previewLines = remember(editorState.text, shouldPreparePreview) {
        if (shouldPreparePreview) SimpleMarkdownPreview.parse(editorState.text) else emptyList()
    }
    // `<details>` sections toggled away from their parsed default (#403) —
    // see `visiblePreviewLines`'s own doc for why this is a delta rather than
    // the absolute collapsed set. Reset per note like the rest of this
    // screen's transient UI state.
    var toggledSectionIds by remember(noteId) { mutableStateOf<Set<Int>>(emptySet()) }
    // collapsibleId is assigned by a section's position among every
    // <details> in the note, reassigned from zero on every reparse — so
    // inserting, removing, or reordering a section above an already-toggled
    // one reassigns its neighbours' ids out from under the toggle set,
    // silently applying the user's earlier tap to the wrong section (a Codex
    // review finding). There is no id here stable across an edit that adds or
    // removes a section, so this detects the next best thing — the ordered
    // list of (summary text, parsed-open-default) pairs actually changing —
    // and forgets stale toggles rather than risk misattributing one. The
    // default is part of the signature, not just the text, because hand-
    // editing a `<details>` tag's `open` attribute without touching its
    // `<summary>` is exactly the kind of edit that would otherwise slip past
    // a text-only comparison and flip the wrong section (a second self-review
    // finding). Compared only while preview is actually prepared:
    // previewLines itself goes empty while editing (shouldPreparePreview is
    // false then), and that transient emptiness must not read as "every
    // section just disappeared". Two sections sharing both an identical
    // title and default state is the one case this still cannot tell apart
    // from a no-op edit — accepted as a narrow, self-correcting residue (a
    // stray toggle there costs one extra tap, not data).
    val currentSummarySignature = remember(previewLines) {
        previewLines
            .filter { it.type == PreviewLineType.COLLAPSIBLE_SUMMARY }
            .map { it.text to it.extra }
    }
    var lastSeenSummarySignature by remember(noteId) { mutableStateOf<List<Pair<String, String?>>?>(null) }
    if (shouldPreparePreview && currentSummarySignature != lastSeenSummarySignature) {
        if (lastSeenSummarySignature != null) toggledSectionIds = emptySet()
        lastSeenSummarySignature = currentSummarySignature
    }
    // What is actually on screen with that toggle state applied. The outline
    // and the jump-to-end button both compute a LazyColumn item index, and
    // MarkdownPreviewList lays out this same filtered list (recomputed there
    // from the same inputs) — a heading or the note's last row inside a
    // currently-collapsed section is not a jump target until it is expanded.
    val visibleLines = remember(previewLines, toggledSectionIds) {
        visiblePreviewLines(previewLines, toggledSectionIds)
    }
    val tocHeadings = remember(visibleLines) { extractHeadings(visibleLines) }
    // Which line becomes the title is a user setting (#280); it keys every
    // derivation below so flipping it re-titles the open note straight away.
    val currentTitle = remember(editorState.text, noteId, titleSource) {
        if (noteId == null) "" else TitleExtractor.extractTitle(editorState.text, titleSource)
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
    val allTags by tagRepo.observeVisibleTags().collectAsState(initial = emptyList())
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
                    applyEdit(
                        editorState.copy(
                            text = updatedText,
                            selection = TextRange(cursor + insertion.length)
                        )
                    )
                    saver.requestSave()
                } else {
                    Toast.makeText(context, R.string.attachment_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    // Single dispatch path for every formatting action, whether it arrives from
    // a panel tap, a selection action, or a hardware-keyboard shortcut. Keeping
    // one handler means haptics, focus restore, and autosave cannot diverge
    // between the touch and keyboard paths.
    val applyFormattingAction: (EditorFormattingAction) -> Unit = { action ->
        HapticFeedback.light(context)
        when (val result = action.applyTo(editorState)) {
            is EditorFormattingResult.Edited -> {
                applyEdit(result.value)
                shouldRequestEditorFocus = true
                if (isLoaded) saver.requestSave()
            }
            EditorFormattingResult.PickImage -> {
                imagePickerLauncher.launch(arrayOf("image/*"))
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
            undoHistory.reset(editorState)
            isLoaded = true
        } else {
            // Read the persisted setting directly (not the collectAsState
            // snapshot, which starts on the default before DataStore emits) so
            // an existing note honours "open notes in preview" on its very first
            // frame instead of flashing edit (#200). New notes stay in edit.
            //
            // Bounded, because this suspends before the note is shown at all: a
            // DataStore read that never returns would hang note-open with no
            // way out, and the whole point of reading it here is a detail of
            // which mode the note opens in (#204). Falling back to the defaults
            // opens in edit at the top, which is the safe answer — it shows the
            // note and puts the caret somewhere harmless.
            val readSettings =
                withTimeoutOrNull(SETTINGS_READ_TIMEOUT_MS) { settingsRepository.settings.first() }
            openedOnFallbackSettings = readSettings == null
            val persistedSettings = readSettings ?: AppSettings()
            val openInPreview = persistedSettings.openNotesInPreview
            val loadedNote = repo.getNote(noteId)
            val content = loadedNote?.contentMarkdown.orEmpty()
            openedContent = loadedNote?.contentMarkdown
            openedUpdatedAt = loadedNote?.updatedAt
            // Where the note opens (#214). Read from the same persisted
            // snapshot as the preview setting above, for the same reason: the
            // collected state starts on the default, so using it here would
            // land at the top and then jump.
            val lastPosition = if (persistedSettings.openNotesAt == OpenNotesAt.LAST_POSITION) {
                viewStateDao.getForNote(noteId)
            } else {
                null
            }
            lastCaretOffset = lastPosition?.caretOffset
            lastPreviewIndex = lastPosition?.previewIndex
            val caret = when (persistedSettings.openNotesAt) {
                OpenNotesAt.TOP -> 0
                OpenNotesAt.BOTTOM -> content.length
                OpenNotesAt.LAST_POSITION -> {
                    val (resolved, status) = resolveRestoredCaret(
                        lastPosition?.caretOffset, content.length
                    )
                    // A dropped or clamped restore is silent to the user but
                    // explains "it forgot where I was" — leave a debug
                    // breadcrumb, like the reopen-last-note guard (#195).
                    if (BuildConfig.DEBUG && status != RestoreStatus.OK) {
                        Log.d(
                            "EditorScreen",
                            "position restore $status for note $noteId" +
                                (if (status == RestoreStatus.CLAMPED)
                                    ": saved ${lastPosition?.caretOffset} but note is ${content.length} chars"
                                else "")
                        )
                    }
                    resolved
                }
            }
            // Clamped, never trusted: the note can be shorter than when the
            // position was recorded — edited in another app, or a smaller
            // version brought in by sync.
            editorState = TextFieldValue(content, TextRange(caret.coerceIn(0, content.length)))
            // The loaded note is the floor: undo must not walk back past it
            // into the empty text the field held while the row was being read.
            undoHistory.reset(editorState)
            pendingPreviewScroll = when (persistedSettings.openNotesAt) {
                OpenNotesAt.TOP -> null
                // Clamped against the rendered list when the scroll runs, so
                // "as far as it goes" is all this has to say.
                OpenNotesAt.BOTTOM -> PreviewScrollRequest(Int.MAX_VALUE, animate = false)
                OpenNotesAt.LAST_POSITION ->
                    lastPosition?.let {
                        PreviewScrollRequest(it.previewIndex, animate = false, restore = true)
                    }
            }
            isPreviewMode = opensInPreview(openInPreview, content)
            shouldRequestEditorFocus = content.isEmpty()
            isLoaded = true
            // Remember this note as the launch target for the opt-in
            // "Reopen last note on launch" setting (#192) — but only when the
            // note actually exists. A stale deep link must not overwrite a
            // valid last-note id with a dangling one (#195).
            if (loadedNote != null) {
                settingsRepository.setLastOpenedNoteId(noteId)
            }
        }
    }

    LaunchedEffect(noteId, isLoaded) {
        if (noteId != null && isLoaded) saver.run()
    }

    LaunchedEffect(shouldRequestEditorFocus, isLoaded, isPreviewMode) {
        if (shouldRequestEditorFocus && isLoaded && !isPreviewMode) {
            withFrameNanos { }
            editorFocusRequester.requestFocus()
            shouldRequestEditorFocus = false
        }
    }

    // Preview scrolls that have to wait for the preview to exist: the outline's
    // jump-to-heading, and restoring where the note was left (#214). Held until
    // the list actually has rows, and clamped against them — the request can name
    // a block that a shorter note no longer has. Restores are instant; a jump the
    // user asked for animates, so it reads as movement rather than a cut.
    LaunchedEffect(isPreviewMode, visibleLines.size, pendingPreviewScroll) {
        val request = pendingPreviewScroll ?: return@LaunchedEffect
        if (!isPreviewMode || visibleLines.isEmpty()) return@LaunchedEffect
        withFrameNanos { }
        val (target, status) = resolveRestoredPreviewIndex(request.index, visibleLines.lastIndex)
        // Only a restore reports. `BOTTOM` asks for `Int.MAX_VALUE` on purpose
        // and the outline names a row it just read off this same list, so
        // neither clamp means anything went missing — but a note that opens
        // straight into preview restores here rather than through the caret,
        // and that is the case the caret breadcrumb cannot see (#262). A
        // collapsed `<details>` section also clamps here now: the saved index
        // pointed at a row that a shorter, collapsed rendering no longer has,
        // which reads the same as the note itself having gotten shorter.
        if (BuildConfig.DEBUG && request.restore && status != RestoreStatus.OK) {
            Log.d(
                "EditorScreen",
                "preview position restore $status for note $noteId: saved block " +
                    "${request.index} but the note renders ${visibleLines.size}"
            )
        }
        if (request.animate) {
            previewListState.animateScrollToItem(target)
        } else {
            previewListState.scrollToItem(target)
        }
        pendingPreviewScroll = null
    }

    // Record where the note was left, for "Open notes at: where I left off".
    // Only while that value is selected — with the setting off, Markleaf keeps
    // no record of where anyone was. Debounced, so typing a paragraph is one
    // write at the end of it rather than one per keystroke, and written while
    // the screen is still composed so it survives the process being killed
    // rather than depending on a tidy exit.
    LaunchedEffect(noteId, isLoaded, appSettings.openNotesAt, openedOnFallbackSettings) {
        if (noteId == null || !isLoaded) return@LaunchedEffect
        if (!recordsPosition(appSettings.openNotesAt, openedOnFallbackSettings)) return@LaunchedEffect
        snapshotFlow {
            Triple(
                isPreviewMode,
                editorState.selection.start,
                previewListState.firstVisibleItemIndex
            )
        }
            .distinctUntilChanged()
            // collectLatest + delay rather than debounce(): the operator is
            // still a preview API, and this is the same behaviour — a new
            // position cancels the pending write and restarts the wait.
            .collectLatest { (inPreview, caretOffset, previewIndex) ->
                delay(POSITION_WRITE_DEBOUNCE_MS)
                // Only the surface on screen knows anything. While you are
                // editing, the preview list is not composed and reports 0;
                // while you are reading, the caret has not moved since you
                // left the editor. Writing both would have each surface blank
                // the other's position every time you switched.
                val effectiveCaret = if (inPreview) lastCaretOffset ?: caretOffset else caretOffset
                val effectiveIndex = if (inPreview) previewIndex else lastPreviewIndex ?: previewIndex
                lastCaretOffset = effectiveCaret
                lastPreviewIndex = effectiveIndex
                viewStateDao.upsert(
                    NoteViewStateEntity(
                        noteId = noteId,
                        caretOffset = effectiveCaret,
                        previewIndex = effectiveIndex
                    )
                )
            }
    }

    // What takes the row above the keyboard away altogether: another surface is
    // in front of it, or something else is already occupying that slot.
    val isEditorRowPreempted =
        isFocusMode ||
            isPreviewMode ||
            showInfo ||
            showOutline ||
            overflowExpanded ||
            showDeleteConfirm ||
            imageAltEditing != null ||
            (quickInsertQuery != null && quickInsertItems.isNotEmpty()) ||
            (wikilinkQuery != null && wikilinkSuggestions.isNotEmpty()) ||
            (tagQuery != null && tagSuggestions.isNotEmpty())
    // The find bar stands the formatting controls down but not undo: while it is
    // open the editor's selection is the current match rather than something the
    // user chose, so bold / italic / link would be aimed at the wrong thing —
    // and a replace-all is exactly the change you may want back (#360).
    val isFormattingPreempted = isEditorRowPreempted || isFindOpen
    // With "Show formatting button" off, the row is only worth mounting while
    // text is selected (#331). The standing `Aa` handle is what the setting
    // removes; bold / italic / link stay, because selecting is how you ask for
    // them. Unmounting rather than hiding keeps the row from leaving an empty
    // 48dp strip above the keyboard, which is the whole complaint.
    val isFormattingIdle =
        !appSettings.showFormattingButton && editorState.selection.collapsed
    val showsFormattingEntry = !isFormattingPreempted && !isFormattingIdle
    val showsSelectionActions = !isFormattingPreempted && !editorState.selection.collapsed
    LaunchedEffect(isFormattingPreempted, isFormattingIdle) {
        if (isFormattingPreempted || isFormattingIdle) isFormattingExpanded = false
    }
    BackHandler(enabled = isFormattingExpanded) {
        isFormattingExpanded = false
        shouldRequestEditorFocus = true
    }

    // The outline is a screen, not a sheet: it takes over the bar and the body
    // rather than floating above them, so a long one gets the full height (#215).
    BackHandler(enabled = showOutline) { showOutline = false }

    // Where a tapped outline entry lands. In preview the rendered index is what
    // scrolls the list. While editing it has to move the caret instead —
    // flipping the note into preview to answer "take me to this section" threw
    // away the edit the user was in the middle of. Setting the selection is the
    // whole mechanism; the text field brings the caret into view on its own,
    // which is what the find bar already relies on.
    //
    // A heading the parser could not attribute to a line, or one whose line has
    // since gone, falls back to the preview jump rather than putting the caret
    // somewhere the user did not point at.
    val jumpToHeading: (TocHeading) -> Unit = { heading ->
        val offset = heading.sourceLine
            ?.let { MarkdownEditActions.offsetOfLine(editorState.text, it) }
        if (isPreviewMode || offset == null) {
            pendingPreviewScroll = PreviewScrollRequest(heading.index, animate = true)
            if (!isPreviewMode) isPreviewMode = true
        } else {
            editorState = editorState.copy(selection = TextRange(offset))
            shouldRequestEditorFocus = true
        }
    }

    Scaffold(
        topBar = topBar@{
            if (showOutline) {
                NoteOutlineTopBar(onClose = { showOutline = false })
                return@topBar
            }
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
                isViewModeLocked = appSettings.openNotesInPreview,
                onToggleLock = {
                    // Long-press flips the persistent lock and mirrors it onto
                    // this note straight away: locking jumps to preview and makes
                    // it stick across notes; unlocking drops back to edit (#200).
                    val nowLocked = !appSettings.openNotesInPreview
                    coroutineScope.launch {
                        settingsRepository.setOpenNotesInPreview(nowLocked)
                    }
                    isPreviewMode = nowLocked
                    if (!nowLocked) shouldRequestEditorFocus = true
                    HapticFeedback.light(context)
                    Toast.makeText(
                        context,
                        if (nowLocked) R.string.view_mode_locked else R.string.view_mode_unlocked,
                        Toast.LENGTH_SHORT
                    ).show()
                },
                onExitFocusMode = { isFocusMode = false },
                onOpenOutline = {
                    overflowExpanded = false
                    showOutline = true
                },
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
                                        title = TitleExtractor.extractTitle(editorState.text, titleSource),
                                        contentMarkdown = editorState.text,
                                        excerpt = TitleExtractor.generateExcerpt(editorState.text, titleSource)
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
                                        title = TitleExtractor.extractTitle(editorState.text, titleSource),
                                        contentMarkdown = editorState.text,
                                        excerpt = TitleExtractor.generateExcerpt(editorState.text, titleSource)
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
                                        title = TitleExtractor.extractTitle(editorState.text, titleSource),
                                        contentMarkdown = editorState.text,
                                        excerpt = TitleExtractor.generateExcerpt(editorState.text, titleSource)
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
        if (showOutline) {
            NoteOutlineContent(
                headings = tocHeadings,
                onHeadingClick = { heading ->
                    showOutline = false
                    jumpToHeading(heading)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
            return@Scaffold
        }
        Crossfade(
            targetState = isPreviewMode,
            label = "Editor preview mode"
        ) { previewMode ->
            if (previewMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    MarkdownPreviewList(
                        lines = previewLines,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                        listState = previewListState,
                        // Tapping a checkbox in the preview flips it in the
                        // source (#219). The preview is rebuilt from the text,
                        // so the row redraws on its own; a null result means the
                        // line stopped being a task item since this preview was
                        // built, and the tap is dropped rather than guessed at.
                        onToggleTask = { sourceLine ->
                            MarkdownEditActions.toggleTaskAtLine(editorState.text, sourceLine)
                                ?.let { updated ->
                                    applyEdit(editorState.copy(text = updated))
                                    if (isLoaded) saver.requestSave()
                                }
                        },
                        onWikilinkClick = { title ->
                            coroutineScope.launch {
                                val localName = LocalMarkdownLink.fileName(title)
                                val mirroredId = if (localName != null) {
                                    appSettings.syncFolderUriOrNull()?.let { uri ->
                                        withContext(Dispatchers.IO) {
                                            NoteFolderMirror.noteIdForFileName(
                                                context, uri, localName, appSettings.mirrorMetadata()
                                            )
                                        }
                                    }
                                } else null
                                val existing = mirroredId?.let { db.noteDao().getNoteById(it) }
                                    ?: db.noteDao().getNoteByTitle(title)
                                if (existing != null) {
                                    if (existing.locked) {
                                        Toast.makeText(context, R.string.wikilink_target_locked, Toast.LENGTH_SHORT).show()
                                    } else if (!existing.trashed && !existing.archived) {
                                        onNavigateToNote(existing.id)
                                    } else {
                                        Toast.makeText(context, R.string.quick_switcher_no_results, Toast.LENGTH_SHORT).show()
                                    }
                                } else if (db.noteDao().countLockedNotesWithTitle(title) > 0) {
                                    // The note exists but lives in the Locked space.
                                    // Opening it here would bypass the passcode, and
                                    // creating a new one would leave two notes sharing
                                    // a title, so say where it is instead (#156). The
                                    // title is already visible in this note's body, so
                                    // naming its whereabouts leaks nothing new.
                                    Toast.makeText(
                                        context,
                                        R.string.wikilink_target_locked,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else if (localName != null) {
                                    Toast.makeText(context, R.string.quick_switcher_no_results, Toast.LENGTH_SHORT).show()
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
                                    onNavigateToNote(newNote.id)
                                }
                            }
                        },
                        onLocalLinkClick = { fileName ->
                            coroutineScope.launch {
                                val folder = appSettings.syncFolderUriOrNull()
                                val linkedId = folder?.let { uri ->
                                    withContext(Dispatchers.IO) {
                                        NoteFolderMirror.noteIdForFileName(
                                            context, uri, fileName, appSettings.mirrorMetadata()
                                        )
                                    }
                                }
                                val target = linkedId?.let { db.noteDao().getNoteById(it) }
                                when {
                                    target == null || target.trashed || target.archived ->
                                        Toast.makeText(context, R.string.quick_switcher_no_results, Toast.LENGTH_SHORT).show()
                                    target.locked ->
                                        Toast.makeText(context, R.string.wikilink_target_locked, Toast.LENGTH_SHORT).show()
                                    else -> onNavigateToNote(target.id)
                                }
                            }
                        },
                        onImageLongPress = { path, currentAlt ->
                            imageAltEditing = path to currentAlt
                        },
                        // Same scale as the editor (#346): the preview is the
                        // rendered form of the same text, and growing it grows
                        // the checkbox glyphs and link tap targets too.
                        fontScale = appSettings.editorFontSize.scale,
                        toggledSectionIds = toggledSectionIds,
                        onToggleSection = { id ->
                            toggledSectionIds = if (id in toggledSectionIds) {
                                toggledSectionIds - id
                            } else {
                                toggledSectionIds + id
                            }
                        }
                    )

                    // The same jump control as the editor (#214). Here the
                    // scroll position is known outright, so the direction is
                    // the honest one rather than inferred from a caret. Uses
                    // visibleLines, not previewLines: the LazyColumn this
                    // scrolls is the one MarkdownPreviewList actually laid
                    // out, which is shorter whenever a section is collapsed.
                    if (isLongEnoughToJump(editorState.text) && visibleLines.isNotEmpty()) {
                        val toBottom =
                            previewListState.firstVisibleItemIndex < visibleLines.size / 2
                        JumpToEndButton(
                            jumpsToBottom = toBottom,
                            onClick = {
                                coroutineScope.launch {
                                    previewListState.animateScrollToItem(
                                        if (toBottom) visibleLines.lastIndex else 0
                                    )
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 20.dp, bottom = 20.dp)
                        )
                    }
                }
            } else {
                // imePadding() shrinks the editor body when the soft keyboard is up so
                // BasicTextField's built-in cursor bring-into-view can keep the caret
                // above the keyboard. Without it, enableEdgeToEdge() lets the IME draw
                // over the last lines and they stay hidden (#136).
                //
                // consumeWindowInsets(paddingValues) is what stops that from becoming a
                // gap (#398). Insets are distances from the window edge, so two bottom
                // insets overlap rather than stack — which is why the framework's own
                // safeDrawing unions systemBars with ime and takes the *larger* of the
                // two. Scaffold hands us the navigation-bar inset in paddingValues but
                // does not mark it consumed, and plain Modifier.padding does not mark
                // it either, so imePadding() below would add the whole IME height on
                // top of it: navBar + ime instead of max(navBar, ime), leaving exactly
                // one navigation bar of dead space above the keyboard. Consuming it
                // here means imePadding() adds only (ime - navBar), so the sum is the
                // max the framework intends. With the keyboard down ime is 0 and the
                // navigation-bar padding from paddingValues stands on its own.
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .consumeWindowInsets(paddingValues)
                        .imePadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        // Undo is bound here rather than on the text field so it
                        // works wherever focus is inside the editor body — the
                        // find and replace fields included. Undoing a
                        // replace-all is the case that needs it most, and that
                        // is exactly when the Replace All button still has
                        // focus (#360). Nothing below this claims Ctrl+Z, so
                        // taking it on the preview pass costs nothing.
                        .onPreviewKeyEvent { event ->
                            val shortcut =
                                if (event.type == KeyEventType.KeyDown) event.toUndoAction() else null
                            when (shortcut) {
                                UndoAction.UNDO -> performUndo()
                                UndoAction.REDO -> performRedo()
                                null -> {}
                            }
                            shortcut != null
                        }
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
                                    applyEdit(replaceRange(editorState, target, replaceQuery))
                                    shouldRequestEditorFocus = true
                                    if (isLoaded) saver.requestSave()
                                }
                            },
                            onReplaceAll = {
                                if (findMatches.isNotEmpty()) {
                                    val count = findMatches.size
                                    applyEdit(replaceAllRanges(editorState, findMatches, replaceQuery))
                                    shouldRequestEditorFocus = true
                                    if (isLoaded) saver.requestSave()
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
                    val editorFontScale = appSettings.editorFontSize.scale
                    val markdownSyntaxVisible =
                        appSettings.markdownSyntaxVisibility == MarkdownSyntaxVisibility.SHOW && !isFocusMode
                    // Stabilise the transformation instance: without `remember` a
                    // fresh MarkdownSyntaxVisualTransformation is allocated on every
                    // recomposition (i.e. every keystroke), which forces Compose to
                    // re-run the syntax filter over the whole document each time.
                    // Re-key only when the colours, visibility, or text scale
                    // actually change.
                    val markdownVisualTransformation = remember(colorScheme, markdownSyntaxVisible, editorFontScale) {
                        if (markdownSyntaxVisible) {
                            MarkdownSyntaxVisualTransformation(markdownSyntaxColors(colorScheme), editorFontScale)
                        } else {
                            VisualTransformation.None
                        }
                    }
                    val onQuickInsertPick: (QuickInsertCommand) -> Unit = pick@{ command ->
                        val query = detectQuickInsertQuery(editorState) ?: return@pick
                        HapticFeedback.light(context)
                        applyEdit(applyQuickInsertCommand(editorState, query, command))
                        quickInsertSelectedIndex = 0
                        shouldRequestEditorFocus = true
                        if (isLoaded) saver.requestSave()
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
                                if (isLoaded) saver.requestSave()
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
                                            applyEdit(
                                                if (event.isShiftPressed) {
                                                    MarkdownEditActions.outdent(editorState)
                                                } else {
                                                    MarkdownEditActions.indent(editorState)
                                                }
                                            )
                                            if (isLoaded) saver.requestSave()
                                            true
                                        } else {
                                            // Hardware-keyboard formatting shortcuts resolve through
                                            // the shared keymap in EditorFormattingAction.kt, the same
                                            // one the expanded panel uses.
                                            val shortcut = event.toFormattingAction()
                                            if (shortcut != null) {
                                                applyFormattingAction(shortcut)
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    }
                                },
                            // Body text honours the text-size setting (#346);
                            // at scale 1f this is the untouched theme style.
                            textStyle = run {
                                val base = MaterialTheme.typography.bodyLarge
                                (if (editorFontScale == 1f) base else base.copy(
                                    fontSize = base.fontSize * editorFontScale,
                                    lineHeight = base.lineHeight * editorFontScale
                                )).copy(color = MaterialTheme.colorScheme.onBackground)
                            },
                            visualTransformation = markdownVisualTransformation,
                            // `BasicTextField` defaults its caret to opaque black, and
                            // this is the only field in the app that draws its own —
                            // every Material text field takes the colour from the theme.
                            // On a dark background the default was all but invisible
                            // while the drag handle beside it was themed, in both
                            // Markleaf Green and Material You (#283). `primary` is what
                            // the Material fields use, so the caret now matches them.
                            cursorBrush = SolidColor(colorScheme.primary),
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

                        // Jump to the far end of a long note (#214). Which end
                        // that is follows the caret: after a jump the caret is
                        // at the other end, so the button flips and the same
                        // spot takes you back. Hidden in focus mode, which
                        // exists to remove exactly this kind of furniture.
                        if (!isFocusMode && isLongEnoughToJump(editorState.text)) {
                            val toBottom =
                                editorState.selection.start < editorState.text.length / 2
                            JumpToEndButton(
                                jumpsToBottom = toBottom,
                                onClick = {
                                    editorState = editorState.copy(
                                        selection = TextRange(
                                            if (toBottom) editorState.text.length else 0
                                        )
                                    )
                                    shouldRequestEditorFocus = true
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 8.dp)
                            )
                        }
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
                                applyEdit(completeWikilink(editorState, title))
                                shouldRequestEditorFocus = true
                                if (isLoaded) saver.requestSave()
                            }
                        )
                    } else if (tagQuery != null && tagSuggestions.isNotEmpty() && !isFocusMode) {
                        TagSuggestionsRow(
                            suggestions = tagSuggestions,
                            onPick = { tag ->
                                applyEdit(completeTag(editorState, tag))
                                shouldRequestEditorFocus = true
                                if (isLoaded) saver.requestSave()
                            }
                        )
                    }

                    // The row now also carries undo, so "nothing to format" is
                    // no longer the only thing it could be showing: with the
                    // formatting button off, or with the find bar standing the
                    // formatting controls down, it still mounts once there is a
                    // step to go back to (#360).
                    val undoAvailable = undoHistory.canUndo || undoHistory.canRedo
                    if (
                        !isEditorRowPreempted &&
                        (showsFormattingEntry || showsSelectionActions || undoAvailable)
                    ) {
                        EditorFormattingControls(
                            state = EditorFormattingUiState(
                                selectionActive = showsSelectionActions,
                                expanded = isFormattingExpanded,
                                enabled = isLoaded,
                                canUndo = undoHistory.canUndo,
                                canRedo = undoHistory.canRedo,
                                showFormattingEntry = showsFormattingEntry
                            ),
                            onExpandedChange = { expanded ->
                                isFormattingExpanded = expanded
                                if (!expanded) shouldRequestEditorFocus = true
                            },
                            onAction = applyFormattingAction,
                            backgroundColor = MaterialTheme.colorScheme.background,
                            onUndo = performUndo,
                            onRedo = performRedo
                        )
                    }
                }
            }
        }
    }

    imageAltEditing?.let { (path, currentAlt) ->
        ImageAltTextDialog(
            path = path,
            currentAlt = currentAlt,
            onConfirm = { newAlt ->
                applyEdit(replaceImageAlt(editorState, path, newAlt))
                if (isLoaded) saver.requestSave()
                imageAltEditing = null
            },
            onDismiss = { imageAltEditing = null }
        )
    }

    if (showDeleteConfirm && noteId != null) {
        DeleteConfirmDialog(
            onConfirm = {
                showDeleteConfirm = false
                coroutineScope.launch {
                    repo.moveToTrash(noteId)
                    onBack()
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    if (showInfo) {
        EditorInfoSheet(
            state = EditorInfoUiState(
                statsText = editorStatsText,
                backlinks = backlinks
            ),
            onBacklinkClick = { id ->
                showInfo = false
                onNavigateToNote(id)
            },
            onDismiss = { showInfo = false }
        )
    }
}
