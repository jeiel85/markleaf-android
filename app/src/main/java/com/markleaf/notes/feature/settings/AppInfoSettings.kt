package com.markleaf.notes.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.markleaf.notes.BuildConfig
import com.markleaf.notes.R
import com.markleaf.notes.update.UpdateSurface

/** The App page: version, application id and, in sideload builds, update checks. */
@Composable
internal fun AppInfoSettings(
    anchors: SectionAnchors
) {
    SettingLine(stringResource(R.string.version_format, BuildConfig.VERSION_NAME))
    SettingLine(stringResource(R.string.application_id_format, BuildConfig.APPLICATION_ID))
    // 사이드로드 빌드에서만 행이 생긴다. 스토어 빌드가 얻는 구현은
    // 아무것도 그리지 않는 스텁이다(D074, docs/AGENT_SPEC.md §15.9).
    UpdateSurface.SettingsRows(
        titleModifier = anchors.item(SettingsItem.UPDATE_CHECK)
    )
}
