@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.markleaf.notes.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.markleaf.notes.BuildConfig
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.markleaf.notes.feature.archive.ArchiveScreen
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.feature.lock.LockedNotesScreen
import com.markleaf.notes.feature.notes.NotesListScreen
import com.markleaf.notes.feature.search.SearchScreen
import com.markleaf.notes.feature.settings.SettingsScreen
import com.markleaf.notes.feature.tags.TagRail
import com.markleaf.notes.feature.tags.TagsScreen
import com.markleaf.notes.feature.trash.TrashScreen
import com.markleaf.notes.feature.sync.SyncCenterScreen
import com.markleaf.notes.feature.viewer.FileViewerScreen
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.NotesLayout
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.sync.LocalNoteLinkResult
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.data.sync.resolveLocalNoteLink
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.ui.viewmodel.ArchiveViewModel
import com.markleaf.notes.ui.viewmodel.LockedNotesViewModel
import com.markleaf.notes.ui.viewmodel.NotesViewModel
import com.markleaf.notes.ui.viewmodel.SearchViewModel
import com.markleaf.notes.ui.viewmodel.TrashViewModel
import com.markleaf.notes.ui.viewmodel.SyncCenterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

// Scopes for the list-card -> editor shared-element (container transform). They
// are only non-null on the phone navigation path (provided around the NavHost
// and the NOTES destination); the tablet layout opens the editor in-pane with no
// AnimatedContent to morph through, so its rows read null and render normally.
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MarkleafNavHost(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    viewModelFactory: ViewModelProvider.Factory,
    shouldCreateNote: Boolean = false,
    /** The launcher's "Search" shortcut: start on the Search screen. */
    openSearch: Boolean = false,
    sharedText: String? = null,
    sharedCreatedAt: Instant? = null,
    sharedUpdatedAt: Instant? = null,
    openNoteId: String? = null,
    viewFileUri: String? = null,
    /**
     * False when an earlier instance of the activity has already run this launch
     * request — an entry intent, or the plain-launch "reopen last note" fallback —
     * and this one is only a recreation of it. Then nothing is dispatched: the
     * restored back stack is where the user is, and running the request again would
     * repeat it on top (a second note from a share, the last note pushed over the
     * file viewer). Defaults to true, a fresh launch.
     */
    dispatchLaunchRequest: Boolean = true,
    onEntryDispatched: () -> Unit = {}
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val context = LocalContext.current
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    // Only used to decide where an incoming open-note intent may land; see
    // resolveOpenNoteRoute. AppDatabase.getInstance is a singleton, so this
    // shares the instance MainActivity already built the factory from.
    val noteRepository = remember { LocalNoteRepository(AppDatabase.getInstance(context)) }
    // Outlives every individual destination, unlike a scope obtained inside a
    // `composable { }` route or inside EditorScreen itself -- see the
    // `hostScope` parameter on EditorScreen (#405) for why that matters.
    val hostScope = rememberCoroutineScope()

    // One-shot intent entry points — widget "new note", text or a file shared
    // into the app (#139), a file opened for reading (#326), and a widget tap on
    // a recent note. These must run EXACTLY ONCE per launch. They used to live
    // in LaunchedEffects inside the NOTES destination, but a navigation
    // destination re-enters composition every time it returns to the foreground
    // — e.g. pressing back from the editor — which re-ran the effect, created
    // another duplicate note, and immediately reopened the editor, trapping the
    // user in a reopen loop (#142). Hoisting them here ties them to the host,
    // which stays composed for the whole activity instance, so returning from
    // the editor no longer re-imports. A genuinely new intent arrives on a
    // fresh activity (onNewIntent → recreate) and re-composes the host, so new
    // shares/opens still import. The sources are mutually exclusive (each
    // derives from a single intent action), so a `when` handles at most one.
    // A recreated activity gets the same intent again, so MainActivity keeps a
    // saved-state flag that onEntryDispatched sets once the branch below has run;
    // the next instance is told not to dispatch at all (dispatchLaunchRequest).
    val intentEntryViewModel = viewModel<NotesViewModel>(factory = viewModelFactory)
    LaunchedEffect(Unit) {
        if (!dispatchLaunchRequest) return@LaunchedEffect
        when {
            shouldCreateNote -> {
                val newNote = intentEntryViewModel.createNote()
                navController.navigateOnMain(NavRoutes.editorRoute(newNote.id))
            }
            openSearch -> navController.navigateOnMain(NavRoutes.SEARCH)
            !viewFileUri.isNullOrBlank() -> {
                // A file tapped in a file manager opens for reading, not as a
                // new note (#326). Nothing is written until the reader asks for
                // it in the viewer.
                navController.navigateOnMain(NavRoutes.viewerRoute(viewFileUri))
            }
            !sharedText.isNullOrBlank() -> {
                // Read the title rule from the store rather than the collected
                // state: this effect runs on the first composition, where
                // `appSettings` is still the initial default (#280).
                val titleSource = settingsRepository.settings.first().noteTitleSource
                val newNote = intentEntryViewModel.createNote(
                    initialContent = sharedText,
                    titleSource = titleSource,
                    createdAt = sharedCreatedAt,
                    updatedAt = sharedUpdatedAt
                )
                navController.navigateOnMain(NavRoutes.editorRoute(newNote.id))
            }
            !openNoteId.isNullOrBlank() -> {
                // Not editorRoute directly: this is the one entry point an
                // external app can reach, and a locked id must land on the
                // passcode gate instead of the editor (#158).
                navController.navigateOnMain(resolveOpenNoteRoute(openNoteId, noteRepository))
            }
            else -> {
                // Opt-in "Reopen last note on launch" (#192): a plain launch —
                // no widget/share/open intent — lands straight in the note the
                // user last edited. One-shot read rather than the collected
                // state, which may still hold the initial defaults this early.
                // A stale id (note since deleted or trashed) falls back to the
                // list; a locked id goes through resolveOpenNoteRoute so it
                // lands on the passcode gate, never straight in the editor.
                val settings = settingsRepository.settings.first()
                val lastId = settings.lastOpenedNoteId
                if (settings.reopenLastNote && !lastId.isNullOrBlank()) {
                    val note = noteRepository.getNote(lastId)
                    if (note != null && !note.trashed) {
                        navController.navigateOnMain(resolveOpenNoteRoute(lastId, noteRepository))
                    } else {
                        // The note is gone (deleted or trashed since). Clear the
                        // dangling id so it isn't re-validated on every launch,
                        // and leave a debug breadcrumb — "the app stopped
                        // reopening my note" is undiagnosable without one (#195).
                        if (BuildConfig.DEBUG) {
                            Log.d(
                                "MarkleafNavHost",
                                "reopen-last-note skipped: note $lastId is " +
                                    if (note == null) "deleted" else "in the trash"
                            )
                        }
                        settingsRepository.setLastOpenedNoteId(null)
                    }
                }
            }
        }
        // Reached only if the branch above ran to the end: an effect cancelled part
        // way (the activity recreated mid-dispatch) leaves the request unacted on, so
        // the next instance runs it again rather than losing it.
        onEntryDispatched()
    }

    // Restrained shared-axis-X motion for forward/back navigation: the incoming
    // screen slides a fifth of the width and cross-fades. Because the manifest
    // opts into enableOnBackInvokedCallback and Navigation 2.8 makes the pop
    // transitions seekable, the system back gesture drives popEnter/popExit as a
    // predictive "peek" of the previous screen rather than an instant swap. The
    // tablet layout keeps the editor in-pane (no navigation) and never hits these.
    val navMotion = tween<Float>(durationMillis = 280)
    val navOffsetMotion = tween<IntOffset>(durationMillis = 280)
    // The note (and the screen it was tapped on) the editor is growing out of.
    // Set by the click handlers immediately before navigating and read by the
    // transition specs below and by the EDITOR destination. Saved across
    // recreation: it also decides whether the editor is composed inside the
    // container, and `rememberSaveable` finds the editor's state again only when
    // that is decided the same way after a rotation (#499).
    var noteOrigin by rememberNoteOrigin()
    // SharedTransitionLayout wraps the graph so a tapped note card can morph into
    // the editor (container transform). `this` is the SharedTransitionScope; it is
    // published via LocalSharedTransitionScope so the deeply-nested NoteRow (source)
    // and the EDITOR destination (target) can tag matching bounds.
    SharedTransitionLayout {
    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES,
        // When a navigation is the container hop, the slide/fade below must be
        // off: the shared bounds already carry the motion, and running both makes
        // the editor slide sideways while it also unfolds from the card.
        // Everything else — settings, tags, wikilink hops, intent opens — keeps it.
        enterTransition = {
            if (opensOrClosesNoteContainer(noteOrigin)) EnterTransition.None
            else slideInHorizontally(navOffsetMotion) { it / 5 } + fadeIn(navMotion)
        },
        exitTransition = {
            if (opensOrClosesNoteContainer(noteOrigin)) ExitTransition.None
            else slideOutHorizontally(navOffsetMotion) { -it / 5 } + fadeOut(navMotion)
        },
        popEnterTransition = {
            if (opensOrClosesNoteContainer(noteOrigin)) EnterTransition.None
            else slideInHorizontally(navOffsetMotion) { -it / 5 } + fadeIn(navMotion)
        },
        popExitTransition = {
            if (opensOrClosesNoteContainer(noteOrigin)) ExitTransition.None
            else slideOutHorizontally(navOffsetMotion) { it / 5 } + fadeOut(navMotion)
        }
    ) {
        composable(NavRoutes.NOTES) {
            val viewModel = viewModel<NotesViewModel>(factory = viewModelFactory)
            val coroutineScope = rememberCoroutineScope()
            // "Open file…" (#326). The picker's read grant lasts as long as this
            // task, which is exactly how long the viewer needs it — nothing is
            // persisted, so the app never accumulates access to files it is no
            // longer showing.
            val openFileLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) navController.navigate(NavRoutes.viewerRoute(uri.toString()))
            }
            val onOpenFile = { openFileLauncher.launch(OPEN_FILE_MIME_TYPES) }

            // Intent entry points (widget new-note, shared/opened content, widget
            // recent-note tap) are handled once at the host scope above, not here
            // — see the LaunchedEffect in MarkleafNavHost's body (#142).

            // The note open in the tablet's editor pane. Saved, and held outside the
            // layout branch below, because a plain `remember` inside it lost the note
            // three ways: a rotation, a trip to Settings and back, and a tablet turned
            // upright — which is narrower than the two-pane layout needs, so the
            // branch itself switched and took the state with it.
            var selectedNoteId by rememberSaveable { mutableStateOf<String?>(null) }
            // Keeping the id means it can outlive what made it valid to show:
            // - Leaving for Settings disposes the pane's editor, and an editor left
            //   blank deletes its note as it goes (#405); the id would reopen an
            //   editor on a missing row, where nothing typed is saved.
            // - The list pane's Lock and Trash act on the row beside the open
            //   editor. A locked note restored straight into the pane would skip the
            //   passcode gate that every other way in goes through.
            // So the selection is dropped whenever its row is gone, locked or
            // trashed — on return, or the moment the change lands.
            LaunchedEffect(selectedNoteId) {
                val id = selectedNoteId ?: return@LaunchedEffect
                viewModel.observeNote(id).first { !isPaneSelectable(it) }
                if (selectedNoteId == id) selectedNoteId = null
            }

            if (isExpanded) {
                var isNoteListCollapsed by rememberSaveable { mutableStateOf(false) }
                var isTagRailCollapsed by rememberSaveable { mutableStateOf(false) }
                val selectedTag by viewModel.selectedTag.collectAsState()
                val listPaneColor = MaterialTheme.colorScheme.surfaceVariant
                val listPaneContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                val editorPaneColor = MaterialTheme.colorScheme.background
                val dividerColor = MaterialTheme.colorScheme.outlineVariant

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(editorPaneColor)
                ) {
                    // Tag rail — the leading sidebar of the 3-column tablet layout
                    // (tags | note list | editor). Tapping a tag filters the note
                    // list pane in place via the shared NotesViewModel. Bear-style,
                    // it hides entirely to reclaim writing space; the note-list top
                    // bar then shows a ">" to bring it back.
                    if (!isTagRailCollapsed) {
                        Surface(
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                                .systemBarsPadding()
                                .consumeWindowInsets(WindowInsets.systemBars),
                            color = listPaneColor,
                            contentColor = listPaneContentColor
                        ) {
                            TagRail(
                                selectedTag = selectedTag,
                                onSelectTag = { viewModel.selectTag(it) },
                                onCollapse = { isTagRailCollapsed = true }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(dividerColor)
                        )
                    }
                    if (isNoteListCollapsed) {
                        CollapsedNoteListRail(onExpandClick = { isNoteListCollapsed = false })
                    } else {
                        // systemBarsPadding paints surfaceVariant only below the
                        // status bar (matching the collapsed rail fix in v1.4.2);
                        // consumeWindowInsets stops the nested Scaffold inside
                        // NotesListScreen from re-padding the same insets.
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .systemBarsPadding()
                                .consumeWindowInsets(WindowInsets.systemBars),
                            color = listPaneColor,
                            contentColor = listPaneContentColor
                        ) {
                            NotesListScreen(
                                viewModel = viewModel,
                                onNoteClick = { noteId -> selectedNoteId = noteId },
                                onFabClick = {
                                    coroutineScope.launch {
                                        val newNote = viewModel.createNote()
                                        selectedNoteId = newNote.id
                                    }
                                },
                                onSearchClick = { navController.navigate(NavRoutes.SEARCH) },
                                onTagsClick = { navController.navigate(NavRoutes.TAGS) },
                                onArchiveClick = { navController.navigate(NavRoutes.ARCHIVE) },
                                onLockedClick = { navController.navigate(NavRoutes.LOCKED) },
                                onTrashClick = { navController.navigate(NavRoutes.TRASH) },
                                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                                onOpenFileClick = onOpenFile,
                                lockPasscodeSet = appSettings.lockPasscodeSet,
                                onRequestSetPasscode = { navController.navigate(NavRoutes.SETTINGS) },
                                onCollapseClick = { isNoteListCollapsed = true },
                                onShowTagRail = if (isTagRailCollapsed) ({ isTagRailCollapsed = false }) else null,
                                selectedNoteId = selectedNoteId,
                                selectedTag = selectedTag,
                                containerColor = listPaneColor,
                                contentColor = listPaneContentColor
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(dividerColor)
                    )
                    Box(
                        modifier = Modifier
                            .weight(if (isNoteListCollapsed) 1f else 1.5f)
                            .fillMaxHeight()
                            .background(editorPaneColor),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Switching notes in the 3-pane swaps the editor content in
                        // place; cross-fade it (empty state included) so it doesn't
                        // snap. A fade — not the phone's card morph — because tablet
                        // users flip between notes rapidly and a morph each time
                        // would feel heavy. Keyed on selectedNoteId so the outgoing
                        // editor keeps showing its own note as it fades.
                        Crossfade(
                            targetState = selectedNoteId,
                            animationSpec = tween(durationMillis = 220),
                            modifier = Modifier.fillMaxSize(),
                            label = "tablet editor pane"
                        ) { paneNoteId ->
                            if (paneNoteId != null) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .widthIn(max = appSettings.lineWidth.maxWidthDp.dp)
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                    ) {
                                        EditorScreen(
                                            noteId = paneNoteId,
                                            onBack = { selectedNoteId = null },
                                            onNavigateToNote = { id -> selectedNoteId = id },
                                            hostScope = hostScope
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(R.string.select_note_to_view),
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // A note left open in the editor pane when the window narrowed (a
                // tablet turned upright) is opened in the full-screen editor, where
                // the narrow layout shows an open note, rather than dropped. Through
                // resolveOpenNoteRoute because the note may have been locked since it
                // was selected. The selection is cleared so Back from that editor
                // lands on the list instead of reopening it — in the same main-thread
                // step as the navigation: it is this effect's key, so clearing it any
                // earlier cancels the effect before it navigates, and clearing it
                // after a suspension can be skipped by that same cancellation.
                LaunchedEffect(selectedNoteId) {
                    val carried = selectedNoteId ?: return@LaunchedEffect
                    // One no longer selectable is left to the effect above to clear.
                    if (!isPaneSelectable(viewModel.observeNote(carried).first())) return@LaunchedEffect
                    val route = resolveOpenNoteRoute(carried, noteRepository)
                    withContext(Dispatchers.Main.immediate) {
                        selectedNoteId = null
                        noteOrigin = null
                        navController.navigate(route)
                    }
                }
                // Publish this NOTES destination's AnimatedVisibilityScope so its
                // rows can act as the shared-element source for the card->editor
                // morph. Phone path only — the tablet branch above opens the editor
                // in-pane and provides no scope.
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                NotesListScreen(
                    viewModel = viewModel,
                    onNoteClick = { noteId ->
                        if (noteId != null) {
                            // A grid tile has a fill of its own that the container has to
                            // start as; a list row does not (see NoteSource.startSurface).
                            val source = if (appSettings.notesLayout == NotesLayout.GRID) {
                                NoteSource.TILE
                            } else {
                                NoteSource.LIST
                            }
                            noteOrigin = NoteOrigin(noteId, source)
                            navController.navigate(NavRoutes.editorRoute(noteId))
                        }
                    },
                    onFabClick = {
                        coroutineScope.launch {
                            val newNote = viewModel.createNote()
                            // The note did not exist when the FAB was drawn, so the
                            // FAB owns a fixed key rather than one per note.
                            noteOrigin = NoteOrigin(newNote.id, NoteSource.FAB)
                            navController.navigateOnMain(NavRoutes.editorRoute(newNote.id))
                        }
                    },
                    onSearchClick = { navController.navigate(NavRoutes.SEARCH) },
                    onTagsClick = { navController.navigate(NavRoutes.TAGS) },
                    onArchiveClick = { navController.navigate(NavRoutes.ARCHIVE) },
                    onLockedClick = { navController.navigate(NavRoutes.LOCKED) },
                    onTrashClick = { navController.navigate(NavRoutes.TRASH) },
                    onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                    onOpenFileClick = onOpenFile,
                    lockPasscodeSet = appSettings.lockPasscodeSet,
                    onRequestSetPasscode = { navController.navigate(NavRoutes.SETTINGS) }
                )
                }
            }
        }
        composable(NavRoutes.EDITOR) {
            val noteId = it.arguments?.getString("noteId")
            NoteEditorDestination(
                noteId = noteId,
                origin = noteOrigin,
                animatedVisibilityScope = this
            ) {
                EditorScreen(
                    noteId = noteId,
                    onBack = { navController.popBackStack() },
                    onNavigateToNote = { id -> navController.navigate(NavRoutes.editorRoute(id)) },
                    hostScope = hostScope
                )
            }
        }
        composable(NavRoutes.TAGS) {
            TagsScreen(
                onBack = { navController.popBackStack() },
                onTagClick = { tagQuery ->
                    navController.navigate("${NavRoutes.SEARCH}?query=${Uri.encode(tagQuery)}")
                }
            )
        }
        composable(
            route = "${NavRoutes.SEARCH}?query={query}",
            arguments = listOf(navArgument("query") {
                type = NavType.StringType
                defaultValue = ""
            })
        ) {
            val viewModel = viewModel<SearchViewModel>(factory = viewModelFactory)
            val query = it.arguments?.getString("query").orEmpty()
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                SearchScreen(
                    viewModel = viewModel,
                    initialQuery = query,
                    onBack = { navController.popBackStack() },
                    onNoteClick = { noteId ->
                        noteOrigin = NoteOrigin(noteId, NoteSource.SEARCH)
                        navController.navigate(NavRoutes.editorRoute(noteId))
                    }
                )
            }
        }
        composable(NavRoutes.TRASH) {
            val viewModel = viewModel<TrashViewModel>(factory = viewModelFactory)
            TrashScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.ARCHIVE) {
            val viewModel = viewModel<ArchiveViewModel>(factory = viewModelFactory)
            CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                ArchiveScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNoteClick = { noteId ->
                        noteOrigin = NoteOrigin(noteId, NoteSource.ARCHIVE)
                        navController.navigate(NavRoutes.editorRoute(noteId))
                    }
                )
            }
        }
        composable(NavRoutes.LOCKED) {
            val viewModel = viewModel<LockedNotesViewModel>(factory = viewModelFactory)
            LockedNotesScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNoteClick = { noteId -> navController.navigate(NavRoutes.editorRoute(noteId)) },
                onOpenSettings = { navController.navigate(NavRoutes.SETTINGS) }
            )
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onPrivacyClick = { navController.navigate(NavRoutes.PRIVACY) },
                onSyncCenterClick = { navController.navigate(NavRoutes.SYNC_CENTER) }
            )
        }

        composable(NavRoutes.SYNC_CENTER) {
            val viewModel = viewModel<SyncCenterViewModel>(factory = viewModelFactory)
            SyncCenterScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNoteClick = { noteId -> navController.navigate(NavRoutes.editorRoute(noteId)) }
            )
        }

        composable(NavRoutes.PRIVACY) {
            com.markleaf.notes.feature.privacy.PrivacyDashboardScreen(
                onBack = { navController.popBackStack() }
            )
        }

        // Reading a file that is not a note (#326) — reached from "Open file…"
        // and from a file manager's ACTION_VIEW. Nothing is stored unless the
        // reader asks for it, and asking runs the same import the share sheet
        // has always used.
        composable(
            route = NavRoutes.VIEWER,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { entry ->
            val viewerViewModel = viewModel<NotesViewModel>(factory = viewModelFactory)
            val coroutineScope = rememberCoroutineScope()
            val uriArg = entry.arguments?.getString("uri").orEmpty()
            val uri = remember(uriArg) { Uri.parse(uriArg) }
            FileViewerScreen(
                uri = uri,
                contentMaxWidth = appSettings.lineWidth.maxWidthDp.dp,
                onBack = { navController.popBackStack() },
                onSaveAsNote = { body, createdAt, updatedAt ->
                    coroutineScope.launch {
                        val settings = settingsRepository.settings.first()
                        val newNote = viewerViewModel.createNote(
                            initialContent = body,
                            titleSource = settings.noteTitleSource,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                        // Saving from the read-only viewer is a complete note
                        // creation path. Mirror it immediately when folder
                        // sync is enabled; waiting for the first editor change
                        // leaves the new note local-only until then (#366).
                        settings.syncFolderUriOrNull()?.let { folderUri ->
                            withContext(Dispatchers.IO) {
                                NoteFolderMirror.writeNoteAndStamp(
                                    context = context,
                                    folderUri = folderUri,
                                    note = newNote,
                                    extension = settings.syncFileExtension,
                                    metadata = settings.mirrorMetadata(),
                                    onStamped = { stamped -> noteRepository.updateNote(stamped) }
                                )
                            }
                        }
                        withContext(Dispatchers.Main.immediate) {
                            navController.navigate(NavRoutes.editorRoute(newNote.id)) {
                                // The file has been kept; backing out of the new
                                // note belongs in the list, not in the viewer it
                                // came from.
                                popUpTo(NavRoutes.VIEWER) { inclusive = true }
                            }
                        }
                    }
                },
                // A relative `.md`/`.txt` link in a file being read externally can
                // still name a note already in the sync folder (#414) — opening it
                // does not touch the file on screen, so it fits the read-only
                // contract the rest of this screen keeps.
                onLocalLinkClick = { fileName ->
                    coroutineScope.launch {
                        val settings = settingsRepository.settings.first()
                        // resolveLocalNoteLink suspends into Room, which resumes its
                        // continuation on its own executor rather than hopping back to
                        // Main (see navigateOnMain's doc below, and #235) — so every
                        // branch here has to marshal back explicitly rather than assume
                        // it is still on Main.
                        when (val result = resolveLocalNoteLink(context, AppDatabase.getInstance(context), settings, fileName)) {
                            is LocalNoteLinkResult.Open -> navController.navigateOnMain(NavRoutes.editorRoute(result.noteId))
                            LocalNoteLinkResult.Locked ->
                                withContext(Dispatchers.Main.immediate) {
                                    Toast.makeText(context, R.string.wikilink_target_locked, Toast.LENGTH_SHORT).show()
                                }
                            LocalNoteLinkResult.NotFound ->
                                withContext(Dispatchers.Main.immediate) {
                                    Toast.makeText(context, R.string.quick_switcher_no_results, Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                }
            )
        }
    }
    } // CompositionLocalProvider(LocalSharedTransitionScope)
    } // SharedTransitionLayout
}

/**
 * What the "Open file…" picker lets you choose (#326).
 *
 * The `text` wildcard covers `.md` and `.txt` wherever the provider types them
 * honestly.
 * `application/octet-stream` is there because several file managers and cloud
 * providers have no MIME type for Markdown and fall back to it — without the
 * second entry those files are greyed out in the picker and cannot be opened at
 * all. A file that turns out not to be text is caught on read and reported.
 */
/**
 * Whether a note may stay open in the tablet's editor pane: it still exists, and
 * it is neither locked (the passcode gate guards those) nor in the trash.
 */
internal fun isPaneSelectable(note: Note?): Boolean =
    note != null && !note.locked && !note.trashed

private val OPEN_FILE_MIME_TYPES = arrayOf("text/*", "application/octet-stream")

/**
 * Navigate from a coroutine that has been through a suspend point.
 *
 * `NavController` must be driven from the main thread, and every call site that
 * uses this first suspends into Room — creating the note, or resolving where an
 * incoming id should land. Room resumes its continuations on its own executor,
 * so what puts us back on the main thread is the dispatcher that intercepted the
 * continuation, not anything the code says.
 *
 * In the app that dispatcher is the composition's, and it does hop back, which
 * is why this has never misbehaved in production. Under Compose's *test*
 * dispatcher it does not: `navigate()` then runs on a Room thread, throws
 * `IllegalStateException`, and leaves the new back-stack entry stuck in
 * `INITIALIZED` — so the next activity destroy dies with "State must be at least
 * CREATED to move to DESTROYED" and takes the rest of the instrumentation run
 * with it (#235).
 *
 * Stating the requirement is cheap and removes the dependency on who intercepted
 * the continuation. `Main.immediate` so a call that is already on the main
 * thread stays in the same frame rather than waiting for the next one.
 */
private suspend fun NavHostController.navigateOnMain(route: String) {
    withContext(Dispatchers.Main.immediate) { navigate(route) }
}

@Composable
private fun CollapsedNoteListRail(
    onExpandClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(56.dp)
            .systemBarsPadding(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            IconButton(onClick = onExpandClick) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.expand_note_list))
            }
        }
    }
}
