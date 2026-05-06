package com.markleaf.notes.ui

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.navigation.MarkleafNavHost
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory

fun createInMemoryMarkleafDatabase(): AppDatabase =
    Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        AppDatabase::class.java
    )
        .allowMainThreadQueries()
        .build()

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
val compactWindowSizeClass: WindowSizeClass =
    WindowSizeClass.calculateFromSize(DpSize(width = 360.dp, height = 800.dp))

fun markleafTestContent(database: AppDatabase): @Composable () -> Unit {
    val viewModelFactory = MarkleafViewModelFactory(LocalNoteRepository(database))
    return {
        MarkleafTheme {
            MarkleafNavHost(
                navController = rememberNavController(),
                windowSizeClass = compactWindowSizeClass,
                viewModelFactory = viewModelFactory
            )
        }
    }
}
