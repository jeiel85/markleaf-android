package com.markleaf.notes.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import android.net.Uri
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
import com.markleaf.notes.feature.notes.NotesListScreen
import com.markleaf.notes.feature.search.SearchScreen
import com.markleaf.notes.feature.settings.SettingsScreen
import com.markleaf.notes.feature.tags.TagRail
import com.markleaf.notes.feature.tags.TagsScreen
import com.markleaf.notes.feature.trash.TrashScreen
import com.markleaf.notes.feature.sync.SyncCenterScreen
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.ui.viewmodel.ArchiveViewModel
import com.markleaf.notes.ui.viewmodel.NotesViewModel
import com.markleaf.notes.ui.viewmodel.SearchViewModel
import com.markleaf.notes.ui.viewmodel.TrashViewModel
import com.markleaf.notes.ui.viewmodel.SyncCenterViewModel
import kotlinx.coroutines.launch

@Composable
fun MarkleafNavHost(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    viewModelFactory: ViewModelProvider.Factory,
    shouldCreateNote: Boolean = false,
    sharedText: String? = null,
    openNoteId: String? = null
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val context = LocalContext.current
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())

    // One-shot intent entry points — widget "new note", text/file shared or
    // opened into the app (#139), and a widget tap on a recent note. These must
    // run EXACTLY ONCE per launch. They used to live in LaunchedEffects inside
    // the NOTES destination, but a navigation destination re-enters composition
    // every time it returns to the foreground — e.g. pressing back from the
    // editor — which re-ran the effect, created another duplicate note, and
    // immediately reopened the editor, trapping the user in a reopen loop
    // (#142). Hoisting them here ties them to the host, which stays composed for
    // the whole activity instance, so returning from the editor no longer
    // re-imports. A genuinely new intent arrives on a fresh activity
    // (onNewIntent → recreate) and re-composes the host, so new shares/opens
    // still import. The three sources are mutually exclusive (each derives from
    // a single intent action), so a `when` handles at most one.
    val intentEntryViewModel = viewModel<NotesViewModel>(factory = viewModelFactory)
    LaunchedEffect(Unit) {
        when {
            shouldCreateNote -> {
                val newNote = intentEntryViewModel.createNote()
                navController.navigate(NavRoutes.editorRoute(newNote.id))
            }
            !sharedText.isNullOrBlank() -> {
                val newNote = intentEntryViewModel.createNote(sharedText)
                navController.navigate(NavRoutes.editorRoute(newNote.id))
            }
            !openNoteId.isNullOrBlank() -> {
                navController.navigate(NavRoutes.editorRoute(openNoteId))
            }
        }
    }

    // Restrained shared-axis-X motion for forward/back navigation: the incoming
    // screen slides a fifth of the width and cross-fades. Because the manifest
    // opts into enableOnBackInvokedCallback and Navigation 2.8 makes the pop
    // transitions seekable, the system back gesture drives popEnter/popExit as a
    // predictive "peek" of the previous screen rather than an instant swap. The
    // tablet layout keeps the editor in-pane (no navigation) and never hits these.
    val navMotion = tween<Float>(durationMillis = 280)
    val navOffsetMotion = tween<IntOffset>(durationMillis = 280)
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES,
        enterTransition = {
            slideInHorizontally(navOffsetMotion) { it / 5 } + fadeIn(navMotion)
        },
        exitTransition = {
            slideOutHorizontally(navOffsetMotion) { -it / 5 } + fadeOut(navMotion)
        },
        popEnterTransition = {
            slideInHorizontally(navOffsetMotion) { -it / 5 } + fadeIn(navMotion)
        },
        popExitTransition = {
            slideOutHorizontally(navOffsetMotion) { it / 5 } + fadeOut(navMotion)
        }
    ) {
        composable(NavRoutes.NOTES) {
            val viewModel = viewModel<NotesViewModel>(factory = viewModelFactory)
            val coroutineScope = rememberCoroutineScope()

            // Intent entry points (widget new-note, shared/opened content, widget
            // recent-note tap) are handled once at the host scope above, not here
            // — see the LaunchedEffect in MarkleafNavHost's body (#142).

            if (isExpanded) {
                var selectedNoteId by remember { mutableStateOf<String?>(null) }
                var isNoteListCollapsed by remember { mutableStateOf(false) }
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
                    // Tag rail — the persistent sidebar of the 3-column tablet
                    // layout (tags | note list | editor). Tapping a tag filters
                    // the note list pane in place via the shared NotesViewModel.
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
                            onSelectTag = { viewModel.selectTag(it) }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .background(dividerColor)
                    )
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
                                onTrashClick = { navController.navigate(NavRoutes.TRASH) },
                                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                                onCollapseClick = { isNoteListCollapsed = true },
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
                        if (selectedNoteId != null) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = appSettings.lineWidth.maxWidthDp.dp)
                                    .fillMaxWidth()
                                    .fillMaxHeight()
                            ) {
                                EditorScreen(
                                    noteId = selectedNoteId,
                                    onBack = { selectedNoteId = null },
                                    onNavigateToNote = { id -> selectedNoteId = id }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = appSettings.lineWidth.maxWidthDp.dp)
                                    .fillMaxWidth()
                                    .fillMaxHeight(),
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
            } else {
                NotesListScreen(
                    viewModel = viewModel,
                    onNoteClick = { noteId ->
                        if (noteId != null) navController.navigate(NavRoutes.editorRoute(noteId))
                    },
                    onFabClick = {
                        coroutineScope.launch {
                            val newNote = viewModel.createNote()
                            navController.navigate(NavRoutes.editorRoute(newNote.id))
                        }
                    },
                    onSearchClick = { navController.navigate(NavRoutes.SEARCH) },
                    onTagsClick = { navController.navigate(NavRoutes.TAGS) },
                    onArchiveClick = { navController.navigate(NavRoutes.ARCHIVE) },
                    onTrashClick = { navController.navigate(NavRoutes.TRASH) },
                    onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) }
                )
            }
        }
        composable(NavRoutes.EDITOR) {
            val noteId = it.arguments?.getString("noteId")
            EditorScreen(
                noteId = noteId,
                onBack = { navController.popBackStack() },
                onNavigateToNote = { id -> navController.navigate(NavRoutes.editorRoute(id)) }
            )
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
            SearchScreen(
                viewModel = viewModel,
                initialQuery = query,
                onBack = { navController.popBackStack() },
                onNoteClick = { noteId -> navController.navigate(NavRoutes.editorRoute(noteId)) }
            )
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
            ArchiveScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNoteClick = { noteId -> navController.navigate(NavRoutes.editorRoute(noteId)) }
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
    }
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
