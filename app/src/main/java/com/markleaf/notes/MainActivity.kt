package com.markleaf.notes

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.onboarding.StarterNotesSeeder
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.navigation.MarkleafNavHost
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory
import com.markleaf.notes.widget.QuickNoteWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Make the app edge-to-edge across all Android versions and devices.
        // Without this, devices that do not enforce edge-to-edge automatically
        // (e.g. tablets on Android 14 and below) leave the status bar
        // semi-transparent while no inset padding is applied, so content draws
        // under the notification area. enableEdgeToEdge() makes Compose feed
        // the right WindowInsets to Material 3 Scaffold + TopAppBar, which
        // already know how to add the correct top/bottom padding.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getInstance(applicationContext)
        val settingsRepository = AppSettingsRepository(applicationContext)

        lifecycleScope.launch(Dispatchers.IO) {
            StarterNotesSeeder.seedIfNeeded(applicationContext, database)
        }

        // Apply FLAG_SECURE based on the persisted setting. Re-applies on every
        // change so toggling in Settings takes effect immediately.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings
                    .map { it.screenshotProtection }
                    .distinctUntilChanged()
                    .collect { enabled ->
                        if (enabled) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                        }
                    }
            }
        }

        val shouldCreateNote = intent.action == QuickNoteWidget.ACTION_CREATE_NOTE
        val sharedText = extractSharedText(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val viewModelFactory = remember {
                MarkleafViewModelFactory(LocalNoteRepository(database))
            }
            MarkleafTheme {
                val navController = rememberNavController()
                MarkleafNavHost(
                    navController = navController,
                    windowSizeClass = windowSizeClass,
                    viewModelFactory = viewModelFactory,
                    shouldCreateNote = shouldCreateNote,
                    sharedText = sharedText
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.action == QuickNoteWidget.ACTION_CREATE_NOTE) {
            recreate()
        } else if (intent?.action == Intent.ACTION_SEND && extractSharedText(intent) != null) {
            setIntent(intent)
            recreate()
        }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type != "text/plain") return null
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf { it.isNotBlank() }
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
        if (subject == null && text == null) return null
        return buildString {
            if (subject != null) {
                append("# ").append(subject)
                if (text != null) append("\n\n")
            }
            if (text != null) append(text)
        }
    }
}
