package com.markleaf.notes.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.markleaf.notes.core.security.PasscodeHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.markleafSettingsDataStore by preferencesDataStore(name = "markleaf_settings")

class AppSettingsRepository(
    private val context: Context
) {
    val settings: Flow<AppSettings> = context.markleafSettingsDataStore.data.map { preferences ->
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
            lockPasscodeSet = !preferences[LOCK_PASSCODE_HASH].isNullOrBlank()
        )
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setBiometricLockEnabled(enabled: Boolean) {
        context.markleafSettingsDataStore.edit { preferences ->
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
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[LOCK_PASSCODE_SALT] = salt
            preferences[LOCK_PASSCODE_HASH] = hash
        }
    }

    /** Remove the Locked-notes passcode. Callers should also unlock any locked
     *  notes so they aren't stranded behind a gate with no key. */
    suspend fun clearLockPasscode() {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences.remove(LOCK_PASSCODE_HASH)
            preferences.remove(LOCK_PASSCODE_SALT)
        }
    }

    /** True iff [passcode] matches the stored digest. False when no passcode is
     *  set. Hashing runs off the main thread. */
    suspend fun verifyLockPasscode(passcode: String): Boolean {
        val preferences = context.markleafSettingsDataStore.data.first()
        val salt = preferences[LOCK_PASSCODE_SALT] ?: return false
        val hash = preferences[LOCK_PASSCODE_HASH] ?: return false
        return withContext(Dispatchers.Default) {
            PasscodeHasher.verify(passcode, salt, hash)
        }
    }

    suspend fun setColorPalette(palette: ColorPalette) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[COLOR_PALETTE] = palette.name
        }
    }

    suspend fun setMarkdownSyntaxVisibility(visibility: MarkdownSyntaxVisibility) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[MARKDOWN_SYNTAX_VISIBILITY] = visibility.name
        }
    }

    suspend fun setLineWidth(lineWidth: EditorLineWidth) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[LINE_WIDTH] = lineWidth.name
        }
    }

    suspend fun setEditorFont(font: EditorFont) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[EDITOR_FONT] = font.name
        }
    }

    suspend fun setScreenshotProtection(enabled: Boolean) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[SCREENSHOT_PROTECTION] = enabled
        }
    }

    suspend fun setSyncFolderUri(uri: String?) {
        context.markleafSettingsDataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(SYNC_FOLDER_URI)
                preferences.remove(SYNC_LAST_SYNCED_AT)
            } else {
                preferences[SYNC_FOLDER_URI] = uri
            }
        }
    }

    suspend fun setSyncLastSyncedAt(epochMillis: Long) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[SYNC_LAST_SYNCED_AT] = epochMillis
        }
    }

    suspend fun setSyncFileExtension(extension: SyncFileExtension) {
        context.markleafSettingsDataStore.edit { preferences ->
            preferences[SYNC_FILE_EXTENSION] = extension.name
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
    }
}
