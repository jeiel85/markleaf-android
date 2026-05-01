package com.markleaf.notes.navigation

import androidx.compose.foundation.background
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.feature.notes.NotesListScreen
import com.markleaf.notes.feature.search.SearchScreen
import com.markleaf.notes.feature.settings.SettingsScreen
import com.markleaf.notes.feature.tags.TagsScreen
import com.markleaf.notes.feature.trash.TrashScreen
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.ui.viewmodel.NotesViewModel
import com.markleaf.notes.ui.viewmodel.SearchViewModel
import com.markleaf.notes.ui.viewmodel.TrashViewModel
import kotlinx.coroutines.launch

@Composable
fun MarkleafNavHost(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass,
    viewModelFactory: ViewModelProvider.Factory
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val context = LocalContext.current
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES
    ) {
        composable(NavRoutes.NOTES) {
            val viewModel = viewModel<NotesViewModel>(factory = viewModelFactory)
            val coroutineScope = rememberCoroutineScope()
            
            if (isExpanded) {
                var selectedNoteId by remember { mutableStateOf<String?>(null) }
                var isNoteListCollapsed by remember { mutableStateOf(false) }
                val listPaneColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f)
                val editorPaneColor = MaterialTheme.colorScheme.background
                val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.56f)
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(editorPaneColor)
                ) {
                    if (isNoteListCollapsed) {
                        CollapsedNoteListRail(onExpandClick = { isNoteListCollapsed = false })
                    } else {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(listPaneColor)
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
                                onTrashClick = { navController.navigate(NavRoutes.TRASH) },
                                onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                                onCollapseClick = { isNoteListCollapsed = true },
                                selectedNoteId = selectedNoteId,
                                containerColor = listPaneColor
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
                                    onLinkClick = { title ->
                                        navController.navigate("${NavRoutes.SEARCH}?query=${Uri.encode(title)}")
                                    },
                                    onNoteClick = { noteId ->
                                        selectedNoteId = noteId
                                    }
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
                                Text(stringResource(R.string.select_note_to_view))
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
                onLinkClick = { title ->
                    navController.navigate("${NavRoutes.SEARCH}?query=${Uri.encode(title)}")
                },
                onNoteClick = { targetId ->
                    navController.navigate(NavRoutes.editorRoute(targetId))
                }
            )
        }
        composable(NavRoutes.TAGS) {
            TagsScreen(
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
                onNoteClick = { noteId -> navController.navigate(NavRoutes.editorRoute(noteId)) }
            )
        }
        composable(NavRoutes.TRASH) {
            val viewModel = viewModel<TrashViewModel>(factory = viewModelFactory)
            TrashScreen(viewModel = viewModel)
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun CollapsedNoteListRail(
    onExpandClick: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(56.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            IconButton(onClick = onExpandClick) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = stringResource(R.string.expand_note_list))
            }
        }
    }
}
