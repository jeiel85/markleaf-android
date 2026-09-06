package com.markleaf.notes

import android.app.UiModeManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.markleaf.notes.data.settings.ThemeMode
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.NoteImporter
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.feature.lock.BiometricLockGate
import com.markleaf.notes.feature.onboarding.WelcomeOnboardingSheet
import com.markleaf.notes.navigation.MarkleafNavHost
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory
import com.markleaf.notes.util.ExternalFile
import com.markleaf.notes.widget.QuickNoteWidget
import com.markleaf.notes.widget.SingleNoteWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

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

        // Tell the system which night mode this app is in, so the *starting
        // window* — the splash the system draws before any app code runs — is
        // resolved with the Theme the user chose rather than the phone's
        // dark-mode setting. #354 fixed the window the app itself owns; this is
        // the one it does not, and nothing an activity does later can reach it.
        //
        // setApplicationNightMode persists the -night qualifier for this app, so
        // the effect is on the *next* cold start rather than this one. API 31+;
        // below that the splash keeps following the system, which is what every
        // version has done so far.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    settingsRepository.settings
                        .map { it.themeMode }
                        .distinctUntilChanged()
                        .collect(::applyApplicationNightMode)
                }
            }
        }

        val shouldCreateNote = intent.action == QuickNoteWidget.ACTION_CREATE_NOTE
        val openNoteId = if (intent.action == QuickNoteWidget.ACTION_OPEN_NOTE) {
            intent.getStringExtra(QuickNoteWidget.EXTRA_NOTE_ID)
        } else null
        val sharedContent = extractInitialContent(intent)
        // A file opened from elsewhere (ACTION_VIEW) is shown for reading rather
        // than imported (#326); sharing one in (ACTION_SEND) still means "take
        // this", and keeps creating a note.
        val viewFileUri = intent.takeIf { it.action == Intent.ACTION_VIEW }?.data?.toString()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val viewModelFactory = remember {
                MarkleafViewModelFactory(LocalNoteRepository(database))
            }
            val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
            val systemDark = isSystemInDarkTheme()
            MarkleafTheme(
                darkTheme = when (appSettings.themeMode) {
                    ThemeMode.SYSTEM -> systemDark
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                },
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
                        sharedText = sharedContent?.body,
                        sharedCreatedAt = sharedContent?.createdAt,
                        sharedUpdatedAt = sharedContent?.updatedAt,
                        openNoteId = openNoteId,
                        viewFileUri = viewFileUri
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

    /**
     * Hands [mode] to the system as this app's night mode (#354).
     *
     * `MODE_NIGHT_AUTO` is what returns a Theme of [ThemeMode.SYSTEM] to
     * following the device, which is why SYSTEM maps to it rather than to
     * "leave whatever was set last".
     *
     * Only called when the mode actually changes — the flow is
     * `distinctUntilChanged`, and the check below drops the redundant call on
     * every launch, because the setting arrives once per process whether or not
     * the user touched it and each real call is a configuration change.
     */
    @RequiresApi(Build.VERSION_CODES.S)
    private fun applyApplicationNightMode(mode: ThemeMode) {
        val manager = getSystemService(UiModeManager::class.java) ?: return
        // Unconditional on purpose. The effective `uiMode` says what the app is
        // rendering, not whether an override has been *persisted* — picking Dark
        // while the phone is already dark matches without storing anything, and
        // the next system flip would then resolve the splash from the phone
        // again. There is no getApplicationNightMode to ask, so the setting is
        // re-asserted once per process; measured on an API 36 emulator, a cold
        // start in each of the three modes logs one "Displayed MainActivity" and
        // no relaunch, so re-asserting an unchanged mode costs nothing.
        //
        // Not wrapped in runCatching: every value this can pass is in the
        // platform's own @NightMode IntDef, so a throw here would mean the
        // system server died — not something to hide from the user by leaving
        // the Theme silently unapplied.
        manager.setApplicationNightMode(mode.toApplicationNightMode())
    }

    override fun onPause() {
        super.onPause()
        // Nudge the home-screen widgets so they reflect any edits made in this
        // session as soon as the user returns to the launcher.
        runCatching {
            val mgr = android.appwidget.AppWidgetManager.getInstance(applicationContext)
            val ids = mgr.getAppWidgetIds(
                android.content.ComponentName(applicationContext, QuickNoteWidget::class.java)
            )
            if (ids.isNotEmpty()) {
                mgr.notifyAppWidgetViewDataChanged(ids, R.id.widget_list)
            }
        }
        // The single-note widgets redraw rather than reload a list, and each one
        // also re-checks that its note may still be shown — a note moved into the
        // Locked space while the app was open must stop rendering (#351).
        runCatching { SingleNoteWidget.refreshAll(applicationContext) }
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
    }

    /**
     * Note body to seed from an external intent, or null if there's nothing to
     * import. Two entry points, both of them a share:
     *  - ACTION_SEND of a shared file stream (#139),
     *  - ACTION_SEND of plain text from the system share sheet (the original
     *    behaviour).
     *
     * ACTION_VIEW used to import here too. It now opens the file in the viewer
     * instead (#326) — tapping a file in a file manager is a request to read it,
     * and importing wrote a note and, with folder sync on, a second copy of the
     * file, for every file merely looked at. Keeping it is one tap in the viewer.
     */
    private data class ImportedContent(
        val body: String,
        val createdAt: Instant?,
        val updatedAt: Instant?
    )

    private fun extractInitialContent(intent: Intent?): ImportedContent? {
        intent ?: return null
        if (intent.action != Intent.ACTION_SEND) return null
        return intent.streamUri()?.let(::readNoteFromUri)
            ?: extractSharedText(intent)?.let { ImportedContent(it, null, null) }
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
     * Read a shared file as UTF-8 text and turn it into a note body. The rules
     * — the size cap, and seeding the title from the file name — live in
     * [ExternalFile], which the file viewer reads through as well.
     */
    private fun readNoteFromUri(uri: Uri): ImportedContent? =
        ExternalFile.read(this, uri)?.let(ExternalFile::noteSeed)?.let {
            ImportedContent(it.body, it.createdAt, it.updatedAt)
        }
}

/**
 * The `UiModeManager` night mode that makes the system resolve this app — and
 * the starting window it draws before the app runs — the way [this] asks (#354).
 *
 * [ThemeMode.SYSTEM] maps to `MODE_NIGHT_AUTO` rather than to "leave the last
 * override in place", and that mapping is the part worth pinning: there is no
 * `getApplicationNightMode` and the documentation describes AUTO in terms of
 * location and sensors, so whether it releases an app-local override is not
 * something the API tells you. Measured on an API 36 emulator: after the Theme
 * had been set to Dark on a light phone, switching back to System returned the
 * cold-start splash to 229.6/255 average luma on a light system and 66.2 on a
 * dark one — i.e. following the device again in both directions.
 */
internal fun ThemeMode.toApplicationNightMode(): Int = when (this) {
    ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
    ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
    ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
}
