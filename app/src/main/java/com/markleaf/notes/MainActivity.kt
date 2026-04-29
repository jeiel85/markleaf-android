package com.markleaf.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.markleaf.notes.navigation.MarkleafNavHost
import com.markleaf.notes.ui.theme.MarkleafTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarkleafTheme {
                val navController = rememberNavController()
                MarkleafNavHost(navController = navController)
            }
        }
    }
}
