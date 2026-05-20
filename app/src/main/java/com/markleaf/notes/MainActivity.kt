package com.markleaf.notes

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.onboarding.StarterNotesSeeder
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.feature.lock.BiometricLockGate
import com.markleaf.notes.feature.onboarding.WelcomeOnboardingSheet
import com.markleaf.notes.navigation.MarkleafNavHost
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory
import com.markleaf.notes.widget.QuickNoteWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
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

        // Auto-reconcile from the sync folder when the app comes to the
        // foreground, throttled to once per minute. This catches changes
        // made on other devices since the user last visited Markleaf,
        // without ever overwriting a newer in-app edit (importChanges
        // applies the file→DB direction only when the file is strictly
        // newer than the DB record).
        lifecycleScope.launch {
            val noteRepository = LocalNoteRepository(database)
            var lastReconcileMs = 0L
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val now = System.currentTimeMillis()
                if (now - lastReconcileMs < THROTTLE_MS) return@repeatOnLifecycle
                lastReconcileMs = now
                val settings = settingsRepository.settings.first()
                val uriString = settings.syncFolderUri ?: return@repeatOnLifecycle
                val uri = runCatching { android.net.Uri.parse(uriString) }.getOrNull()
                    ?: return@repeatOnLifecycle
                val notes = withContext(Dispatchers.IO) {
                    noteRepository.observeNotes().first()
                }
                withContext(Dispatchers.IO) {
                    NoteFolderMirror.importChanges(
                        context = applicationContext,
                        folderUri = uri,
                        existing = notes,
                        applyUpdate = { updated -> noteRepository.updateNote(updated) },
                        applyCreate = { created -> noteRepository.createNote(created) }
                    )
                }
                settingsRepository.setSyncLastSyncedAt(System.currentTimeMillis())
            }
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
        val openNoteId = if (intent.action == QuickNoteWidget.ACTION_OPEN_NOTE) {
            intent.getStringExtra(QuickNoteWidget.EXTRA_NOTE_ID)
        } else null
        val sharedText = extractSharedText(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val viewModelFactory = remember {
                MarkleafViewModelFactory(LocalNoteRepository(database))
            }
            val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
            MarkleafTheme(
                dynamicColor = appSettings.colorPalette == ColorPalette.MATERIAL_YOU
            ) {
                BiometricLockGate(enabled = appSettings.biometricLockEnabled) {
                    val navController = rememberNavController()
                    MarkleafNavHost(
                        navController = navController,
                        windowSizeClass = windowSizeClass,
                        viewModelFactory = viewModelFactory,
                        shouldCreateNote = shouldCreateNote,
                        sharedText = sharedText,
                        openNoteId = openNoteId
                    )
                    if (!appSettings.onboardingCompleted) {
                        WelcomeOnboardingSheet(
                            onDismiss = {
                                lifecycleScope.launch {
                                    settingsRepository.setOnboardingCompleted(true)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Nudge the home-screen widget so the recent-notes list reflects any
        // edits made in this session as soon as the user returns to launcher.
        runCatching {
            val mgr = android.appwidget.AppWidgetManager.getInstance(applicationContext)
            val ids = mgr.getAppWidgetIds(
                android.content.ComponentName(applicationContext, QuickNoteWidget::class.java)
            )
            if (ids.isNotEmpty()) {
                mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        when {
            intent?.action == QuickNoteWidget.ACTION_CREATE_NOTE -> {
                setIntent(intent)
                recreate()
            }
            intent?.action == QuickNoteWidget.ACTION_OPEN_NOTE -> {
                setIntent(intent)
                recreate()
            }
            intent?.action == Intent.ACTION_SEND && extractSharedText(intent) != null -> {
                setIntent(intent)
                recreate()
            }
        }
    }

    private companion object {
        const val THROTTLE_MS = 60_000L
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
