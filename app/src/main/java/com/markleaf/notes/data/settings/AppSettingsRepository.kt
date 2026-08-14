package com.markleaf.notes.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.markleaf.notes.core.security.PasscodeBackoff
import com.markleaf.notes.core.security.PasscodeHasher
import com.markleaf.notes.core.text.NoteTitleSource
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.markleafSettingsDataStore by preferencesDataStore(name = "markleaf_settings")

/** Outcome of an unlock attempt against the "Locked notes" passcode (#156). */
sealed interface LockPasscodeAttempt {
    data object Success : LockPasscodeAttempt

    /** Wrong passcode. [retryAtMillis] is 0 while attempts are still free. */
    data class Wrong(val retryAtMillis: Long) : LockPasscodeAttempt

    /** Refused without checking — a backoff from earlier failures is still running. */
    data class LockedOut(val retryAtMillis: Long) : LockPasscodeAttempt
}

class AppSettingsRepository internal constructor(
    private val dataStore: DataStore<Preferences>
) {
    /** Production entry point — the process-wide DataStore singleton. The
     *  internal constructor exists so tests can hand in their own instance,
     *  which is what makes [attemptLockPasscode] testable at all: the
     *  `preferencesDataStore` delegate caches one instance per JVM, so
     *  Robolectric-per-method data dirs can never work with it (#158). */
    constructor(context: Context) : this(context.markleafSettingsDataStore)

    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            markdownSyntaxVisibility = preferences[MARKDOWN_SYNTAX_VISIBILITY]
                ?.let { value -> enumValueOrDefault(value, MarkdownSyntaxVisibility.SHOW) }
                ?: MarkdownSyntaxVisibility.SHOW,
            lineWidth = preferences[LINE_WIDTH]
                ?.let { value -> enumValueOrDefault(value, EditorLineWidth.COMFORTABLE) }
                ?: EditorLineWidth.COMFORTABLE,
            editorFont = preferences[EDITOR_FONT]
                ?.let { value -> enumValueOrDefault(value, EditorFont.SANS) }
                ?: EditorFont.SANS,
            screenshotProtection = preferences[SCREENSHOT_PROTECTION] ?: false,
            syncFolderUri = preferences[SYNC_FOLDER_URI],
            syncLastSyncedAt = preferences[SYNC_LAST_SYNCED_AT],
            syncFileExtension = preferences[SYNC_FILE_EXTENSION]
                ?.let { value -> enumValueOrDefault(value, SyncFileExtension.MD) }
                ?: SyncFileExtension.MD,
            colorPalette = preferences[COLOR_PALETTE]
                ?.let { value -> enumValueOrDefault(value, ColorPalette.MARKLEAF_GREEN) }
                ?: ColorPalette.MARKLEAF_GREEN,
            onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
            biometricLockEnabled = preferences[BIOMETRIC_LOCK_ENABLED] ?: false,
            lockPasscodeSet = !preferences[LOCK_PASSCODE_HASH].isNullOrBlank(),
            notesShowPreview = preferences[NOTES_SHOW_PREVIEW] ?: true,
            reopenLastNote = preferences[REOPEN_LAST_NOTE] ?: false,
            notesSortMode = preferences[NOTES_SORT_MODE]
                ?.let { value -> enumValueOrDefault(value, NotesSortMode.UPDATED_DESC) }
                ?: NotesSortMode.UPDATED_DESC,
            notesLayout = preferences[NOTES_LAYOUT]
                ?.let { value -> enumValueOrDefault(value, NotesLayout.LIST) }
                ?: NotesLayout.LIST,
            noteTitleSource = preferences[NOTE_TITLE_SOURCE]
                ?.let { value -> enumValueOrDefault(value, NoteTitleSource.FIRST_HEADING) }
                ?: NoteTitleSource.FIRST_HEADING,
            retitlePending = preferences[RETITLE_PENDING] ?: false,
            searchTitlesOnly = preferences[SEARCH_TITLES_ONLY] ?: false,
            lastOpenedNoteId = preferences[LAST_OPENED_NOTE_ID],
            openNotesInPreview = preferences[OPEN_NOTES_IN_PREVIEW] ?: false,
            openNotesAt = preferences[OPEN_NOTES_AT]
                ?.let { value -> enumValueOrDefault(value, OpenNotesAt.TOP) }
                ?: OpenNotesAt.TOP,
            syncMetadataMode = preferences[SYNC_METADATA_MODE]
                ?.let { value -> enumValueOrDefault(value, SyncMetadataMode.FRONTMATTER) }
                ?: SyncMetadataMode.FRONTMATTER,
            sidecarMigrationPending = preferences[SIDECAR_MIGRATION_PENDING] ?: false,
            syncDeviceId = preferences[SYNC_DEVICE_ID]
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        persist { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        persist { preferences ->
            preferences[BIOMETRIC_LOCK_ENABLED] = enabled
        }
    }

    /**
     * Set (or replace) the "Locked notes" passcode (#155). A fresh salt is drawn
     * per call and the PBKDF2 digest — never the passcode — is persisted. Hashing
     * runs off the main thread.
     */
    suspend fun setLockPasscode(passcode: String) {
        val salt = PasscodeHasher.newSaltBase64()
        val hash = withContext(Dispatchers.Default) { PasscodeHasher.hash(passcode, salt) }
        persist { preferences ->
            preferences[LOCK_PASSCODE_SALT] = salt
            preferences[LOCK_PASSCODE_HASH] = hash
            // A new passcode starts a clean streak; the owner just proved intent.
            preferences.remove(LOCK_FAILED_ATTEMPTS)
            preferences.remove(LOCK_RETRY_AT)
        }
    }

    /** Remove the Locked-notes passcode. Callers should also unlock any locked
     *  notes so they aren't stranded behind a gate with no key. */
    suspend fun clearLockPasscode() {
        persist { preferences ->
            preferences.remove(LOCK_PASSCODE_HASH)
            preferences.remove(LOCK_PASSCODE_SALT)
            preferences.remove(LOCK_FAILED_ATTEMPTS)
            preferences.remove(LOCK_RETRY_AT)
        }
    }

    /** True iff [passcode] matches the stored digest. False when no passcode is
     *  set. Hashing runs off the main thread. */
    suspend fun verifyLockPasscode(passcode: String): Boolean {
        val preferences = dataStore.data.first()
        val salt = preferences[LOCK_PASSCODE_SALT] ?: return false
        val hash = preferences[LOCK_PASSCODE_HASH] ?: return false
        return withContext(Dispatchers.Default) {
            PasscodeHasher.verify(passcode, salt, hash)
        }
    }

    /**
     * Wall-clock instant before which unlock attempts are refused, or 0 when no
     * backoff is active. Read on gate entry so a persisted lockout survives an
     * app restart (#156).
     */
    suspend fun lockPasscodeRetryAtMillis(): Long =
        dataStore.data.first()[LOCK_RETRY_AT] ?: 0L

    /**
     * Verify [passcode], applying and maintaining the [PasscodeBackoff] policy.
     * This is the only unlock entry point, so the lockout cannot be bypassed by
     * calling [verifyLockPasscode] directly from a new caller.
     */
    suspend fun attemptLockPasscode(
        passcode: String,
        now: Long = System.currentTimeMillis()
    ): LockPasscodeAttempt {
        val preferences = dataStore.data.first()
        val retryAt = preferences[LOCK_RETRY_AT] ?: 0L
        if (PasscodeBackoff.isLockedOut(retryAt, now)) return LockPasscodeAttempt.LockedOut(retryAt)

        if (verifyLockPasscode(passcode)) {
            resetLockPasscodeBackoff()
            return LockPasscodeAttempt.Success
        }

        val previousFailures = preferences[LOCK_FAILED_ATTEMPTS] ?: 0
        val nextRetryAt = PasscodeBackoff.retryAtAfterFailure(previousFailures, now)
        persist { edited ->
            edited[LOCK_FAILED_ATTEMPTS] = previousFailures + 1
            if (nextRetryAt > 0L) edited[LOCK_RETRY_AT] = nextRetryAt else edited.remove(LOCK_RETRY_AT)
        }
        return LockPasscodeAttempt.Wrong(nextRetryAt)
    }

    /** Clear the failure streak — on success, and whenever the passcode changes. */
    suspend fun resetLockPasscodeBackoff() {
        persist { preferences ->
            preferences.remove(LOCK_FAILED_ATTEMPTS)
            preferences.remove(LOCK_RETRY_AT)
        }
    }

    suspend fun setColorPalette(palette: ColorPalette) {
        persist { preferences ->
            preferences[COLOR_PALETTE] = palette.name
        }
    }

    suspend fun setMarkdownSyntaxVisibility(visibility: MarkdownSyntaxVisibility) {
        persist { preferences ->
            preferences[MARKDOWN_SYNTAX_VISIBILITY] = visibility.name
        }
    }

    suspend fun setLineWidth(lineWidth: EditorLineWidth) {
        persist { preferences ->
            preferences[LINE_WIDTH] = lineWidth.name
        }
    }

    suspend fun setEditorFont(font: EditorFont) {
        persist { preferences ->
            preferences[EDITOR_FONT] = font.name
        }
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        persist { preferences ->
            preferences[SCREENSHOT_PROTECTION] = enabled
        }
    }

    suspend fun setSyncFolderUri(uri: String?) {
        persist { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(SYNC_FOLDER_URI)
                preferences.remove(SYNC_LAST_SYNCED_AT)
            } else {
                preferences[SYNC_FOLDER_URI] = uri
            }
        }
    }

    suspend fun setSyncLastSyncedAt(epochMillis: Long) {
        persist { preferences ->
            preferences[SYNC_LAST_SYNCED_AT] = epochMillis
        }
    }

    suspend fun setSyncFileExtension(extension: SyncFileExtension) {
        persist { preferences ->
            preferences[SYNC_FILE_EXTENSION] = extension.name
        }
    }

    suspend fun setNotesShowPreview(show: Boolean) {
        persist { preferences ->
            preferences[NOTES_SHOW_PREVIEW] = show
        }
    }

    suspend fun setReopenLastNote(enabled: Boolean) {
        persist { preferences ->
            preferences[REOPEN_LAST_NOTE] = enabled
        }
    }

    suspend fun setNotesSortMode(mode: NotesSortMode) {
        persist { preferences ->
            preferences[NOTES_SORT_MODE] = mode.name
        }
    }

    suspend fun setNotesLayout(layout: NotesLayout) {
        persist { preferences ->
            preferences[NOTES_LAYOUT] = layout.name
        }
    }

    /**
     * Change which line becomes a note's title, and mark the retitle pass it
     * needs as in flight — in one write (#280, #262).
     *
     * Only preferences are written here; stored titles are recomputed by the
     * caller, because the notes database is not this repository's to touch.
     * The two keys go into a single edit on purpose: written separately, a
     * process death between them leaves the new rule selected with the pending
     * flag still false, so nothing resumes the pass and the list keeps titles
     * from two rules. That is precisely the window the flag exists to close,
     * and DataStore's `edit` is what makes the pair land or not land together.
     */
    suspend fun beginRetitle(source: NoteTitleSource) {
        persist { preferences ->
            preferences[NOTE_TITLE_SOURCE] = source.name
            preferences[RETITLE_PENDING] = true
        }
    }

    /** Marks whether a retitle pass is in flight, so a process death mid-pass
     *  can be resumed instead of leaving titles from two rules (#262). */
    suspend fun setRetitlePending(pending: Boolean) {
        persist { preferences ->
            preferences[RETITLE_PENDING] = pending
        }
    }

    suspend fun setSearchTitlesOnly(titlesOnly: Boolean) {
        persist { preferences ->
            preferences[SEARCH_TITLES_ONLY] = titlesOnly
        }
    }

    suspend fun setLastOpenedNoteId(noteId: String?) {
        persist { preferences ->
            if (noteId.isNullOrBlank()) {
                preferences.remove(LAST_OPENED_NOTE_ID)
            } else {
                preferences[LAST_OPENED_NOTE_ID] = noteId
            }
        }
    }

    suspend fun setOpenNotesInPreview(enabled: Boolean) {
        persist { preferences ->
            preferences[OPEN_NOTES_IN_PREVIEW] = enabled
        }
    }

    suspend fun setOpenNotesAt(where: OpenNotesAt) {
        persist { preferences ->
            preferences[OPEN_NOTES_AT] = where.name
        }
    }

    suspend fun setSyncMetadataMode(mode: SyncMetadataMode) {
        persist { preferences ->
            preferences[SYNC_METADATA_MODE] = mode.name
        }
    }

    /**
     * Select sidecar mode and mark its folder conversion as owed, in one write
     * (#262).
     *
     * The mode is selected *before* the conversion runs, because a half-done
     * folder holds files with no header, and only sidecar mode can identify
     * those — see `SidecarMigration.toSidecar`. That ordering needs a companion:
     * with the mode already switched, the settings screen's own "you are already
     * in this mode" guard would refuse to run the conversion again, so a pass
     * that died would never be finished. The flag is what the screen resumes
     * from, and it goes in the same edit as the mode for the reason
     * [beginRetitle] does: written separately, a death between the two leaves
     * the mode switched with nothing recording that the folder is unconverted.
     */
    suspend fun beginSidecarMigration() {
        persist { preferences ->
            preferences[SYNC_METADATA_MODE] = SyncMetadataMode.SIDECAR.name
            preferences[SIDECAR_MIGRATION_PENDING] = true
        }
    }

    /** Clears the flag [beginSidecarMigration] set, once the pass has finished. */
    suspend fun setSidecarMigrationPending(pending: Boolean) {
        persist { preferences ->
            preferences[SIDECAR_MIGRATION_PENDING] = pending
        }
    }

    /**
     * This install's device id, generating and persisting one on first call.
     *
     * Only ever used to name the sidecar index file this device owns (#216), so
     * that two devices syncing one folder never write the same file. A random
     * UUID's first segment is short enough to keep the filename readable and
     * far more than enough to keep a handful of devices apart.
     */
    suspend fun getOrCreateSyncDeviceId(): String {
        val existing = settings.first().syncDeviceId
        if (!existing.isNullOrBlank()) return existing
        val generated = UUID.randomUUID().toString().substringBefore('-')
        persist { preferences ->
            // Re-check inside the edit: two callers racing must agree on one id,
            // or the folder collects an orphan index per loser.
            if (preferences[SYNC_DEVICE_ID].isNullOrBlank()) {
                preferences[SYNC_DEVICE_ID] = generated
            }
        }
        return settings.first().syncDeviceId ?: generated
    }

    /**
     * Every write funnels through here inside [NonCancellable]. Settings writes
     * are typically launched from a screen's `rememberCoroutineScope`, and a
     * rotation cancelling that scope mid-write must not silently drop a toggle
     * the user just flipped (#195). Ordering is preserved — this stays a
     * suspend call on the caller's dispatcher, only shielded from cancellation.
     */
    private suspend fun persist(transform: suspend (MutablePreferences) -> Unit) {
        withContext(NonCancellable) {
            dataStore.edit(transform)
        }
    }

    private fun <T : Enum<T>> enumValueOrDefault(value: String, default: T): T {
        return runCatching {
            java.lang.Enum.valueOf(default.declaringJavaClass, value)
        }.getOrDefault(default)
    }

    private companion object {
        val MARKDOWN_SYNTAX_VISIBILITY = stringPreferencesKey("markdown_syntax_visibility")
        val LINE_WIDTH = stringPreferencesKey("line_width")
        val EDITOR_FONT = stringPreferencesKey("editor_font")
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val SYNC_FOLDER_URI = stringPreferencesKey("sync_folder_uri")
        val SYNC_LAST_SYNCED_AT = longPreferencesKey("sync_last_synced_at")
        val SYNC_FILE_EXTENSION = stringPreferencesKey("sync_file_extension")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
        val LOCK_PASSCODE_HASH = stringPreferencesKey("lock_passcode_hash")
        val LOCK_PASSCODE_SALT = stringPreferencesKey("lock_passcode_salt")
        val LOCK_FAILED_ATTEMPTS = intPreferencesKey("lock_failed_attempts")
        val LOCK_RETRY_AT = longPreferencesKey("lock_retry_at")
        val NOTES_SHOW_PREVIEW = booleanPreferencesKey("notes_show_preview")
        val REOPEN_LAST_NOTE = booleanPreferencesKey("reopen_last_note")
        val NOTES_SORT_MODE = stringPreferencesKey("notes_sort_mode")
        val NOTES_LAYOUT = stringPreferencesKey("notes_layout")
        val NOTE_TITLE_SOURCE = stringPreferencesKey("note_title_source")
        val RETITLE_PENDING = booleanPreferencesKey("retitle_pending")
        val SEARCH_TITLES_ONLY = booleanPreferencesKey("search_titles_only")
        val LAST_OPENED_NOTE_ID = stringPreferencesKey("last_opened_note_id")
        val OPEN_NOTES_IN_PREVIEW = booleanPreferencesKey("open_notes_in_preview")
        val OPEN_NOTES_AT = stringPreferencesKey("open_notes_at")
        val SYNC_METADATA_MODE = stringPreferencesKey("sync_metadata_mode")
        val SIDECAR_MIGRATION_PENDING = booleanPreferencesKey("sidecar_migration_pending")
        val SYNC_DEVICE_ID = stringPreferencesKey("sync_device_id")
    }
}
