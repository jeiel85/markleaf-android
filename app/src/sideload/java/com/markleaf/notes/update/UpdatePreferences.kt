package com.markleaf.notes.update

import android.content.Context

/**
 * 업데이트 확인이 기억해야 하는 세 가지. 전부 이 빌드에서만 의미가 있으므로 앱의
 * `AppSettings`(DataStore)에 넣지 않는다 — 스토어 빌드의 설정 모델에 쓰이지 않는 필드를
 * 남기지 않기 위해서다.
 *
 * `SharedPreferences`인 이유는 `SingleNoteWidgetStore`·`WidgetPaletteStore`와 같다: 읽는 쪽이
 * 코루틴 밖(Compose 컴포지션, 앱 시작 경로)이고, 값이 셋뿐이라 DataStore의 비동기 계약을
 * 끌어올 이유가 없다.
 */
internal class UpdatePreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    /**
     * 기본값이 `false`인 것이 이 기능의 계약이다. F-Droid 포함 정책은 추가 실행 파일을 받는
     * 동작에 **옵트인**을 요구하고, 사용자가 켜는 순간 그것이 F-Droid의 검증을 우회한다는 사실을
     * 같은 화면에서 설명해야 한다(`docs/AGENT_SPEC.md` §15.9).
     */
    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** 마지막으로 확인에 **성공한** 시각. 실패는 기록하지 않는다 — 실패로 하루를 소모하면
     *  잠깐 오프라인이었던 사용자가 다음 날까지 확인하지 못한다. */
    fun lastCheckedAt(): Long = prefs.getLong(KEY_LAST_CHECKED_AT, 0L)

    fun setLastCheckedAt(at: Long) {
        prefs.edit().putLong(KEY_LAST_CHECKED_AT, at).apply()
    }

    /** 사용자가 "이 버전 건너뛰기"를 누른 versionCode. 없으면 0. */
    fun skippedVersionCode(): Int = prefs.getInt(KEY_SKIPPED_VERSION_CODE, 0)

    fun setSkippedVersionCode(versionCode: Int) {
        prefs.edit().putInt(KEY_SKIPPED_VERSION_CODE, versionCode).apply()
    }

    private companion object {
        const val NAME = "markleaf_update"
        const val KEY_ENABLED = "enabled"
        const val KEY_LAST_CHECKED_AT = "last_checked_at"
        const val KEY_SKIPPED_VERSION_CODE = "skipped_version_code"
    }
}
