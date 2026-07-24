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

// 화면을 통째로 띄우지 않는 테스트용. 목록과 에디터를 함께 지나가는 흐름에는
// 쓰면 안 된다 — `EditorScreen` 은 `AppDatabase.getInstance` 에서 자기
// repository 를 만들기 때문에 여기서 주입한 DB 를 보지 않는다(AppIntegrationTest).
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
