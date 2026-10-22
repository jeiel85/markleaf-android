package com.markleaf.notes.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.ColorPalette
import com.markleaf.notes.data.settings.ThemeMode
import com.markleaf.notes.data.settings.WidgetOpacity
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/** The Appearance page: theme, colour palette and the home-screen widget's look. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AppearanceSettings(
    appSettings: AppSettings,
    settingsRepository: AppSettingsRepository,
    scope: CoroutineScope,
    anchors: SectionAnchors
) {
    Text(
        text = stringResource(R.string.theme_label),
        modifier = anchors.item(SettingsItem.THEME),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { mode ->
            val selected = appSettings.themeMode == mode
            if (selected) {
                Button(onClick = {}) {
                    Text(mode.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch { settingsRepository.setThemeMode(mode) }
                    }
                ) {
                    Text(mode.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.theme_mode_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.color_palette_label),
        modifier = anchors.item(SettingsItem.COLOR_PALETTE),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ColorPalette.entries.forEach { palette ->
            val selected = appSettings.colorPalette == palette
            if (selected) {
                Button(onClick = {}) {
                    Text(palette.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch { settingsRepository.setColorPalette(palette) }
                    }
                ) {
                    Text(palette.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.theme_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    SettingsGroupHeading(stringResource(R.string.settings_group_widget), first = false)
    Text(
        text = stringResource(R.string.widget_opacity_label),
        modifier = anchors.item(SettingsItem.WIDGET_OPACITY),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    // Five percentages don't fit one row on a narrow phone,
    // so they wrap like the font sizes below.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        WidgetOpacity.entries.forEach { opacity ->
            val selected = appSettings.widgetOpacity == opacity
            if (selected) {
                Button(onClick = {}) {
                    Text(opacity.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch { settingsRepository.setWidgetOpacity(opacity) }
                    }
                ) {
                    Text(opacity.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.widget_opacity_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    WidgetColorSetting(
        customColor = appSettings.widgetCustomColor,
        titleModifier = anchors.item(SettingsItem.WIDGET_COLOR),
        onChange = { color ->
            scope.launch { settingsRepository.setWidgetCustomColor(color) }
        }
    )
}
