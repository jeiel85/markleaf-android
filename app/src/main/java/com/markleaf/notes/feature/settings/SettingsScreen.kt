package com.markleaf.notes.feature.settings

import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.BuildConfig
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.core.text.NoteTitleSource
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.repository.LocalTagRepository
import com.markleaf.notes.data.repository.NoteRetitler
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.core.font.CustomFontStore
import com.markleaf.notes.data.settings.SyncMetadataMode
import com.markleaf.notes.data.settings.WidgetOpacity
import com.markleaf.notes.data.sync.NoteFolderMirror
import com.markleaf.notes.data.sync.NoteImporter
import com.markleaf.notes.data.sync.SidecarMigration
import com.markleaf.notes.data.sync.syncFolderUriOrNull
import com.markleaf.notes.data.sync.mirrorMetadata
import com.markleaf.notes.feature.sync.rememberSyncFolderLinker
import com.markleaf.notes.util.ExportAllNotes
import com.markleaf.notes.util.TagParser
import com.markleaf.notes.widget.WidgetRefresh
import java.text.NumberFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.key

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacyClick: () -> Unit = {},
    onSyncCenterClick: () -> Unit = {},
    initialSection: SettingsShortcut? = null,
    initialItem: SettingsItem? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    val db = remember { AppDatabase.getInstance(context.applicationContext) }
    val noteRepository = remember { LocalNoteRepository(db) }
    val tagRepository = remember { LocalTagRepository(db) }
    val exportTags by tagRepository.observeVisibleTags().collectAsState(initial = emptyList())
    // rememberSaveable, not remember: OpenDocumentTree can outlive an activity
    // recreation (rotation, background process reclamation), and its callback
    // fires against the recomposed launcher — a plain remember would reset this
    // to null first and export every unlocked note instead of the chosen tag.
    var exportTag by rememberSaveable { mutableStateOf<String?>(null) }
    var showExportTagPicker by remember { mutableStateOf(false) }
    val noteImporter = remember { NoteImporter(db) }
    // Switching metadata mode rewrites every file in the folder. Disabling the
    // control while it runs stops a second switch starting on top of the first,
    // which would have two passes rewriting the same files.
    var metadataSwitchBusy by remember { mutableStateOf(false) }
    // A conversion to sidecar mode selects the mode first, so a pass that died
    // mid-way comes back with the mode already switched — and the "you are
    // already in this mode" guard below would then refuse to finish the folder,
    // leaving those files carrying headers for good (the import only strips them
    // in memory). The flag `beginSidecarMigration` set is what this resumes from
    // (#262).
    LaunchedEffect(appSettings.sidecarMigrationPending, metadataSwitchBusy) {
        val uri = appSettings.syncFolderUriOrNull()
        if (!appSettings.sidecarMigrationPending || metadataSwitchBusy || uri == null) {
            return@LaunchedEffect
        }
        metadataSwitchBusy = true
        try {
            withContext(Dispatchers.IO + NonCancellable) {
                SidecarMigration.toSidecar(
                    context,
                    uri,
                    settingsRepository.getOrCreateSyncDeviceId()
                )
            }
        } finally {
            // Same order as the retitle resume: the flag goes down before the
            // guard, or the gap between them satisfies this effect again.
            settingsRepository.setSidecarMigrationPending(false)
            metadataSwitchBusy = false
        }
    }
    // Same reason for the title rule (#280): two retitle passes running over the
    // same rows would leave titles from both rules behind.
    var retitleBusy by remember { mutableStateOf(false) }
    // A retitle pass that died mid-way (process death between writing the
    // preference and finishing the pass) leaves the new rule selected with part
    // of the list still under the old one (#262). The preference carries a
    // pending flag; coming back here resumes the pass and clears it.
    LaunchedEffect(appSettings.retitlePending, retitleBusy) {
        if (appSettings.retitlePending && !retitleBusy) {
            retitleBusy = true
            try {
                val changed = withContext(Dispatchers.IO + NonCancellable) {
                    NoteRetitler.retitleAll(noteRepository, appSettings.noteTitleSource)
                }
                Toast.makeText(
                    context,
                    context.resources.getQuantityString(
                        R.plurals.note_title_retitled_format,
                        changed,
                        changed
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                settingsRepository.setRetitlePending(false)
                retitleBusy = false
            }
        }
    }
    // #510: a font file of the reader's own. The picker needs no storage
    // permission; the file is copied into app storage and checked before the
    // writing surface switches to it.
    val customFontLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { fontUri ->
        if (fontUri != null) {
            scope.launch {
                // Import, record and clean up as one replacement, serialised
                // with any other pick still in flight. A large file can take a
                // while; if the reader picks Sans/Serif/Monospace meanwhile,
                // that later choice wins and the new file is only recorded
                // (#513 review).
                val fontAtPick = appSettings.editorFont
                val result = CustomFontStore.replace(context, fontUri) { imported ->
                    val unchanged = settingsRepository.settings.first().editorFont == fontAtPick
                    settingsRepository.setCustomFont(imported.fileName, imported.displayName, select = unchanged)
                }
                when (result) {
                    is CustomFontStore.ImportResult.Imported -> Unit
                    CustomFontStore.ImportResult.NotAFont ->
                        Toast.makeText(context, R.string.font_custom_not_a_font, Toast.LENGTH_LONG).show()
                    CustomFontStore.ImportResult.TooLarge ->
                        Toast.makeText(context, R.string.font_custom_too_large, Toast.LENGTH_LONG).show()
                    CustomFontStore.ImportResult.Unreadable ->
                        Toast.makeText(context, R.string.font_custom_unreadable, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    val exportAllLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { folderUri ->
        if (folderUri != null) {
            scope.launch {
                val selectedTag = exportTag
                exportTag = null
                val notes = withContext(Dispatchers.IO) {
                    ExportAllNotes.selectNotes(noteRepository.getAllNotes(), selectedTag)
                }
                val count = withContext(Dispatchers.IO) {
                    ExportAllNotes.exportAllNotes(context, folderUri, notes)
                }
                val msg = context.resources.getQuantityString(R.plurals.export_all_done_format, count, count)
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (showExportTagPicker) {
        AlertDialog(
            onDismissRequest = { showExportTagPicker = false },
            title = { Text(stringResource(R.string.export_tag_notes)) },
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                    items(exportTags, key = { it.id }) { tag ->
                        TextButton(onClick = {
                            exportTag = TagParser.normalizeTagName(tag.name)
                            showExportTagPicker = false
                            exportAllLauncher.launch(null)
                        }) { Text(tag.name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showExportTagPicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Picker, the question about files already in the folder, and the link
    // itself all live in one place now — the Sync Center offers the same
    // button, and the import used to be missing from both copies (#372).
    val pickSyncFolder = rememberSyncFolderLinker(
        settingsRepository = settingsRepository,
        noteRepository = noteRepository,
        noteImporter = noteImporter,
        appSettings = appSettings
    )

    // The title rule is chosen on the Notes page, but its pass shares
    // `retitleBusy` with the resume effect above, so it starts here.
    val changeNoteTitleSource: (NoteTitleSource) -> Unit = { source ->
        retitleBusy = true
        scope.launch {
            // Stored titles are derived once, at save
            // time, so without this pass the setting
            // would appear to do nothing (#280).
            // NonCancellable: leaving the screen
            // mid-pass would otherwise strand half the
            // notes under the old rule. The rule and
            // its pending flag are written together —
            // a death between two writes would select
            // the rule with nothing to resume — and
            // the flag survives a process death
            // mid-pass, so this screen's
            // LaunchedEffect resumes it next time
            // (#262).
            settingsRepository.beginRetitle(source)
            try {
                val changed = withContext(
                    Dispatchers.IO + NonCancellable
                ) {
                    NoteRetitler.retitleAll(noteRepository, source)
                }
                Toast.makeText(
                    context,
                    context.resources.getQuantityString(
                        R.plurals.note_title_retitled_format,
                        changed,
                        changed
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Order matters, and it is the same
                // order the resume effect uses: the
                // busy guard is what stops that effect
                // seeing `retitlePending && !busy` and
                // launching a second full pass, so it
                // must outlive the pending flag rather
                // than be released while the flag is
                // still true (#262).
                settingsRepository.setRetitlePending(false)
                retitleBusy = false
            }
        }
    }

    // Which page is open: null is the list of sections. Saveable, so a
    // rotation, or a picker that outlives the activity, comes back to the same
    // page.
    var openSection by rememberSaveable { mutableStateOf(initialSection) }
    // The setting the page lands on (a search result, or the item a deep link
    // asked for) and whether the landing is still owed. A page that only
    // recomposes, or comes back from a rotation, does not take focus again.
    var landingItem by rememberSaveable { mutableStateOf(initialItem) }
    var landingPending by rememberSaveable { mutableStateOf(initialSection != null) }
    val openPage: (SettingsShortcut, SettingsItem?) -> Unit = { section, item ->
        openSection = section
        landingItem = item
        landingPending = true
    }
    // A page opened straight from elsewhere (the Locked space asking for a
    // passcode) goes back to where it was opened from: that reader never saw
    // the list.
    val backReturnsToList = openSection != null && initialSection == null
    val closePage = {
        openSection = null
        landingItem = null
        landingPending = false
    }
    BackHandler(enabled = backReturnsToList) { closePage() }

    // #517: the list gets you to a section; search gets you to the setting
    // when you know its name but not which section holds it.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = openSection == null && searchQuery.isNotEmpty()) { searchQuery = "" }
    val resources = context.resources
    val configuration = LocalConfiguration.current
    // Resolved again when the language changes, not on every keystroke.
    val searchEntries = remember(configuration) {
        val percent = NumberFormat.getPercentInstance(configuration.locales[0])
        SettingsSearch.entries(
            text = resources::getString,
            sectionTitle = { resources.getString(it.headingRes()) },
            include = { it != SettingsItem.UPDATE_CHECK || BuildConfig.UPDATER },
            extraTexts = { item ->
                if (item == SettingsItem.WIDGET_OPACITY) {
                    WidgetOpacity.entries.map { percent.format(it.percent / 100.0) }
                } else {
                    emptyList()
                }
            }
        )
    }
    val searchResults = remember(searchQuery, searchEntries) {
        SettingsSearch.search(searchQuery, searchEntries)
    }

    val titleFocus = remember { FocusRequester() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val section = openSection
                    if (section == null) {
                        Text(stringResource(R.string.settings))
                    } else {
                        // The page's heading, and where a jump to the section
                        // puts focus.
                        Text(
                            text = stringResource(section.label),
                            modifier = Modifier
                                .semantics { heading() }
                                .focusRequester(titleFocus)
                                .focusable()
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (backReturnsToList) closePage() else onBack() }) {
                        Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            val section = openSection
            if (section == null) {
                SettingsHome(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    results = searchResults,
                    onOpenSection = { openPage(it, null) },
                    onPickResult = { entry ->
                        searchQuery = ""
                        openPage(entry.section, entry.item)
                    }
                )
            } else {
                // Each page starts at its own top, not at the last page's
                // scroll position.
                key(section) {
                    SettingsPage(
                        landingPending = landingPending,
                        landingItem = landingItem,
                        titleFocus = titleFocus,
                        onLanded = {
                            landingPending = false
                            landingItem = null
                        }
                    ) { anchors ->
                        when (section) {
                            SettingsShortcut.APPEARANCE ->
                                AppearanceSettings(appSettings, settingsRepository, scope, anchors)
                            SettingsShortcut.MARKDOWN -> MarkdownSettings(
                                appSettings = appSettings,
                                settingsRepository = settingsRepository,
                                scope = scope,
                                anchors = anchors,
                                onPickCustomFont = { customFontLauncher.launch(CustomFontMimeTypes) }
                            )
                            SettingsShortcut.NOTES -> NotesSettings(
                                appSettings = appSettings,
                                settingsRepository = settingsRepository,
                                scope = scope,
                                db = db,
                                retitleBusy = retitleBusy,
                                onNoteTitleSourceChange = changeNoteTitleSource,
                                anchors = anchors
                            )
                            SettingsShortcut.PRIVACY -> PrivacySettings(
                                appSettings = appSettings,
                                settingsRepository = settingsRepository,
                                scope = scope,
                                noteRepository = noteRepository,
                                onPrivacyClick = onPrivacyClick,
                                anchors = anchors
                            )
                            SettingsShortcut.DATA -> DataSettings(
                                exportTagsAvailable = exportTags.isNotEmpty(),
                                onExportAll = {
                                    exportTag = null
                                    exportAllLauncher.launch(null)
                                },
                                onExportTag = { showExportTagPicker = true },
                                anchors = anchors
                            )
                            SettingsShortcut.SYNC ->
                                SyncSection(
                                    itemAnchor = { item, focusable -> anchors.item(item, focusable) },
                                    folderUri = appSettings.syncFolderUri,
                                    lastSyncedAt = appSettings.syncLastSyncedAt,
                                    metadataMode = appSettings.syncMetadataMode,
                                    metadataBusy = metadataSwitchBusy,
                                    onMetadataModeChange = { mode ->
                                        if (mode == appSettings.syncMetadataMode) return@SyncSection
                                        val uri = appSettings.syncFolderUriOrNull()
                                        if (uri == null) {
                                            // No folder yet: no files carry a header and no
                                            // index exists, so there is nothing to migrate
                                            // and the setting is the whole change. This is
                                            // the cheap moment to choose, and the only one
                                            // that costs nothing.
                                            scope.launch {
                                                // Sidecar mode needs a device id to name the
                                                // index it owns; without one mirrorMetadata()
                                                // falls back to Frontmatter and the choice
                                                // would be silently undone at link time.
                                                if (mode == SyncMetadataMode.SIDECAR) {
                                                    settingsRepository.getOrCreateSyncDeviceId()
                                                }
                                                settingsRepository.setSyncMetadataMode(mode)
                                            }
                                            return@SyncSection
                                        }
                                        scope.launch {
                                            metadataSwitchBusy = true
                                            // The two directions flip the setting at
                                            // opposite ends of the conversion, and the rule
                                            // behind both is the same: a mirror file with no
                                            // header must never be read by a mode that has
                                            // only the header to identify it by, or the
                                            // import mints a fresh id and the note comes
                                            // back as a second copy (#140).
                                            //
                                            // To SIDECAR, flip first. A pass that dies
                                            // halfway leaves stripped files behind, and the
                                            // frontmatter import has nothing to match them
                                            // with; the sidecar import has the index entry,
                                            // which `toSidecar` makes durable before it
                                            // strips anything. The older comment here said
                                            // the opposite order was needed because an
                                            // import in that window "would read each header
                                            // as note text" — that stopped being true when
                                            // the sidecar import learned to strip a stray
                                            // header and use the id inside it
                                            // (`NoteFolderMirror`, "A file may still carry a
                                            // header").
                                            //
                                            // To FRONTMATTER, flip last, for the mirror
                                            // reason: the conversion is what *adds* the
                                            // header, so until it has run the files are
                                            // identifiable only by the sidecar index, which
                                            // only the sidecar mode reads.
                                            // Selecting the mode also records that the folder
                                            // conversion is owed, in one write — otherwise a
                                            // death between the two leaves the mode switched
                                            // with nothing saying the folder is unconverted,
                                            // and the guard above this block refuses to run
                                            // it again.
                                            if (mode == SyncMetadataMode.SIDECAR) {
                                                settingsRepository.beginSidecarMigration()
                                            }
                                            val result = withContext(Dispatchers.IO) {
                                                val deviceId = settingsRepository.getOrCreateSyncDeviceId()
                                                when (mode) {
                                                    SyncMetadataMode.SIDECAR ->
                                                        SidecarMigration.toSidecar(context, uri, deviceId)
                                                    SyncMetadataMode.FRONTMATTER ->
                                                        SidecarMigration.toFrontmatter(
                                                            context,
                                                            uri,
                                                            deviceId,
                                                            noteRepository.getAllNotes()
                                                        )
                                                }
                                            }
                                            if (mode == SyncMetadataMode.FRONTMATTER) {
                                                settingsRepository.setSyncMetadataMode(mode)
                                            } else {
                                                settingsRepository.setSidecarMigrationPending(false)
                                            }
                                            metadataSwitchBusy = false
                                            Toast.makeText(
                                                context,
                                                context.getString(
                                                    R.string.sync_metadata_switched_format,
                                                    result.converted
                                                ),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onPickFolder = pickSyncFolder,
                                    onSyncNow = {
                                        val uri = appSettings.syncFolderUriOrNull() ?: return@SyncSection
                                        scope.launch {
                                            val notes = withContext(Dispatchers.IO) {
                                                // Full set (incl. trashed/archived) so a hidden note
                                                // isn't re-imported as new — see #148.
                                                noteRepository.getAllNotes()
                                            }
                                            val result = withContext(Dispatchers.IO) {
                                                NoteFolderMirror.importChanges(
                                                    context = context,
                                                    folderUri = uri,
                                                    existing = notes,
                                                    applyUpdate = { updated ->
                                                        noteImporter.update(updated)
                                                    },
                                                    applyCreate = { created ->
                                                        noteImporter.create(created)
                                                    },
                                                    currentNote = { id -> noteImporter.current(id) },
                                                    metadata = appSettings.mirrorMetadata(),
                                                    titleSource = appSettings.noteTitleSource
                                                )
                                            }
                                            // A placed widget hears about an import no
                                            // other way (#262).
                                            if (result.changedAnything()) WidgetRefresh.notesChanged(context)
                                            settingsRepository.setSyncLastSyncedAt(System.currentTimeMillis())
                                            val msg = if (result.conflicts > 0) {
                                                context.getString(
                                                    R.string.sync_done_with_conflicts_format,
                                                    result.updated,
                                                    result.created,
                                                    result.conflicts,
                                                    result.skipped
                                                )
                                            } else {
                                                context.getString(
                                                    R.string.sync_done_format,
                                                    result.updated,
                                                    result.created,
                                                    result.skipped
                                                )
                                            }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onStopSync = {
                                        scope.launch {
                                            settingsRepository.setSyncFolderUri(null)
                                            Toast.makeText(
                                                context,
                                                R.string.sync_stopped,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    onSyncCenterClick = onSyncCenterClick
                                )
                            SettingsShortcut.OPEN_SOURCE -> OpenSourceSettings(anchors)
                            SettingsShortcut.APP -> AppInfoSettings(anchors)
                        }
                    }
                }
            }
        }
    }
}

/**
 * What the font picker offers. Providers disagree on a font's MIME type — many
 * file managers report `.ttf` as `application/octet-stream` — so the generic
 * binary type is included, and [CustomFontStore] checks the file itself.
 */
private val CustomFontMimeTypes = arrayOf(
    "font/ttf",
    "font/otf",
    "font/sfnt",
    "font/collection",
    "application/x-font-ttf",
    "application/x-font-otf",
    "application/font-sfnt",
    "application/vnd.ms-opentype",
    "application/octet-stream"
)
