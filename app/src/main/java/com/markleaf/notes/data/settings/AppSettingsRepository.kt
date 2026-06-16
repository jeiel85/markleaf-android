package com.markleaf.notes.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
            biometricLockEnabled = preferences[BIOMETRIC_LOCK_ENABLED] ?: false
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
        val SCREENSHOT_PROTECTION = booleanPreferencesKey("screenshot_protection")
        val SYNC_FOLDER_URI = stringPreferencesKey("sync_folder_uri")
        val SYNC_LAST_SYNCED_AT = longPreferencesKey("sync_last_synced_at")
        val SYNC_FILE_EXTENSION = stringPreferencesKey("sync_file_extension")
        val COLOR_PALETTE = stringPreferencesKey("color_palette")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val BIOMETRIC_LOCK_ENABLED = booleanPreferencesKey("biometric_lock_enabled")
    }
}
