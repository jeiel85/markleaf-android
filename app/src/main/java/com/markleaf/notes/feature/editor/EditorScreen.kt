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
import com.markleaf.notes.core.markdown.SimpleMarkdownPreview
import com.markleaf.notes.core.markdown.markdownSyntaxColors
import com.markleaf.notes.core.markdown.preview.MarkdownPreviewList
import com.markleaf.notes.core.markdown.preview.TocHeading
import com.markleaf.notes.core.markdown.preview.extractHeadings
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
import com.markleaf.notes.util.ExportPdf
import com.markleaf.notes.util.ShareNoteUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    settingsRepository: AppSettingsRepository = rememberAppSettingsRepository()
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
    val titleSource = appSettings.noteTitleSource
    // The persistence behind the debounce gate: reads the note fresh from the
    // DB so a save never clobbers a newer row, then updates the sync mirror
    // when one is configured (#262).
    suspend fun autosave(content: String) {
        val id = noteId ?: return
        val currentNote = repo.getNote(id)
        if (currentNote != null) {
            val updatedNote = currentNote.copy(
                title = TitleExtractor.extractTitle(content, appSettings.noteTitleSource),
                contentMarkdown = content,
                excerpt = TitleExtractor.generateExcerpt(content, appSettings.noteTitleSource),
                updatedAt = java.time.Instant.now()
            )
            repo.updateNote(updatedNote)
            tagRepo.reindexTagsForNote(id, content)
            linkRepo.reindexLinksForNote(id, content)
            appSettings.syncFolderUriOrNull()?.let { uri ->
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
    var shouldRequestEditorFocus by remember(noteId) { mutableStateOf(noteId == null) }
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
    val tocHeadings = remember(previewLines) { extractHeadings(previewLines) }
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
                    editorState = editorState.copy(
                        text = updatedText,
                        selection = TextRange(cursor + insertion.length)
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
                editorState = result.value
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
    LaunchedEffect(isPreviewMode, previewLines.size, pendingPreviewScroll) {
        val request = pendingPreviewScroll ?: return@LaunchedEffect
        if (!isPreviewMode || previewLines.isEmpty()) return@LaunchedEffect
        withFrameNanos { }
        val (target, status) = resolveRestoredPreviewIndex(request.index, previewLines.lastIndex)
        // Only a restore reports. `BOTTOM` asks for `Int.MAX_VALUE` on purpose
        // and the outline names a row it just read off this same list, so
        // neither clamp means anything went missing — but a note that opens
        // straight into preview restores here rather than through the caret,
        // and that is the case the caret breadcrumb cannot see (#262).
        if (BuildConfig.DEBUG && request.restore && status != RestoreStatus.OK) {
            Log.d(
                "EditorScreen",
                "preview position restore $status for note $noteId: saved block " +
                    "${request.index} but the note renders ${previewLines.size}"
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

    val isFormattingPreempted =
        isFindOpen ||
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
    // With "Show formatting button" off, the row is only worth mounting while
    // text is selected (#331). The standing `Aa` handle is what the setting
    // removes; bold / italic / link stay, because selecting is how you ask for
    // them. Unmounting rather than hiding keeps the row from leaving an empty
    // 48dp strip above the keyboard, which is the whole complaint.
    val isFormattingIdle =
        !appSettings.showFormattingButton && editorState.selection.collapsed
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
                                    editorState = editorState.copy(text = updated)
                                    if (isLoaded) saver.requestSave()
                                }
                        },
                        onWikilinkClick = { title ->
                            coroutineScope.launch {
                                val existing = db.noteDao().getNoteByTitle(title)
                                if (existing != null) {
                                    onNavigateToNote(existing.id)
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
                        onImageLongPress = { path, currentAlt ->
                            imageAltEditing = path to currentAlt
                        }
                    )

                    // The same jump control as the editor (#214). Here the
                    // scroll position is known outright, so the direction is
                    // the honest one rather than inferred from a caret.
                    if (isLongEnoughToJump(editorState.text) && previewLines.isNotEmpty()) {
                        val toBottom =
                            previewListState.firstVisibleItemIndex < previewLines.size / 2
                        JumpToEndButton(
                            jumpsToBottom = toBottom,
                            onClick = {
                                coroutineScope.launch {
                                    previewListState.animateScrollToItem(
                                        if (toBottom) previewLines.lastIndex else 0
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
                                    if (isLoaded) saver.requestSave()
                                }
                            },
                            onReplaceAll = {
                                if (findMatches.isNotEmpty()) {
                                    val count = findMatches.size
                                    editorState = replaceAllRanges(editorState, findMatches, replaceQuery)
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
                    val markdownSyntaxVisible =
                        appSettings.markdownSyntaxVisibility == MarkdownSyntaxVisibility.SHOW && !isFocusMode
                    // Stabilise the transformation instance: without `remember` a
                    // fresh MarkdownSyntaxVisualTransformation is allocated on every
                    // recomposition (i.e. every keystroke), which forces Compose to
                    // re-run the syntax filter over the whole document each time.
                    // Re-key only when the colours or visibility actually change.
                    val markdownVisualTransformation = remember(colorScheme, markdownSyntaxVisible) {
                        if (markdownSyntaxVisible) {
                            MarkdownSyntaxVisualTransformation(markdownSyntaxColors(colorScheme))
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
                                            editorState = if (event.isShiftPressed) {
                                                MarkdownEditActions.outdent(editorState)
                                            } else {
                                                MarkdownEditActions.indent(editorState)
                                            }
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
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onBackground
                            ),
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
                                editorState = completeWikilink(editorState, title)
                                shouldRequestEditorFocus = true
                                if (isLoaded) saver.requestSave()
                            }
                        )
                    } else if (tagQuery != null && tagSuggestions.isNotEmpty() && !isFocusMode) {
                        TagSuggestionsRow(
                            suggestions = tagSuggestions,
                            onPick = { tag ->
                                editorState = completeTag(editorState, tag)
                                shouldRequestEditorFocus = true
                                if (isLoaded) saver.requestSave()
                            }
                        )
                    }

                    if (!isFormattingPreempted && !isFormattingIdle) {
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
                            onAction = applyFormattingAction,
                            backgroundColor = MaterialTheme.colorScheme.background
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
                editorState = replaceImageAlt(editorState, path, newAlt)
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
