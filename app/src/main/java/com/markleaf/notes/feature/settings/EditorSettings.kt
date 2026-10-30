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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.core.font.CustomFontStore
import com.markleaf.notes.data.settings.EditorFont
import com.markleaf.notes.data.settings.EditorFontSize
import com.markleaf.notes.data.settings.EditorLineSpacing
import com.markleaf.notes.data.settings.EditorLineWidth
import com.markleaf.notes.data.settings.MarkdownSyntaxVisibility
import com.markleaf.notes.util.HapticFeedback
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope

/** The Editor page: how the writing surface sets its text, and what Markdown it shows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EditorSettings(
    appSettings: AppSettings,
    settingsRepository: AppSettingsRepository,
    scope: CoroutineScope,
    anchors: SectionAnchors,
    onPickCustomFont: () -> Unit
) {
    val context = LocalContext.current
    SettingsGroupHeading(stringResource(R.string.settings_group_text), first = true)
    Text(
        text = stringResource(R.string.font_label),
        modifier = anchors.item(SettingsItem.FONT),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    // Three options with long translations (ru "Моноширинный")
    // overflow one row on a narrow screen, so they wrap.
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EditorFont.entries.forEach { font ->
            val selected = appSettings.editorFont == font
            if (selected) {
                Button(onClick = {}) {
                    Text(font.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        // Your font with no file yet goes
                        // straight to the picker; picking
                        // one is what selects it.
                        if (font == EditorFont.CUSTOM &&
                            CustomFontStore.file(context, appSettings.customFontFile) == null
                        ) {
                            onPickCustomFont()
                        } else {
                            scope.launch {
                                settingsRepository.setEditorFont(font)
                            }
                        }
                    }
                ) {
                    Text(font.localizedLabel())
                }
            }
        }
    }
    if (appSettings.editorFont == EditorFont.CUSTOM) {
        Spacer(Modifier.height(6.dp))
        val fontName = appSettings.customFontName
            ?.takeIf { CustomFontStore.file(context, appSettings.customFontFile) != null }
        Text(
            text = if (fontName != null) {
                stringResource(R.string.font_custom_file_format, fontName)
            } else {
                stringResource(R.string.font_custom_missing)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = { onPickCustomFont() }) {
            Text(stringResource(R.string.font_custom_choose))
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.font_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.font_size_label),
        modifier = anchors.item(SettingsItem.FONT_SIZE),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    // Four options with long translations (de "Sehr groß")
    // overflow one row at some widths, so they wrap.
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorFontSize.entries.forEach { size ->
            val selected = appSettings.editorFontSize == size
            if (selected) {
                Button(onClick = {}) {
                    Text(size.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            settingsRepository.setEditorFontSize(size)
                        }
                    }
                ) {
                    Text(size.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.font_size_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.line_spacing_label),
        modifier = anchors.item(SettingsItem.LINE_SPACING),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    // Wraps for the same reason as the sizes above (ru "Компактный").
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorLineSpacing.entries.forEach { spacing ->
            val selected = appSettings.editorLineSpacing == spacing
            if (selected) {
                Button(onClick = {}) {
                    Text(spacing.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            settingsRepository.setEditorLineSpacing(spacing)
                        }
                    }
                ) {
                    Text(spacing.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(
        text = stringResource(R.string.line_spacing_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    Text(
        text = stringResource(R.string.line_width),
        modifier = anchors.item(SettingsItem.LINE_WIDTH),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorLineWidth.entries.forEach { lineWidth ->
            val selected = appSettings.lineWidth == lineWidth
            if (selected) {
                Button(onClick = {}) {
                    Text(lineWidth.localizedLabel())
                }
            } else {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            settingsRepository.setLineWidth(lineWidth)
                        }
                    }
                ) {
                    Text(lineWidth.localizedLabel())
                }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    // The cap only binds on a wide layout (the tablet editor
    // pane, the file viewer); an upright phone is narrower than
    // every option, so the label alone read as line spacing that
    // "did nothing" (#465).
    Text(
        text = stringResource(R.string.line_width_description),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(12.dp))
    SettingsGroupHeading(stringResource(R.string.settings_markdown), first = false)
    SettingsSwitchRow(
        title = stringResource(R.string.show_markdown_syntax),
        titleModifier = anchors.item(SettingsItem.MARKDOWN_SYNTAX),
        description = stringResource(R.string.show_markdown_syntax_description),
        checked = appSettings.markdownSyntaxVisibility == MarkdownSyntaxVisibility.SHOW,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setMarkdownSyntaxVisibility(
                    if (checked) MarkdownSyntaxVisibility.SHOW else MarkdownSyntaxVisibility.HIDE
                )
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    SettingsSwitchRow(
        title = stringResource(R.string.indent_paragraphs),
        titleModifier = anchors.item(SettingsItem.INDENT_PARAGRAPHS),
        description = stringResource(R.string.indent_paragraphs_description),
        checked = appSettings.indentParagraphs,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setIndentParagraphs(checked)
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    SettingsSwitchRow(
        title = stringResource(R.string.show_formatting_button),
        titleModifier = anchors.item(SettingsItem.FORMATTING_BUTTON),
        description = stringResource(R.string.show_formatting_button_description),
        checked = appSettings.showFormattingButton,
        onCheckedChange = { checked ->
            HapticFeedback.light(context)
            scope.launch {
                settingsRepository.setShowFormattingButton(checked)
            }
        }
    )
    Spacer(Modifier.height(12.dp))
    SnippetButtonsSetting(
        buttons = appSettings.snippetButtons,
        titleModifier = anchors.item(SettingsItem.SNIPPET_BUTTONS),
        onChange = { buttons ->
            scope.launch { settingsRepository.setSnippetButtons(buttons) }
        }
    )
}
