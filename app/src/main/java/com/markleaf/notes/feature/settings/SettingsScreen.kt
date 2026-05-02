package com.markleaf.notes.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.markleaf.notes.BuildConfig
import com.markleaf.notes.R
import com.markleaf.notes.data.settings.AppSettings
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.EditorLineWidth
import com.markleaf.notes.data.settings.MarkdownSyntaxVisibility
import com.markleaf.notes.util.BackupUtil
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = BackupUtil.createBackupResult(context, uri)
                statusIsError = !result.success
                statusMessage = if (result.success) {
                    context.getString(
                        R.string.backup_created_detail,
                        result.noteCount,
                        result.attachmentCount,
                        result.linkCount
                    )
                } else {
                    context.getString(R.string.backup_failed_detail)
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = BackupUtil.restoreBackupResult(context, uri)
                statusIsError = !result.success
                statusMessage = if (result.success) {
                    context.getString(
                        R.string.backup_restored_detail,
                        result.noteCount,
                        result.attachmentCount,
                        result.linkCount
                    )
                } else {
                    context.getString(R.string.restore_failed_detail)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                SettingsSection(title = stringResource(R.string.settings_data)) {
                    Text(
                        text = stringResource(R.string.settings_data_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { backupLauncher.launch("markleaf_backup.zip") }) {
                            Text(stringResource(R.string.create_backup))
                        }
                        OutlinedButton(onClick = { restoreLauncher.launch(arrayOf("application/zip")) }) {
                            Text(stringResource(R.string.restore))
                        }
                    }
                    statusMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (statusIsError) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_markdown)) {
                    SettingsSwitchRow(
                        title = stringResource(R.string.show_markdown_syntax),
                        description = stringResource(R.string.show_markdown_syntax_description),
                        checked = appSettings.markdownSyntaxVisibility == MarkdownSyntaxVisibility.SHOW,
                        onCheckedChange = { checked ->
                            scope.launch {
                                settingsRepository.setMarkdownSyntaxVisibility(
                                    if (checked) MarkdownSyntaxVisibility.SHOW else MarkdownSyntaxVisibility.HIDE
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.line_width),
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
                    Spacer(Modifier.height(12.dp))
                    SettingLine(stringResource(R.string.markdown_preview_support))
                    SettingLine(stringResource(R.string.external_links_mvp))
                    SettingLine(stringResource(R.string.local_link_hint))
                }

                SettingsSection(title = stringResource(R.string.settings_privacy)) {
                    SettingLine(stringResource(R.string.privacy_no_tracking))
                    SettingLine(stringResource(R.string.privacy_no_internet))
                    SettingLine(stringResource(R.string.privacy_local_first))
                }

                SettingsSection(title = stringResource(R.string.settings_app)) {
                    SettingLine(stringResource(R.string.version_format, BuildConfig.VERSION_NAME))
                    SettingLine(stringResource(R.string.application_id_format, BuildConfig.APPLICATION_ID))
                }
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Column(content = content)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
    }
}

@Composable
private fun SettingLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}

@Composable
private fun EditorLineWidth.localizedLabel(): String {
    return when (this) {
        EditorLineWidth.NARROW -> stringResource(R.string.line_width_narrow)
        EditorLineWidth.COMFORTABLE -> stringResource(R.string.line_width_comfortable)
        EditorLineWidth.WIDE -> stringResource(R.string.line_width_wide)
    }
}
