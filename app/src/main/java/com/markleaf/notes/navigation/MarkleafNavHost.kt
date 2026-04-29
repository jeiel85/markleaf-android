package com.markleaf.notes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.markleaf.notes.feature.notes.NotesListScreen

@Composable
fun MarkleafNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES
    ) {
        composable(NavRoutes.NOTES) {
            NotesListScreen(
                onNoteClick = { noteId ->
                    // TODO: Navigate to editor
                },
                onFabClick = {
                    // TODO: Create new note and navigate to editor
                }
            )
        }
        composable(NavRoutes.EDITOR) {
            EditorScreenPlaceholder()
        }
        composable(NavRoutes.TAGS) {
            TagsScreenPlaceholder()
        }
        composable(NavRoutes.SEARCH) {
            SearchScreenPlaceholder()
        }
        composable(NavRoutes.TRASH) {
            TrashScreenPlaceholder()
        }
        composable(NavRoutes.SETTINGS) {
            SettingsScreenPlaceholder()
        }
    }
}
