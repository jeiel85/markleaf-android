package com.markleaf.notes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.markleaf.notes.feature.editor.EditorScreenPlaceholder
import com.markleaf.notes.feature.notes.NotesListScreen

@Composable
fun MarkleafNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES
    ) {
        composable(NavRoutes.NOTES) {
            NotesListScreen(
                onNoteClick = { /* TODO: navigate to editor */ },
                onFabClick = { /* TODO: create note and navigate */ }
            )
        }
        composable(NavRoutes.EDITOR) {
            EditorScreenPlaceholder(noteId = null, onBack = { /* TODO: navigate back */ })
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
