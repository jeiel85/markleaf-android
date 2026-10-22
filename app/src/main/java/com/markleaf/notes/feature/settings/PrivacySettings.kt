package com.markleaf.notes.feature.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.feature.lock.canUseBiometric
import com.markleaf.notes.util.HapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope

/** The Privacy & security page: what is promised, what is locked, what is hidden. */
@Composable
internal fun PrivacySettings(
    appSettings: AppSettings,
    settingsRepository: AppSettingsRepository,
    scope: CoroutineScope,
    noteRepository: LocalNoteRepository,
    onPrivacyClick: () -> Unit,
    anchors: SectionAnchors
) {
    val context = LocalContext.current
    SettingLine(stringResource(R.string.privacy_no_tracking))
    SettingLine(stringResource(R.string.privacy_no_internet))
    SettingLine(stringResource(R.string.privacy_local_first))
    Spacer(Modifier.height(12.dp))
    SettingsGroupHeading(stringResource(R.string.settings_group_lock), first = false)
    val biometricAvailable = remember(context) { context.canUseBiometric() }
    SettingsSwitchRow(
        title = stringResource(R.string.biometric_lock_setting),
        titleModifier = anchors.item(SettingsItem.BIOMETRIC_LOCK),
        description = if (biometricAvailable) {
            stringResource(R.string.biometric_lock_description)
        } else {
            stringResource(R.string.biometric_lock_unavailable)
        },
        checked = appSettings.biometricLockEnabled && biometricAvailable,
        onCheckedChange = { checked ->
            if (!biometricAvailable && checked) return@SettingsSwitchRow
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setBiometricLockEnabled(checked)
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    LockedNotesPasscodeSetting(
        passcodeSet = appSettings.lockPasscodeSet,
        titleModifier = anchors.item(SettingsItem.LOCKED_PASSCODE),
        onSetPasscode = { passcode ->
            scope.launch {
                settingsRepository.setLockPasscode(passcode)
                Toast.makeText(
                    context,
                    R.string.locked_passcode_set_done,
                    Toast.LENGTH_SHORT
                ).show()
            }
        },
        onRemovePasscode = {
            scope.launch {
                settingsRepository.clearLockPasscode()
                withContext(Dispatchers.IO) { noteRepository.unlockAllLocked() }
                Toast.makeText(
                    context,
                    R.string.locked_passcode_removed_done,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    SettingsGroupHeading(stringResource(R.string.settings_group_screen), first = false)
    SettingsSwitchRow(
        title = stringResource(R.string.screenshot_protection),
        titleModifier = anchors.item(SettingsItem.SCREENSHOT_PROTECTION),
        description = stringResource(R.string.screenshot_protection_description),
        checked = appSettings.screenshotProtection,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setScreenshotProtection(checked)
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    SettingsSwitchRow(
        title = stringResource(R.string.recent_notes_in_shortcuts),
        titleModifier = anchors.item(SettingsItem.RECENT_NOTES_IN_SHORTCUTS),
        description = stringResource(R.string.recent_notes_in_shortcuts_description),
        checked = appSettings.recentNotesInShortcuts,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setRecentNotesInShortcuts(checked)
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = onPrivacyClick,
        modifier = anchors.item(SettingsItem.PRIVACY_DASHBOARD, focusable = false).fillMaxWidth()
    ) {
        Text(stringResource(R.string.privacy_dashboard_button))
    }
}
