package com.markleaf.notes.update

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.markleaf.notes.R
import com.markleaf.notes.feature.settings.SettingsSwitchRow
import com.markleaf.notes.util.HapticFeedback

/**
 * 사이드로드 빌드가 얻는 이음매. `src/storeStub/java`의 같은 이름 파일과 **배타적으로**
 * 컴파일된다 (D074, `docs/AGENT_SPEC.md` §15.9).
 *
 * 공통 UI(`main`)는 이 객체만 부른다. 그래서 설정 화면은 업데이트 기능의 존재를 몰라도 되고,
 * 스토어 빌드에는 그 기능의 코드가 아예 들어가지 않는다.
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
     *
     * 상태를 `AppSettings`(DataStore)가 아니라 [UpdatePreferences]에서 읽는 이유는 그쪽 주석에 있다.
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
}
