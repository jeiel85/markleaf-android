package com.markleaf.notes.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
fun MarkleafNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES
    ) {
        composable(NavRoutes.NOTES) {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.markleaf.notes.ui.viewmodel.NotesViewModel>()
            val coroutineScope = rememberCoroutineScope()
            NotesListScreen(
                viewModel = viewModel,
                onNoteClick = { noteId -> navController.navigate(NavRoutes.EDITOR) },
                onFabClick = {
                    coroutineScope.launch {
                        val newNote = viewModel.createNote()
                        navController.navigate("${NavRoutes.EDITOR}?noteId=${newNote.id}")
                    }
                }
            )
        }
        composable(NavRoutes.EDITOR) {
            val noteId = it.arguments?.getString("noteId")
            EditorScreen(noteId = noteId, onBack = { navController.popBackStack() })
        }
        composable(NavRoutes.TAGS) {
            TagsScreen()
        }
        composable(NavRoutes.SEARCH) {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.markleaf.notes.ui.viewmodel.SearchViewModel>()
            SearchScreen(viewModel = viewModel)
        }
        composable(NavRoutes.TRASH) {
            TrashScreen()
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen()
        }
    }
}
