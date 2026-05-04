package com.markleaf.notes.feature.settings

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import android.Manifest
import android.os.Build
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
import com.markleaf.notes.util.HapticFeedback
import com.markleaf.notes.util.PermissionUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrivacyClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsRepository = remember { AppSettingsRepository(context.applicationContext) }
    val appSettings by settingsRepository.settings.collectAsState(initial = AppSettings())
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var statusIsError by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }
    var hasStoragePermission by remember { mutableStateOf(PermissionUtils.hasStoragePermission(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasNotificationPermission = PermissionUtils.hasNotificationPermission(context)
                hasStoragePermission = PermissionUtils.hasStoragePermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val performToggle: (Boolean, () -> Unit) -> Unit = { checked, action ->
        HapticFeedback.light(context)
        action()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (!isGranted) {
            // If denied, we could show a message, but the UI will just show the "Request" button again
            // which will then probably need to go to settings if permanently denied.
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasStoragePermission = PermissionUtils.hasStoragePermission(context)
    }

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
                            performToggle(checked) {
                                scope.launch {
                                    settingsRepository.setMarkdownSyntaxVisibility(
                                        if (checked) MarkdownSyntaxVisibility.SHOW else MarkdownSyntaxVisibility.HIDE
                                    )
                                }
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
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.toolbar_customization),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsSwitchRow(
                        title = stringResource(R.string.bold),
                        description = null,
                        checked = appSettings.toolbarConfig.showBold,
                        onCheckedChange = { checked ->
                            performToggle(checked) {
                                scope.launch {
                                    settingsRepository.setToolbarConfig(
                                        appSettings.toolbarConfig.copy(showBold = checked)
                                    )
                                }
                            }
                        }
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.italic),
                        description = null,
                        checked = appSettings.toolbarConfig.showItalic,
                        onCheckedChange = { checked ->
                            performToggle(checked) {
                                scope.launch {
                                    settingsRepository.setToolbarConfig(
                                        appSettings.toolbarConfig.copy(showItalic = checked)
                                    )
                                }
                            }
                        }
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.checkbox),
                        description = null,
                        checked = appSettings.toolbarConfig.showCheckbox,
                        onCheckedChange = { checked ->
                            performToggle(checked) {
                                scope.launch {
                                    settingsRepository.setToolbarConfig(
                                        appSettings.toolbarConfig.copy(showCheckbox = checked)
                                    )
                                }
                            }
                        }
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.markdown_link),
                        description = null,
                        checked = appSettings.toolbarConfig.showMarkdownLink,
                        onCheckedChange = { checked ->
                            performToggle(checked) {
                                scope.launch {
                                    settingsRepository.setToolbarConfig(
                                        appSettings.toolbarConfig.copy(showMarkdownLink = checked)
                                    )
                                }
                            }
                        }
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.wiki_link),
                        description = null,
                        checked = appSettings.toolbarConfig.showWikiLink,
                        onCheckedChange = { checked ->
                            performToggle(checked) {
                                scope.launch {
                                    settingsRepository.setToolbarConfig(
                                        appSettings.toolbarConfig.copy(showWikiLink = checked)
                                    )
                                }
                            }
                        }
                    )
                    SettingsSwitchRow(
                        title = stringResource(R.string.image),
                        description = null,
                        checked = appSettings.toolbarConfig.showImage,
                        onCheckedChange = { checked ->
                            performToggle(checked) {
                                scope.launch {
                                    settingsRepository.setToolbarConfig(
                                        appSettings.toolbarConfig.copy(showImage = checked)
                                    )
                                }
                            }
                        }
                    )
                }

                SettingsSection(title = stringResource(R.string.settings_privacy)) {
                    SettingLine(stringResource(R.string.privacy_no_tracking))
                    SettingLine(stringResource(R.string.privacy_no_internet))
                    SettingLine(stringResource(R.string.privacy_local_first))
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onPrivacyClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.privacy_dashboard_button))
                    }
                }

                SettingsSection(title = stringResource(R.string.settings_permissions)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        PermissionRow(
                            title = stringResource(R.string.permission_notifications),
                            description = stringResource(R.string.permission_notifications_desc),
                            isGranted = hasNotificationPermission,
                            onRequest = {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            },
                            onOpenSettings = {
                                PermissionUtils.openAppSettings(context)
                            }
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                    PermissionRow(
                        title = stringResource(R.string.permission_storage),
                        description = stringResource(R.string.permission_storage_desc),
                        isGranted = hasStoragePermission,
                        onRequest = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                storagePermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_MEDIA_IMAGES,
                                        Manifest.permission.READ_MEDIA_VIDEO
                                    )
                                )
                            } else {
                                storagePermissionLauncher.launch(
                                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                                )
                            }
                        },
                        onOpenSettings = {
                            PermissionUtils.openAppSettings(context)
                        }
                    )
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
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
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
        if (isGranted) {
            Text(
                text = stringResource(R.string.permission_granted),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        } else {
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                OutlinedButton(onClick = onRequest) {
                    Text(stringResource(R.string.permission_request))
                }
                androidx.compose.material3.TextButton(onClick = onOpenSettings) {
                    Text(stringResource(R.string.permission_settings))
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
