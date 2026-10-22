package com.markleaf.notes.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.EditorFont
import com.markleaf.notes.feature.lock.canUseBiometric
import com.markleaf.notes.ui.component.elapsedTimeLabel

/**
 * The line under a category's row on the Settings list: where its main
 * settings stand now, so the list answers "what is it set to" without a tap.
 *
 * Input: the category and the current settings.
 * Output: a few values joined with " · ".
 *
 * Why About shows the licence rather than the version: the list is in a
 * golden image, and a version there would change it on every release (#255).
 */
@Composable
internal fun settingsSummary(category: SettingsCategory, appSettings: AppSettings): String {
    val parts: List<String> = when (category) {
        SettingsCategory.APPEARANCE -> listOf(
            appSettings.themeMode.localizedLabel(),
            appSettings.colorPalette.localizedLabel()
        )
        SettingsCategory.EDITOR -> listOf(
            // A picked font reads as its own name, not "Your font".
            appSettings.customFontName
                ?.takeIf { appSettings.editorFont == EditorFont.CUSTOM }
                ?: appSettings.editorFont.localizedLabel(),
            appSettings.editorFontSize.localizedLabel(),
            appSettings.lineWidth.localizedLabel()
        )
        SettingsCategory.NOTES -> listOf(
            appSettings.notesLayout.localizedLabel(),
            appSettings.noteTitleSource.localizedLabel(),
            appSettings.openNotesAt.localizedLabel()
        )
        SettingsCategory.PRIVACY -> {
            val context = LocalContext.current
            val biometricAvailable = remember(context) { context.canUseBiometric() }
            listOfNotNull(
                stringResource(R.string.settings_summary_biometric)
                    .takeIf { appSettings.biometricLockEnabled && biometricAvailable },
                stringResource(R.string.locked_passcode_setting_title).takeIf { appSettings.lockPasscodeSet },
                stringResource(R.string.settings_summary_screenshots).takeIf { appSettings.screenshotProtection }
            ).ifEmpty { listOf(stringResource(R.string.settings_summary_no_locks)) }
        }
        SettingsCategory.SYNC -> {
            val folder = appSettings.syncFolderUri
            val synced = appSettings.syncLastSyncedAt
            if (folder.isNullOrBlank()) {
                listOf(stringResource(R.string.settings_summary_not_syncing))
            } else {
                listOfNotNull(
                    remember(folder) { humanReadableTreePath(folder) },
                    synced?.let { elapsedTimeLabel(it) }
                )
            }
        }
        SettingsCategory.ABOUT -> listOf(
            stringResource(R.string.settings_open_source),
            stringResource(R.string.oss_license_value)
        )
    }
    return parts.joinToString(" · ")
}
