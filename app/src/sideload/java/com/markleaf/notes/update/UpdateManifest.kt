package com.markleaf.notes.update

import org.json.JSONObject

/**
 * `docs/update.json`이 싣는 내용. 릴리스 워크플로가 실제 자산에서 계산해 넣는다
 * (`docs/UPDATE_STRATEGY_EVALUATION.md`).
 *
 * 이 파일은 게이트 뒤(`src/sideload/java`)에만 있으므로 스토어 산출물에는 존재하지 않는다
 * (D074, `docs/AGENT_SPEC.md` §15.9).
 */
internal data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val sha256: String,
    val minSdk: Int,
    val releaseNotesUrl: String,
)

internal object UpdateManifestParser {

    /**
     * Input : `docs/update.json`의 본문 문자열
     * Output: 온전한 [UpdateManifest], 또는 조금이라도 이상하면 `null`
     *
     * 핵심 로직: **의심스러우면 null이다.** 업데이트 확인은 사용자가 요청한 적 없는 배경 작업이고,
     * 실패해도 사용자에게 보여줄 것이 없다 — 그래서 예외를 던지는 대신 조용히 포기한다. 빠진 필드,
     * 타입 불일치, 음수 versionCode, 깨진 JSON이 모두 같은 결과로 수렴한다.
     *
     * `https`를 강제하는 이유: 이 URL은 나중에 사용자의 브라우저로 넘어가고(B단계), 그 다음 단계에서는
     * 앱이 직접 내려받는다(C단계). 평문 URL을 받아들이면 중간자가 APK 주소를 갈아끼울 수 있다.
     * 호스트까지 고정하지 않는 것은 릴리스 자산이 GitHub 리다이렉트를 타기 때문이다 — 그쪽 방어는
     * 다운로드 단계의 SHA-256 대조와 OS의 서명 검증이 맡는다.
     */
    fun parse(raw: String): UpdateManifest? = try {
        val json = JSONObject(raw)
        val manifest = UpdateManifest(
            versionCode = json.getInt(KEY_VERSION_CODE),
            versionName = json.getString(KEY_VERSION_NAME),
            apkUrl = json.getString(KEY_APK_URL),
            apkSizeBytes = json.getLong(KEY_APK_SIZE),
            sha256 = json.getString(KEY_SHA256),
            minSdk = json.getInt(KEY_MIN_SDK),
            releaseNotesUrl = json.getString(KEY_RELEASE_NOTES_URL),
        )
        manifest.takeIf { it.isUsable() }
    } catch (_: Exception) {
        null
    }

    private fun UpdateManifest.isUsable(): Boolean =
        versionCode > 0 &&
            versionName.isNotBlank() &&
            apkUrl.startsWith(HTTPS) &&
            apkSizeBytes > 0 &&
            sha256.isNotBlank() &&
            minSdk > 0 &&
            releaseNotesUrl.startsWith(HTTPS)

    private const val HTTPS = "https://"
    private const val KEY_VERSION_CODE = "versionCode"
    private const val KEY_VERSION_NAME = "versionName"
    private const val KEY_APK_URL = "apkUrl"
    private const val KEY_APK_SIZE = "apkSizeBytes"
    private const val KEY_SHA256 = "sha256"
    private const val KEY_MIN_SDK = "minSdk"
    private const val KEY_RELEASE_NOTES_URL = "releaseNotesUrl"
}
