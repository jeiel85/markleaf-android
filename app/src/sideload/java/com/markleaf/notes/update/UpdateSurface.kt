package com.markleaf.notes.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.markleaf.notes.BuildConfig
import com.markleaf.notes.R
import com.markleaf.notes.feature.settings.SettingsSwitchRow
import com.markleaf.notes.util.HapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 사이드로드 빌드가 얻는 이음매. `src/storeStub/java`의 같은 이름 파일과 **배타적으로**
 * 컴파일된다 (D074, `docs/AGENT_SPEC.md` §15.9).
 *
 * 공통 UI(`main`)는 이 객체만 부른다. 그래서 설정 화면과 노트 목록은 업데이트 기능의 존재를
 * 몰라도 되고, 스토어 빌드에는 그 기능의 코드가 아예 들어가지 않는다.
 */
internal object UpdateSurface {

    /**
     * 설정 → 앱 섹션에 붙는 행들.
     *
     * Input : 없음 (상태는 [UpdatePreferences]에서 읽는다)
     * Output: 기본 꺼짐 토글 한 줄과 그 아래 설명
     *
     * 핵심 로직: 켜는 순간 무엇이 달라지는지를 **같은 화면에서** 말한다. F-Droid 포함 정책은
     * 추가 실행 파일을 받는 동작에 옵트인을 요구하면서, 사용자가 "F-Droid의 검사를 우회하는
     * 선택을 하고 있다"는 것을 분명히 알 수 있게 하라고 적는다. 설명을 다른 화면으로 미루면
     * 그 요구를 형식적으로만 만족시킨다.
     */
    @Composable
    fun SettingsRows() {
        val context = LocalContext.current
        val preferences = remember(context) { UpdatePreferences(context) }
        var enabled by remember { mutableStateOf(preferences.isEnabled()) }

        Spacer(Modifier.height(12.dp))
        SettingsSwitchRow(
            title = stringResource(R.string.update_check_title),
            description = stringResource(R.string.update_check_description),
            checked = enabled,
            onCheckedChange = { checked ->
                HapticFeedback.light(context)
                preferences.setEnabled(checked)
                enabled = checked
            },
        )
    }

    /**
     * 노트 목록 위의 한 줄짜리 알림. 새 버전이 없으면 **아무것도 그리지 않는다.**
     *
     * Input : 없음
     * Output: 새 버전이 있을 때만 눌러서 모달을 여는 배너
     *
     * 핵심 로직: 확인은 백그라운드에서, 표시는 목록에서만 한다. 앱 시작 화면을 모달로 막지
     * 않는 것이 UX 계약이다 — 업데이트는 사용자가 지금 하려던 일이 아니다.
     *
     * 상태를 화면에 들고 있는 이유: 확인 자체는 하루 1회지만 배너는 그 사이에도 떠 있어야 하고,
     * 그 판단은 캐시된 원문으로 네트워크 없이 재현된다([refresh] 참조).
     */
    @Composable
    fun Banner(modifier: Modifier = Modifier) {
        val context = LocalContext.current
        val preferences = remember(context) { UpdatePreferences(context) }
        var offered by remember { mutableStateOf<UpdateManifest?>(null) }
        var dialogOpen by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            offered = withContext(Dispatchers.IO) { refresh(preferences) }
        }

        val manifest = offered ?: return

        // `Surface(onClick = ...)`가 아니라 `Modifier.clickable`인 이유: 그 오버로드는 Material3
        // 버전에 따라 실험 API라 opt-in이 필요해질 수 있다. 눌리는 영역과 모양은 동일하다.
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(
                text = stringResource(R.string.update_banner_text, manifest.versionName),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { dialogOpen = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        if (dialogOpen) {
            UpdateDialog(
                manifest = manifest,
                onDismiss = { dialogOpen = false },
                onSkip = {
                    preferences.setSkippedVersionCode(manifest.versionCode)
                    offered = null
                    dialogOpen = false
                },
            )
        }
    }

    /**
     * 이 모달의 상태 기계. `Offer`가 시작점이고, 성공하면 [onDismiss]로 빠져나가며 다시
     * 이 상태로 돌아오지 않는다 — 설치가 넘어간 뒤에 이 모달이 여전히 무언가를 보여줄 이유가
     * 없다(그 다음은 시스템 설치 확인 팝업의 몫).
     */
    private enum class DownloadFlowState { Offer, NeedsInstallPermission, Downloading, Failed }

    // 프로세스 전체에서 하나만 도는 다운로드/설치를 보장하는 재진입 가드. `UpdateDialog`가
    // 닫혔다 다시 열려도(이론상으로도) 같은 캐시 파일 경로(`DOWNLOAD_FILE_NAME`)를 두 흐름이
    // 동시에 쓰는 경합이 생기지 않게 한다 — 아래 `canDismiss`가 애초에 그 경로를 차단하지만,
    // 이 가드는 그 차단에 기대지 않는 두 번째 방어선이다.
    private val downloadInProgress = AtomicBoolean(false)

    @Composable
    private fun UpdateDialog(
        manifest: UpdateManifest,
        onDismiss: () -> Unit,
        onSkip: () -> Unit,
    ) {
        val context = LocalContext.current
        val size = remember(manifest) { Formatter.formatShortFileSize(context, manifest.apkSizeBytes) }
        var flowState by remember(manifest) { mutableStateOf(DownloadFlowState.Offer) }
        // LaunchedEffect의 재실행 열쇠다. 0은 "아직 시작 안 함"이고, 그 뒤로는 시도할 때마다
        // 하나씩 올린다 — "다시 시도"를 눌러도 같은 값이면 LaunchedEffect가 다시 돌지 않는다.
        var downloadEpoch by remember(manifest) { mutableIntStateOf(0) }

        fun startDownload() {
            // compareAndSet이 false를 반환하면 이미 다른 흐름이 돌고 있다는 뜻 — 조용히
            // 무시한다. 정상 경로에서는 canDismiss가 그 상태로의 재진입 자체를 막으므로
            // 여기 걸릴 일이 없어야 하지만, 그 가정이 깨지더라도 파일 경합만은 막는다.
            if (!downloadInProgress.compareAndSet(false, true)) return
            // 매번 다시 묻는다 — Settings에서 돌아온 뒤 다시 확인하는 것과 같은 이유로,
            // 사용자가 그사이 권한을 바꿨을 수도 있는 사실을 이 함수가 직접 안다고 가정하지 않는다.
            flowState = if (context.packageManager.canRequestPackageInstalls()) {
                downloadEpoch++
                DownloadFlowState.Downloading
            } else {
                downloadInProgress.set(false)
                DownloadFlowState.NeedsInstallPermission
            }
        }

        // `ACTION_MANAGE_UNKNOWN_APP_SOURCES`는 API 26(=minSdk)에 생겼다 — 버전 분기가 필요 없다.
        val installPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Settings 화면은 결과 코드로 승인 여부를 알려주지 않는다. 돌아온 뒤 PackageManager에
            // 직접 다시 물어야 한다.
            startDownload()
        }

        LaunchedEffect(downloadEpoch) {
            if (downloadEpoch == 0) return@LaunchedEffect
            try {
                val destination =
                    File(File(context.cacheDir, DOWNLOAD_DIR).apply { mkdirs() }, DOWNLOAD_FILE_NAME)
                val installed = withContext(Dispatchers.IO) {
                    when (val result = UpdateDownloader().download(manifest.apkUrl, destination, manifest.sha256)) {
                        is UpdateDownloader.Result.Success -> UpdateInstaller.install(context, result.file)
                        UpdateDownloader.Result.Failed -> false
                    }
                }
                // 성공하면 이 모달이 더 할 일이 없다 — 설치 확인 팝업은 시스템이 띄운다. 실패하면
                // "다시 시도"와 "브라우저에서 열기" 중 고르게 한다(디자인 문서의 "실패 복구").
                if (installed) onDismiss() else flowState = DownloadFlowState.Failed
            } finally {
                // `UpdateDownloader.download()`/`UpdateInstaller.install()`은 취소 체크포인트가
                // 없는 순수 블로킹 함수라, 이 LaunchedEffect가 취소돼도 둘 다 이미 시작했다면
                // 끝까지 실행된 뒤에야 여기 도달한다 — 그래서 가드 해제를 finally에 둬도
                // "아직 실행 중인데 풀렸다"는 일이 없다.
                downloadInProgress.set(false)
            }
        }

        // Downloading 동안은 닫을 방법을 아예 주지 않는다. 이 상태에서 시작된 네트워크 읽기나
        // `PackageInstaller.session.commit()`은 취소 체크포인트가 없는 블로킹 호출이라 다이얼로그를
        // 나가도 실제로는 멈추지 않는다 — "닫았다"고 믿었는데 잠시 뒤 설치 확인 팝업이 뜨는 혼란과,
        // 그 틈에 재시도가 같은 캐시 파일을 다시 써서 생기는 경합(위 `downloadInProgress` 참조)을
        // 애초에 만들지 않는 편이 낫다.
        val canDismiss = flowState != DownloadFlowState.Downloading

        AlertDialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = canDismiss,
                dismissOnClickOutside = canDismiss,
            ),
            title = { Text(stringResource(R.string.update_dialog_title)) },
            text = {
                when (flowState) {
                    DownloadFlowState.Offer -> Column {
                        Text(
                            text = stringResource(R.string.update_dialog_body, manifest.versionName, size),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // 릴리스 노트는 설치된 앱의 CHANGELOG가 아니라 manifest의 링크를 쓴다.
                        // 지금 깔려 있는 앱은 **새 버전의** 변경 내용을 알 수 없다.
                        TextButton(onClick = { context.open(manifest.releaseNotesUrl) }) {
                            Text(stringResource(R.string.update_action_release_notes))
                        }
                        TextButton(onClick = onSkip) {
                            Text(stringResource(R.string.update_action_skip))
                        }
                    }
                    DownloadFlowState.NeedsInstallPermission -> Text(
                        text = stringResource(R.string.update_permission_required_body),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    DownloadFlowState.Downloading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.update_downloading),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    DownloadFlowState.Failed -> Column {
                        Text(
                            text = stringResource(R.string.update_download_failed_body),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // 최종 수단: 이 버튼이 눌리면 B단계와 같은 경로(브라우저에 위임)로
                        // 돌아간다. 앱이 직접 받는 경로가 실패해도 사용자는 막다른 길에 있지
                        // 않다(`docs/UPDATE_STRATEGY_EVALUATION.md` "C단계 구현 세부").
                        TextButton(
                            onClick = {
                                context.open(manifest.apkUrl)
                                onDismiss()
                            }
                        ) {
                            Text(stringResource(R.string.update_action_open_in_browser))
                        }
                    }
                }
            },
            confirmButton = {
                when (flowState) {
                    DownloadFlowState.Offer -> TextButton(onClick = ::startDownload) {
                        Text(stringResource(R.string.update_action_open))
                    }
                    DownloadFlowState.NeedsInstallPermission -> TextButton(
                        onClick = {
                            installPermissionLauncher.launch(
                                Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}"),
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.update_action_open_settings))
                    }
                    // 내려받는 동안은 누를 만한 주 동작이 없다 — 이 상태에서는 애초에 닫을
                    // 방법도 없다(위 `canDismiss` 참조), 기다리는 것 외엔 할 게 없다.
                    DownloadFlowState.Downloading -> Unit
                    DownloadFlowState.Failed -> TextButton(onClick = ::startDownload) {
                        Text(stringResource(R.string.update_action_retry))
                    }
                }
            },
            dismissButton = {
                // Downloading 상태에서는 위 `canDismiss`가 뒤로가기·바깥 탭을 이미 막았고,
                // 이 버튼도 같은 이유로 아예 그리지 않는다 — 닫을 방법이 하나라도 남아 있으면
                // 안 된다.
                if (canDismiss) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.close))
                    }
                }
            },
        )
    }

    /**
     * Input : [UpdatePreferences]
     * Output: 지금 알릴 만한 [UpdateManifest], 아니면 null
     *
     * 핵심 로직: 네트워크는 하루 1회만 타고, 배너 판단은 캐시된 원문으로 매번 다시 한다.
     *
     * **온전하게 파싱되는 응답만 저장하고, 그때만 확인 시각을 찍는다.** 실패로 하루를 소모하면
     * 잠깐 오프라인이었던 사용자가 다음 날까지 확인하지 못하고, 깨진 응답을 캐시하면 그 하루
     * 동안 매 실행에서 같은 쓰레기를 파싱하고 버린다.
     *
     * 이 함수는 IO 스레드에서 불린다 — [Banner]의 `LaunchedEffect` 참조.
     */
    private fun refresh(preferences: UpdatePreferences): UpdateManifest? {
        if (!preferences.isEnabled()) return null

        val now = System.currentTimeMillis()
        if (UpdateDecision.shouldCheckNow(preferences.lastCheckedAt(), now)) {
            UpdateChecker().fetchRaw()?.let { raw ->
                if (UpdateManifestParser.parse(raw) != null) {
                    preferences.setCachedManifest(raw)
                    preferences.setLastCheckedAt(now)
                }
            }
        }

        val manifest = preferences.cachedManifest()?.let(UpdateManifestParser::parse) ?: return null
        return manifest.takeIf {
            UpdateDecision.shouldOffer(
                manifest = it,
                installedVersionCode = BuildConfig.VERSION_CODE,
                skippedVersionCode = preferences.skippedVersionCode(),
                deviceSdk = Build.VERSION.SDK_INT,
            )
        }
    }

    /** 링크는 OS에 넘긴다. 앱이 직접 여는 브라우저는 없다 — 실패해도 조용히 넘어간다. */
    private fun Context.open(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    // 외부 저장소 권한을 피하려고 앱 캐시에 받는다(`docs/UPDATE_STRATEGY_EVALUATION.md`
    // "C단계 구현 세부"). 하위 디렉터리로 묶어 두는 것은 정리를 위해서일 뿐, 특별 취급이
    // 필요해서가 아니다 — Android가 캐시를 통째로 비울 때 이 폴더도 다른 캐시와 똑같이 지워진다.
    private const val DOWNLOAD_DIR = "updates"
    private const val DOWNLOAD_FILE_NAME = "update.apk"
}
