package com.markleaf.notes.navigation

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.feature.notes.NotesListScreen
import com.markleaf.notes.feature.search.SearchScreen
import com.markleaf.notes.feature.settings.SettingsScreen
import com.markleaf.notes.feature.tags.TagsScreen
import com.markleaf.notes.feature.trash.TrashScreen
import kotlinx.coroutines.launch

@Composable
fun MarkleafNavHost(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass
) {
    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES
    ) {
        composable(NavRoutes.NOTES) {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.markleaf.notes.ui.viewmodel.NotesViewModel>()
            val coroutineScope = rememberCoroutineScope()
            
            if (isExpanded) {
                var selectedNoteId by remember { mutableStateOf<String?>(null) }
                
                androidx.compose.foundation.layout.Row(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize()
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.weight(1f)
                    ) {
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
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier.weight(1.5f)
                    ) {
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
                            androidx.compose.foundation.layout.Box(
                                modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                                contentAlignment = androidx.compose.ui.Alignment.Center
                            ) {
                                androidx.compose.material3.Text("Select a note to view")
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
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.markleaf.notes.ui.viewmodel.SearchViewModel>()
            SearchScreen(viewModel = viewModel)
        }
        composable(NavRoutes.TRASH) {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.markleaf.notes.ui.viewmodel.TrashViewModel>()
            TrashScreen(viewModel = viewModel)
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen()
        }
    }
}
