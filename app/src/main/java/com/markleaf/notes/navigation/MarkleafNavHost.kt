package com.markleaf.notes.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.feature.notes.NotesListScreen
import com.markleaf.notes.feature.search.SearchScreen
import com.markleaf.notes.feature.settings.SettingsScreen
import com.markleaf.notes.feature.tags.TagsScreen
import com.markleaf.notes.feature.trash.TrashScreen
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
    
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES
    ) {
        composable(NavRoutes.NOTES) {
            val viewModel = viewModel<NotesViewModel>(factory = viewModelFactory)
            val coroutineScope = rememberCoroutineScope()
            
            if (isExpanded) {
                var selectedNoteId by remember { mutableStateOf<String?>(null) }
                
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NotesListScreen(
                            viewModel = viewModel,
                            onNoteClick = { noteId -> selectedNoteId = noteId },
                            onFabClick = {
                                coroutineScope.launch {
                                    val newNote = viewModel.createNote()
                                    selectedNoteId = newNote.id
                                }
                            }
                        )
                    }
                    Box(modifier = Modifier.weight(1.5f)) {
                        if (selectedNoteId != null) {
                            EditorScreen(
                                noteId = selectedNoteId,
                                onBack = { selectedNoteId = null },
                                onLinkClick = { title ->
                                    navController.navigate("${NavRoutes.SEARCH}?query=$title")
                                },
                                onNoteClick = { noteId ->
                                    selectedNoteId = noteId
                                }
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Select a note to view")
                            }
                        }
                    }
                }
            } else {
                NotesListScreen(
                    viewModel = viewModel,
                    onNoteClick = { noteId -> navController.navigate("${NavRoutes.EDITOR}?noteId=$noteId") },
                    onFabClick = {
                        coroutineScope.launch {
                            val newNote = viewModel.createNote()
                            navController.navigate("${NavRoutes.EDITOR}?noteId=${newNote.id}")
                        }
                    }
                )
            }
        }
        composable(NavRoutes.EDITOR) {
            val noteId = it.arguments?.getString("noteId")
            EditorScreen(
                noteId = noteId, 
                onBack = { navController.popBackStack() },
                onLinkClick = { title ->
                    navController.navigate("${NavRoutes.SEARCH}?query=$title")
                },
                onNoteClick = { targetId ->
                    navController.navigate("${NavRoutes.EDITOR}?noteId=$targetId")
                }
            )
        }
        composable(NavRoutes.TAGS) {
            TagsScreen()
        }
        composable(NavRoutes.SEARCH) {
            val viewModel = viewModel<SearchViewModel>(factory = viewModelFactory)
            SearchScreen(viewModel = viewModel)
        }
        composable(NavRoutes.TRASH) {
            val viewModel = viewModel<TrashViewModel>(factory = viewModelFactory)
            TrashScreen(viewModel = viewModel)
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen()
        }
    }
}
