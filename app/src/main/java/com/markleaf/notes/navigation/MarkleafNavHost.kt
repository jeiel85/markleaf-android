package com.markleaf.notes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.markleaf.notes.feature.editor.EditorScreen
import com.markleaf.notes.feature.notes.NotesListScreen
import com.markleaf.notes.feature.search.SearchScreen
import com.markleaf.notes.feature.settings.SettingsScreen
import com.markleaf.notes.feature.tags.TagsScreen
import com.markleaf.notes.feature.trash.TrashScreen

@Composable
fun MarkleafNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES
    ) {
        composable(NavRoutes.NOTES) {
            val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<com.markleaf.notes.ui.viewmodel.NotesViewModel>()
            NotesListScreen(
                viewModel = viewModel,
                onNoteClick = { /* TODO: navigate to editor */ },
                onFabClick = { /* TODO: create note and navigate */ }
            )
        }
        composable(NavRoutes.EDITOR) {
            EditorScreen(noteId = null, onBack = { /* TODO: navigate back */ })
        }
        composable(NavRoutes.TAGS) {
            TagsScreen()
        }
        composable(NavRoutes.SEARCH) {
            SearchScreen()
        }
        composable(NavRoutes.TRASH) {
            TrashScreen()
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreen()
        }
    }
}
