package com.markleaf.notes.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

@Composable
fun MarkleafNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.NOTES
    ) {
        composable(NavRoutes.NOTES) {
            NotesScreenPlaceholder()
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
