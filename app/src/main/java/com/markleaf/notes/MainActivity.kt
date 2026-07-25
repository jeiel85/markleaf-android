package com.markleaf.notes

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
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
import com.markleaf.notes.data.settings.EditorFont
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.NoteImporter
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.data.sync.mirrorMetadata
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
            val importer = NoteImporter(database)
            var lastReconcileMs = 0L
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                val now = System.currentTimeMillis()
                if (now - lastReconcileMs < THROTTLE_MS) return@repeatOnLifecycle
                lastReconcileMs = now
                val settings = settingsRepository.settings.first()
                val uri = settings.syncFolderUriOrNull() ?: return@repeatOnLifecycle
                val notes = withContext(Dispatchers.IO) {
                    // Full set (incl. trashed/archived) so the reconcile can't
                    // re-import a hidden note as a brand-new one — see #148.
                    noteRepository.getAllNotes()
                }
                withContext(Dispatchers.IO) {
                    NoteFolderMirror.importChanges(
                        context = applicationContext,
                        folderUri = uri,
                        existing = notes,
                        applyUpdate = { updated -> importer.update(updated) },
                        applyCreate = { created -> importer.create(created) },
                        metadata = settings.mirrorMetadata(),
                        titleSource = settings.noteTitleSource
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
        val sharedText = extractInitialContent(intent)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val viewModelFactory = remember {
                MarkleafViewModelFactory(LocalNoteRepository(database))
            }
            val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
            MarkleafTheme(
                dynamicColor = appSettings.colorPalette == ColorPalette.MATERIAL_YOU,
                useSerif = appSettings.editorFont == EditorFont.SERIF
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

    // androidx.activity 1.9 tightened this override to a non-null Intent (it
    // mirrors the platform's @NonNull annotation), so the parameter and the
    // body's former null-safe calls are now plain non-null accesses.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        when {
            intent.action == QuickNoteWidget.ACTION_CREATE_NOTE -> {
                setIntent(intent)
                recreate()
            }
            intent.action == QuickNoteWidget.ACTION_OPEN_NOTE -> {
                setIntent(intent)
                recreate()
            }
            intent.action == Intent.ACTION_SEND &&
                (intent.streamUri() != null || extractSharedText(intent) != null) -> {
                setIntent(intent)
                recreate()
            }
            intent.action == Intent.ACTION_VIEW && intent.data != null -> {
                setIntent(intent)
                recreate()
            }
        }
    }

    private companion object {
        const val THROTTLE_MS = 60_000L

        // Cap how much of an opened/shared file we pull into a note so a huge or
        // non-text file can't OOM/ANR the cold-start path (#139). Real note files
        // are a few KB; 2M chars is a generous ceiling.
        const val MAX_IMPORT_CHARS = 2_000_000
    }

    /**
     * Note body to seed from an external intent, or null if there's nothing to
     * import. Covers three entry points:
     *  - ACTION_VIEW of a `.md` / `.txt` file tapped in a file manager (#139),
     *  - ACTION_SEND of a shared file stream (#139),
     *  - ACTION_SEND of plain text from the system share sheet (the original
     *    behaviour).
     */
    private fun extractInitialContent(intent: Intent?): String? {
        intent ?: return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let(::readNoteFromUri)
            Intent.ACTION_SEND -> intent.streamUri()?.let(::readNoteFromUri)
                ?: extractSharedText(intent)
            else -> null
        }
    }

    private fun extractSharedText(intent: Intent?): String? {
        if (intent?.action != Intent.ACTION_SEND) return null
        if (intent.type?.startsWith("text/") != true) return null
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

    @Suppress("DEPRECATION")
    private fun Intent.streamUri(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            getParcelableExtra(Intent.EXTRA_STREAM)
        }

    /**
     * Read an opened/shared file as UTF-8 text and turn it into a note body.
     * When the file has no leading heading, its name seeds the title so the
     * imported note isn't titled by whatever its first body line happens to be
     * — matching the "filename is the note title" expectation from #134.
     */
    private fun readNoteFromUri(uri: Uri): String? {
        val content = runCatching {
            contentResolver.openInputStream(uri)?.use(::readCapped)
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: return null

        val name = displayNameFor(uri)
            ?.substringBeforeLast('.')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val trimmed = content.trimStart()
        val alreadyTitled = trimmed.startsWith("#") || trimmed.startsWith("---")
        return if (name != null && !alreadyTitled) "# $name\n\n$content" else content
    }

    private fun readCapped(input: java.io.InputStream): String {
        val reader = input.bufferedReader(Charsets.UTF_8)
        val sb = StringBuilder()
        val buf = CharArray(8192)
        var total = 0
        while (total < MAX_IMPORT_CHARS) {
            val read = reader.read(buf)
            if (read < 0) break
            val take = minOf(read, MAX_IMPORT_CHARS - total)
            sb.append(buf, 0, take)
            total += take
        }
        return sb.toString()
    }

    private fun displayNameFor(uri: Uri): String? {
        if (uri.scheme == "file") return uri.lastPathSegment
        return runCatching {
            contentResolver.query(
                uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
