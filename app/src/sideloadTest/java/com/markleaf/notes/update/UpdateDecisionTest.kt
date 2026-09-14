package com.markleaf.notes.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * 확인 주기와 배너 판정 규칙.
 *
 * 순수 함수만 다루므로 Robolectric이 필요 없다 — `UpdateManifestParserTest`와 달리 `org.json`을
 * 건드리지 않는다.
 */
class UpdateDecisionTest {

    @Test
    fun shouldCheckNow_checksOnTheFirstRun() {
        assertTrue(UpdateDecision.shouldCheckNow(lastCheckedAt = 0L, now = NOW))
    }

    @Test
    fun shouldCheckNow_waitsOutTheInterval() {
        val anHourAgo = NOW - TimeUnit.HOURS.toMillis(1)
        assertFalse(UpdateDecision.shouldCheckNow(lastCheckedAt = anHourAgo, now = NOW))

        val aDayAgo = NOW - UpdateDecision.CHECK_INTERVAL_MILLIS
        assertTrue(UpdateDecision.shouldCheckNow(lastCheckedAt = aDayAgo, now = NOW))
    }

    @Test
    fun shouldCheckNow_recoversFromAClockThatWentBackwards() {
        // 기기 시계가 앞당겨졌다가 돌아오면 마지막 확인 시각이 미래로 남는다. 그 상태를 "아직
        // 멀었다"로 읽으면 기능이 조용히 죽고, 사용자는 업데이트가 없다고 믿는다.
        val tomorrow = NOW + TimeUnit.DAYS.toMillis(1)
        assertTrue(UpdateDecision.shouldCheckNow(lastCheckedAt = tomorrow, now = NOW))
    }

    @Test
    fun shouldOffer_offersANewerVersion() {
        assertTrue(
            UpdateDecision.shouldOffer(
                manifest = manifest(versionCode = 145),
                installedVersionCode = 144,
                skippedVersionCode = 0,
                deviceSdk = 35,
            )
        )
    }

    @Test
    fun shouldOffer_staysQuietOnTheSameOrAnOlderVersion() {
        // versionName 문자열 비교였다면 2.9.0 > 2.10.0 같은 오판이 난다. 정수만 본다.
        assertFalse(
            UpdateDecision.shouldOffer(manifest(versionCode = 144), installedVersionCode = 144, skippedVersionCode = 0, deviceSdk = 35)
        )
        // 로컬에서 올린 개발 빌드를 쓰는 사람에게 "구버전으로 업데이트하세요"를 띄우지 않는다.
        assertFalse(
            UpdateDecision.shouldOffer(manifest(versionCode = 143), installedVersionCode = 144, skippedVersionCode = 0, deviceSdk = 35)
        )
    }

    @Test
    fun shouldOffer_respectsASkippedVersion() {
        assertFalse(
            UpdateDecision.shouldOffer(manifest(versionCode = 145), installedVersionCode = 144, skippedVersionCode = 145, deviceSdk = 35)
        )
        // 건너뛴 것은 그 버전 하나뿐이다. 다음 버전은 다시 알린다.
        assertTrue(
            UpdateDecision.shouldOffer(manifest(versionCode = 146), installedVersionCode = 144, skippedVersionCode = 145, deviceSdk = 35)
        )
    }

    @Test
    fun shouldOffer_staysQuietWhenTheDeviceIsTooOld() {
        // 받으라고 안내한 뒤 시스템이 설치를 거부하면 사용자는 원인을 알 수 없다.
        assertFalse(
            UpdateDecision.shouldOffer(manifest(versionCode = 145, minSdk = 30), installedVersionCode = 144, skippedVersionCode = 0, deviceSdk = 26)
        )
        assertTrue(
            UpdateDecision.shouldOffer(manifest(versionCode = 145, minSdk = 26), installedVersionCode = 144, skippedVersionCode = 0, deviceSdk = 26)
        )
    }

    private fun manifest(versionCode: Int, minSdk: Int = 26) = UpdateManifest(
        versionCode = versionCode,
        versionName = "test",
        apkUrl = "https://example.invalid/markleaf.apk",
        apkSizeBytes = 1,
        sha256 = "0",
        minSdk = minSdk,
        releaseNotesUrl = "https://example.invalid/notes",
    )

    private companion object {
        const val NOW = 1_800_000_000_000L
    }
}
