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
import com.markleaf.notes.data.settings.ThemeMode
import com.markleaf.notes.data.settings.WidgetOpacity
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.NoteImporter
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.feature.lock.BiometricLockGate
import com.markleaf.notes.feature.onboarding.WelcomeOnboardingSheet
import com.markleaf.notes.navigation.MarkleafNavHost
import com.markleaf.notes.shortcut.LauncherShortcuts
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.theme.rememberBodyFontFamily
import com.markleaf.notes.ui.viewmodel.MarkleafViewModelFactory
import com.markleaf.notes.util.ExternalFile
import com.markleaf.notes.widget.QuickNoteWidget
import com.markleaf.notes.widget.WidgetPaletteStore
import com.markleaf.notes.widget.WidgetRefresh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

class MainActivity : FragmentActivity() {
    // For the launcher-shortcut pass in onStop, which runs after onCreate's locals are gone.
    private val shortcutNotes by lazy { LocalNoteRepository(AppDatabase.getInstance(applicationContext)) }
    private val shortcutSettings by lazy { AppSettingsRepository(applicationContext) }

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
                val result = withContext(Dispatchers.IO) {
                    NoteFolderMirror.importChanges(
                        context = applicationContext,
                        folderUri = uri,
                        existing = notes,
                        applyUpdate = { updated -> importer.update(updated) },
                        applyCreate = { created -> importer.create(created) },
                        currentNote = { id -> importer.current(id) },
                        metadata = settings.mirrorMetadata(),
                        titleSource = settings.noteTitleSource
                    )
                }
                // The import can finish after the onPause that would otherwise
                // have covered it, and a placed widget has no other way to hear
                // that a note it shows was rewritten from the folder (#262).
                if (result.changedAnything()) WidgetRefresh.notesChanged(applicationContext)
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

        // Mirror the Appearance settings where the widgets can read them, and
        // repaint the widgets when any of them changes (#375, #469). A widget
        // is drawn by a receiver on its main thread and cannot wait on
        // DataStore, so the values have to be pushed to it rather than pulled —
        // WidgetPaletteStore is that copy.
        //
        // Theme travels with Colors because the widget needs to know which end
        // of the dynamic palette to take, and its own Configuration cannot be
        // trusted to say: #354's night mode reaches the process through
        // UiModeManager at a moment this does not control.
        //
        // Collected from the repository rather than from the Compose state
        // above: `collectAsState` starts at `AppSettings()`, whose palette is
        // the green default, so a Material You user's widgets would repaint
        // green and then correct themselves on every launch. This flow emits
        // only what is stored.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                settingsRepository.settings
                    .map { settings ->
                        WidgetAppearance(
                            settings.colorPalette,
                            settings.themeMode,
                            settings.widgetOpacity,
                            settings.widgetCustomColor
                        )
                    }
                    .distinctUntilChanged()
                    .collect { appearance ->
                        // save() reports whether the values actually moved; they
                        // arrive once per process whether or not anyone touched
                        // them, and repainting every widget on each launch would
                        // be work for nothing. It commits, so it runs off the
                        // main thread this collector is on.
                        val changed = withContext(Dispatchers.IO) {
                            WidgetPaletteStore.save(
                                applicationContext,
                                appearance.palette,
                                appearance.themeMode,
                                appearance.opacity,
                                appearance.customColor
                            )
                        }
                        if (changed) {
                            WidgetRefresh.notesChanged(applicationContext)
                        }
                    }
            }
        }

        // The launcher's long-press menu (F1): new note and search always, and the
        // two most recently edited notes when the user has turned that on. Only
        // while started, because every note change happens in this activity; the
        // pinned shortcuts a user dragged out are re-checked on each change too, so
        // a note locked or trashed here stops being reachable from the home screen.
        lifecycleScope.launch {
            val noteRepository = LocalNoteRepository(database)
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                LauncherShortcuts.follow(
                    context = applicationContext,
                    recentEnabled = settingsRepository.settings
                        .map { it.recentNotesInShortcuts }
                        .distinctUntilChanged(),
                    notes = noteRepository.observeNotes(),
                    lookUp = { id -> withContext(Dispatchers.IO) { noteRepository.getNote(id) } }
                )
            }
        }

        // The launching intent is a one-shot request: a widget tap, a share, a file
        // to open. A recreation — rotation, a theme change, the process coming back —
        // hands the activity the same intent again, and acting on it a second time
        // makes another note out of a share, or throws the screen you were writing
        // on over to a fresh blank one. Whether it has been acted on travels in the
        // saved state, so only the first instance reads it.
        //
        // The same goes for a launch with no request at all: the host's plain-launch
        // fallback ("Reopen last note on launch") is part of the one dispatch, so a
        // recreation must skip it too — otherwise the last note is pushed on top of
        // the back stack the activity just restored. An acted-on launch therefore
        // tells the host not to dispatch anything (dispatchLaunchRequest = false),
        // rather than handing it an empty request that reads as a plain launch.
        //
        // "Acted on" is set by the host when it dispatches (onEntryDispatched below),
        // not here. With App lock on, the host is not composed until the user
        // authenticates, so an activity recreated behind the prompt has dispatched
        // nothing yet — marking the intent consumed now would lose the request.
        val entryIntent = intent.unlessConsumedBy(savedInstanceState)
        val launchAlreadyDispatched = entryIntent == null
        entryIntentConsumed = launchAlreadyDispatched
        val shouldCreateNote = entryIntent?.requestsNewNote() == true
        val openSearch = entryIntent?.action == LauncherShortcuts.ACTION_SEARCH
        if (entryIntent != null) LauncherShortcuts.reportUsed(applicationContext, entryIntent)
        val openNoteId = if (entryIntent?.action == QuickNoteWidget.ACTION_OPEN_NOTE) {
            entryIntent.getStringExtra(QuickNoteWidget.EXTRA_NOTE_ID)
        } else null
        val sharedContent = extractInitialContent(entryIntent)
        // A file opened from elsewhere (ACTION_VIEW) is shown for reading rather
        // than imported (#326); sharing one in (ACTION_SEND) still means "take
        // this", and keeps creating a note.
        val viewFileUri = entryIntent?.takeIf { it.action == Intent.ACTION_VIEW }?.data?.toString()

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
                bodyFontFamily = rememberBodyFontFamily(appSettings)
            ) {
                BiometricLockGate(enabled = appSettings.biometricLockEnabled) {
                    val navController = rememberNavController()
                    MarkleafNavHost(
                        navController = navController,
                        windowSizeClass = windowSizeClass,
                        viewModelFactory = viewModelFactory,
                        shouldCreateNote = shouldCreateNote,
                        openSearch = openSearch,
                        sharedText = sharedContent?.body,
                        sharedCreatedAt = sharedContent?.createdAt,
                        sharedUpdatedAt = sharedContent?.updatedAt,
                        openNoteId = openNoteId,
                        viewFileUri = viewFileUri,
                        dispatchLaunchRequest = !launchAlreadyDispatched,
                        onEntryDispatched = { entryIntentConsumed = true }
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
        //
        // A full update of both kinds, rather than the bare
        // notifyAppWidgetViewDataChanged this used to be. That call reloads the
        // recent-notes *rows*, which read the palette, while the container that
        // carries the background is the provider's — so the pair could drift
        // apart into light text on a light background (#375). The single-note
        // widgets redraw rather than reload a list, and each re-checks that its
        // note may still be shown: one moved into the Locked space while the app
        // was open must stop rendering (#351).
        WidgetRefresh.notesChanged(applicationContext)
    }

    override fun onStop() {
        super.onStop()
        // The started-only collector in onCreate debounces, and stopping cancels a
        // pending update; this last pass makes sure a note locked or trashed just
        // before leaving is off the launcher by the time the home screen shows.
        LauncherShortcuts.syncWhenLeaving(
            context = applicationContext,
            recentEnabled = { shortcutSettings.settings.first().recentNotesInShortcuts },
            notes = { shortcutNotes.observeNotes().first() },
            lookUp = { id -> shortcutNotes.getNote(id) }
        )
    }

    // androidx.activity 1.9 tightened this override to a non-null Intent (it
    // mirrors the platform's @NonNull annotation), so the parameter and the
    // body's former null-safe calls are now plain non-null accesses.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val isEntryRequest = when {
            intent.requestsNewNote() -> true
            intent.action == QuickNoteWidget.ACTION_OPEN_NOTE -> true
            intent.action == LauncherShortcuts.ACTION_SEARCH -> true
            intent.action == Intent.ACTION_SEND &&
                (intent.streamUri() != null || extractSharedText(intent) != null) -> true
            intent.action == Intent.ACTION_VIEW && intent.data != null -> true
            else -> false
        }
        if (isEntryRequest) {
            setIntent(intent)
            // A new request, not yet acted on: the instance recreate() builds reads
            // this from the saved state and dispatches it, once.
            entryIntentConsumed = false
            recreate()
        }
    }

    /**
     * True once the launching intent has been acted on, so a recreated activity —
     * which is handed the same intent again — does not act on it twice. Saved and
     * restored with the rest of the instance state.
     */
    private var entryIntentConsumed = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(STATE_ENTRY_INTENT_CONSUMED, entryIntentConsumed)
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

/**
 * The system's "create a note" action (`Intent.ACTION_CREATE_NOTE`, Android 14).
 * Spelled out as a string because the constant is API 34 and `minSdk` is 26; the
 * value is fixed by the platform, and on an older Android nothing ever sends it.
 */
internal const val ACTION_CREATE_NOTE_SYSTEM = "android.intent.action.CREATE_NOTE"

/**
 * True when this intent asks for a fresh, empty note: the widget's "+" button, or
 * the system's Notes-role action (#481) that a stylus button or a Quick Settings
 * tile sends once Markleaf is chosen as the Notes app. Both open the same blank
 * editor, so they share one answer here rather than two copies of the check.
 */
internal fun Intent.requestsNewNote(): Boolean =
    action == QuickNoteWidget.ACTION_CREATE_NOTE || action == ACTION_CREATE_NOTE_SYSTEM

/** The settings a widget paints itself from, compared as one value so a change to any repaints. */
private data class WidgetAppearance(
    val palette: ColorPalette,
    val themeMode: ThemeMode,
    val opacity: WidgetOpacity,
    val customColor: Int?
)

/** The saved-state key that records the launching intent has been acted on. */
internal const val STATE_ENTRY_INTENT_CONSUMED = "entry_intent_consumed"

/**
 * This launching intent, unless the instance state says an earlier instance of the
 * activity already acted on it — the case for every recreation: rotation, a theme
 * change, the process coming back. A `null` [saved] is a first launch, and a saved
 * `false` is what [MainActivity.onNewIntent] leaves for a request that has not been
 * acted on yet, so both hand the intent through.
 */
internal fun Intent.unlessConsumedBy(saved: Bundle?): Intent? =
    takeUnless { saved?.getBoolean(STATE_ENTRY_INTENT_CONSUMED) == true }
