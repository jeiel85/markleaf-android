package com.markleaf.notes.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.Formatter
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.BuildConfig
import com.markleaf.notes.R
import com.markleaf.notes.feature.settings.SettingsSwitchRow
import com.markleaf.notes.util.HapticFeedback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    @Composable
    private fun UpdateDialog(
        manifest: UpdateManifest,
        onDismiss: () -> Unit,
        onSkip: () -> Unit,
    ) {
        val context = LocalContext.current
        val size = remember(manifest) { Formatter.formatShortFileSize(context, manifest.apkSizeBytes) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.update_dialog_title)) },
            text = {
                Column {
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        context.open(manifest.apkUrl)
                        onDismiss()
                    }
                ) {
                    Text(stringResource(R.string.update_action_open))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
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
}
